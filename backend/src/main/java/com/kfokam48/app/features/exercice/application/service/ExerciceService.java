package com.kfokam48.app.features.exercice.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.exercice.application.dto.DepotExerciceRequete;
import com.kfokam48.app.features.exercice.application.dto.ExerciceDeposeReponse;
import com.kfokam48.app.features.exercice.application.dto.RemplacementLienRequete;
import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import com.kfokam48.app.features.exercice.domain.entity.StatutExercice;
import com.kfokam48.app.features.exercice.domain.repository.ExerciceRepository;
import com.kfokam48.app.features.promotion.domain.repository.EtudiantRepository;
import com.kfokam48.app.features.relecture.application.service.AssignateurRelecteur;
import com.kfokam48.app.features.relecture.domain.repository.RelectureRepository;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dépôt du lien d'un exercice par un étudiant (EF3 ; RG10, RG14) et remplacement
 * de ce lien tant qu'aucune relecture n'a été commencée (EF4 ; RG11).
 *
 * <p>Ordre des contrôles :
 * <ol>
 *   <li>format du lien → {@code 400 LIEN_INVALIDE} ;</li>
 *   <li>étudiant référencé → {@code 400} ;</li>
 *   <li>session référencée → {@code 400} ;</li>
 *   <li>session clôturée (RG10/RG14) → {@code 409 SESSION_CLOTUREE} ;</li>
 *   <li>dépôt déjà existant → {@code 409 EXERCICE_DEJA_DEPOSE} ;</li>
 *   <li>sinon création de l'exercice au statut {@code DEPOSE} (D4).</li>
 * </ol>
 *
 * <p><strong>RG10</strong> : l'expiration du code de présence n'a aucun effet ici.
 * Le dépôt reste ouvert jusqu'à la clôture de la session, y compris après la fin
 * de la fenêtre de présence — c'est pourquoi {@code expirationAt} n'est jamais
 * consulté par ce service.
 *
 * <p>EF5 : le dépôt déclenche l'assignation d'un relecteur (effet de bord, RG5/RG13).
 * Lorsqu'un relecteur est trouvé, l'exercice passe de {@code DEPOSE} à
 * {@code EN_ATTENTE_RELECTURE} (D4) ; sans étudiant éligible, il reste
 * {@code DEPOSE} et aucun relecteur ne lui est attaché.
 */
@Service
public class ExerciceService {

    /** Le contrat de données limite le lien à la colonne {@code lien VARCHAR(500)}. */
    private static final int LONGUEUR_LIEN_MAX = 500;

    private final ExerciceRepository exerciceRepository;
    private final SessionRepository sessionRepository;
    private final EtudiantRepository etudiantRepository;
    private final AssignateurRelecteur assignateurRelecteur;
    private final RelectureRepository relectureRepository;

    public ExerciceService(ExerciceRepository exerciceRepository,
                           SessionRepository sessionRepository,
                           EtudiantRepository etudiantRepository,
                           AssignateurRelecteur assignateurRelecteur,
                           RelectureRepository relectureRepository) {
        this.exerciceRepository = exerciceRepository;
        this.sessionRepository = sessionRepository;
        this.etudiantRepository = etudiantRepository;
        this.assignateurRelecteur = assignateurRelecteur;
        this.relectureRepository = relectureRepository;
    }

    @Transactional
    public ExerciceDeposeReponse deposer(DepotExerciceRequete requete) {
        String lien = validerLien(requete.lien());

        if (!etudiantRepository.existsById(requete.etudiantId())) {
            throw new ExceptionMetier(CodeErreur.ETUDIANT_INCONNU, HttpStatus.BAD_REQUEST,
                    "L'étudiant %d est inconnu.".formatted(requete.etudiantId()));
        }

        Session session = sessionRepository.findById(requete.sessionId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.BAD_REQUEST,
                        "La session %d est inconnue.".formatted(requete.sessionId())));

        // RG10 / RG14 : le dépôt reste possible après l'expiration du code, mais
        // s'arrête définitivement à la clôture de la session.
        if (session.isCloturee()) {
            throw new ExceptionMetier(CodeErreur.SESSION_CLOTUREE, HttpStatus.CONFLICT,
                    "La session est clôturée : plus aucun dépôt n'est accepté.");
        }

        if (exerciceRepository.existsBySessionIdAndEtudiantId(session.getId(), requete.etudiantId())) {
            throw new ExceptionMetier(CodeErreur.EXERCICE_DEJA_DEPOSE, HttpStatus.CONFLICT,
                    "Cet étudiant a déjà déposé un exercice pour cette session.");
        }

