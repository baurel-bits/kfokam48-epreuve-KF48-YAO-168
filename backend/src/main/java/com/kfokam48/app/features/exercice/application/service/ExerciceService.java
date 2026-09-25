package com.kfokam48.app.features.exercice.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.exercice.application.dto.DepotExerciceRequete;
import com.kfokam48.app.features.exercice.application.dto.ExerciceDeposeReponse;
import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import com.kfokam48.app.features.exercice.domain.entity.StatutExercice;
import com.kfokam48.app.features.exercice.domain.repository.ExerciceRepository;
import com.kfokam48.app.features.promotion.domain.repository.EtudiantRepository;
import com.kfokam48.app.features.relecture.application.service.AssignateurRelecteur;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dépôt du lien d'un exercice par un étudiant (EF3 ; RG10, RG14).
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

    public ExerciceService(ExerciceRepository exerciceRepository,
                           SessionRepository sessionRepository,
                           EtudiantRepository etudiantRepository,
                           AssignateurRelecteur assignateurRelecteur) {
        this.exerciceRepository = exerciceRepository;
        this.sessionRepository = sessionRepository;
        this.etudiantRepository = etudiantRepository;
        this.assignateurRelecteur = assignateurRelecteur;
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