        Exercice exercice = exerciceRepository.save(new Exercice(session.getId(), requete.etudiantId(),
                lien, StatutExercice.DEPOSE, LocalDateTime.now()));

        // EF5 (RG5, RG13) : le pool est évalué à cet instant précis, présences
        // manuelles antérieures incluses.
        assignateurRelecteur.assignerUnRelecteur(exercice.getId(), session.getId(), requete.etudiantId())
                .ifPresent(relecteurId -> exercice.attribuerRelecteur());

        return new ExerciceDeposeReponse(exercice.getId(), exercice.getStatut());
    }

    /**
     * EF4 — remplace le lien d'un exercice déjà déposé (RG11).
     *
     * <p>Ordre des contrôles :
     * <ol>
     *   <li>format du lien → {@code 400 LIEN_INVALIDE} : le remplacement n'ouvre
     *       pas une porte à un lien que le dépôt aurait refusé ;</li>
     *   <li>exercice inconnu → {@code 404 EXERCICE_INCONNU} ;</li>
     *   <li>session clôturée → {@code 409 SESSION_CLOTUREE} (RG14), qui gèle le
     *       remplacement comme le dépôt ;</li>
     *   <li>relecture commencée → {@code 409 RELECTURE_COMMENCEE} (RG11) ;</li>
     *   <li>sinon remplacement, statut inchangé.</li>
     * </ol>
     *
     * <p><strong>RG11</strong> : « tant qu'aucune relecture n'a été
     * <em>commencée</em> dessus ». Une relecture existe dès qu'un relecteur a été
     * tiré au dépôt (EF5), donc avant toute note : le contrôle porte sur
     * l'existence de cette relecture — la vérité — plutôt que sur le statut de
     * l'exercice, qui n'en est qu'un reflet (l'équivalence {@code DEPOSE} ⟺ aucune
     * relecture reste vraie, et les tests la vérifient, mais RG11 ne dépend pas
     * d'elle). En pratique, le remplacement n'est donc possible que pour un
     * exercice resté {@code DEPOSE}, cas où aucun étudiant n'était éligible au
     * dépôt.
     */
    @Transactional
    public ExerciceDeposeReponse remplacerLien(Long exerciceId, RemplacementLienRequete requete) {
        String lien = validerLien(requete.lien());

        Exercice exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.EXERCICE_INCONNU, HttpStatus.NOT_FOUND,
                        "L'exercice %d est introuvable.".formatted(exerciceId)));

        // Session garantie par la clé étrangère : le 404 ne couvre qu'une donnée
        // introduite hors application, comme sur la notation (EF6).
        Session session = sessionRepository.findById(exercice.getSessionId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.NOT_FOUND,
                        "La session %d est inconnue.".formatted(exercice.getSessionId())));
        if (session.isCloturee()) {
            throw new ExceptionMetier(CodeErreur.SESSION_CLOTUREE, HttpStatus.CONFLICT,
                    "La session est clôturée : plus aucun dépôt ni remplacement de lien n'est accepté.");
        }

        if (relectureRepository.existsByExerciceId(exerciceId)) {
            throw new ExceptionMetier(CodeErreur.RELECTURE_COMMENCEE, HttpStatus.CONFLICT,
                    "Une relecture de cet exercice a été commencée : son lien ne peut plus être remplacé.");
        }

        // Entité gérée par la transaction : la modification est écrite au commit.
        exercice.remplacerLien(lien);

        return new ExerciceDeposeReponse(exercice.getId(), exercice.getStatut());
    }

    /**
     * Le lien doit être une URL absolue {@code http(s)} exploitable, sinon le
     * contrat impose {@code 400 LIEN_INVALIDE} — et non {@code VALIDATION_INVALIDE},
     * d'où ce contrôle dans le service.
     */
    private String validerLien(String lienBrut) {
        String lien = lienBrut == null ? "" : lienBrut.trim();
        if (lien.isEmpty() || lien.length() > LONGUEUR_LIEN_MAX) {
            throw lienInvalide();
        }
        try {
            URI uri = new URI(lien);
            boolean schemaHttp = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            if (!schemaHttp || uri.getHost() == null || uri.getHost().isBlank()) {
                throw lienInvalide();
            }
        } catch (URISyntaxException erreur) {
            throw lienInvalide();
        }
        return lien;
    }

    private ExceptionMetier lienInvalide() {
        return new ExceptionMetier(CodeErreur.LIEN_INVALIDE, HttpStatus.BAD_REQUEST,
                "Le lien de l'exercice doit être une URL http(s) valide (500 caractères maximum).");
    }
}
