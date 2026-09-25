package com.kfokam48.app.features.relecture.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import com.kfokam48.app.features.exercice.domain.repository.ExerciceRepository;
import com.kfokam48.app.features.presence.domain.entity.Presence;
import com.kfokam48.app.features.presence.domain.repository.PresenceRepository;
import com.kfokam48.app.features.relecture.application.dto.MissionRelecteurReponse;
import com.kfokam48.app.features.relecture.application.dto.NoteRecueReponse;
import com.kfokam48.app.features.relecture.application.dto.RelectureRendueReponse;
import com.kfokam48.app.features.relecture.application.dto.SoumissionRelectureRequete;
import com.kfokam48.app.features.relecture.domain.entity.CorrectionRelecture;
import com.kfokam48.app.features.relecture.domain.entity.Relecture;
import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;
import com.kfokam48.app.features.relecture.domain.repository.CorrectionRelectureRepository;
import com.kfokam48.app.features.relecture.domain.repository.RelectureRepository;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assignation d'un relecteur à un exercice déposé (EF5 ; RG4, RG5, RG13) et
 * consultation de sa mission (RG6).
 *
 * <p><strong>RG13</strong> : le pool des étudiants éligibles est lu dans les
 * présences de la session <em>au moment de l'assignation</em>, sans aucun
 * instantané figé. Une présence ajoutée manuellement par le formateur (EF10)
 * avant ce dépôt fait donc bien partie du pool, alors qu'une présence ajoutée
 * après ne peut plus influencer une assignation déjà faite.
 *
 * <p><strong>RG4</strong> : l'auteur est écarté du pool avant le tirage ; la base
 * le re-vérifie via {@code ck_relecture_pas_auto_relecture}.
 *
 * <p>EF6 (rendre une note), EF7 (la corriger avant clôture, RG8) et EF8 (la
 * relire depuis le côté étudiant) partagent la même garantie de confidentialité :
 * aucune réponse ne porte l'identité du relecteur (RG6).
 */
@Service
public class RelectureService implements AssignateurRelecteur {

    /** Bornes de RG7, reprises du {@code CHECK (note BETWEEN 0 AND 20)} de V1. */
    private static final BigDecimal NOTE_MINIMALE = BigDecimal.ZERO;
    private static final BigDecimal NOTE_MAXIMALE = new BigDecimal("20");

    private final RelectureRepository relectureRepository;
    private final CorrectionRelectureRepository correctionRelectureRepository;
    private final PresenceRepository presenceRepository;
    private final ExerciceRepository exerciceRepository;
    private final SessionRepository sessionRepository;
    private final SecureRandom aleatoire = new SecureRandom();

    public RelectureService(RelectureRepository relectureRepository,
                            CorrectionRelectureRepository correctionRelectureRepository,
                            PresenceRepository presenceRepository,
                            ExerciceRepository exerciceRepository,
                            SessionRepository sessionRepository) {
        this.relectureRepository = relectureRepository;
        this.correctionRelectureRepository = correctionRelectureRepository;
        this.presenceRepository = presenceRepository;
        this.exerciceRepository = exerciceRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Tire au hasard un relecteur parmi les étudiants présents à la session,
     * en excluant l'auteur de l'exercice (RG5, RG13, RG4), puis enregistre la
     * relecture correspondante.
     *
     * @return l'identifiant du relecteur désigné, ou {@link Optional#empty()} si
     *         aucun étudiant n'est éligible — auquel cas aucun relecteur n'est
     *         attaché.
     */
    @Override
    @Transactional
    public Optional<Long> assignerUnRelecteur(Long exerciceId, Long sessionId, Long auteurId) {
        List<Long> pool = presenceRepository.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiantId)
                .filter(etudiantId -> !etudiantId.equals(auteurId))
                .distinct()
                .toList();

        if (pool.isEmpty()) {
            return Optional.empty();
        }

        Long relecteurId = pool.get(aleatoire.nextInt(pool.size()));
        relectureRepository.save(new Relecture(exerciceId, relecteurId, auteurId));
        return Optional.of(relecteurId);
    }

    /**
     * Mission du relecteur : l'exercice qui lui est confié.
     *
     * @throws ExceptionMetier {@code 404 RELECTURE_INCONNUE} si aucune relecture
     *         n'est assignée à cet exercice, {@code 403 APPELANT_NON_AUTORISE}
     *         si l'appelant n'est pas le relecteur assigné (RG6).
     */
    @Transactional(readOnly = true)
    public MissionRelecteurReponse consulterMission(Long exerciceId, Long relecteurId) {
        Relecture relecture = relectureRepository.findByExerciceId(exerciceId)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.RELECTURE_INCONNUE, HttpStatus.NOT_FOUND,
                        "Aucune relecture n'est assignée à cet exercice."));

        if (!relecture.getRelecteurId().equals(relecteurId)) {
            throw new ExceptionMetier(CodeErreur.APPELANT_NON_AUTORISE, HttpStatus.FORBIDDEN,
                    "Cette relecture n'est pas confiée à cet étudiant.");
        }

        // La clé étrangère garantit l'existence de l'exercice d'une relecture.
        Exercice exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.EXERCICE_INCONNU, HttpStatus.NOT_FOUND,
                        "L'exercice %d est introuvable.".formatted(exerciceId)));

        return new MissionRelecteurReponse(relecture.getId(), exercice.getId(), exercice.getSessionId(),
                exercice.getLien(), relecture.getStatut());
    }

    /**
     * Rend la note et le commentaire d'une relecture assignée (EF6).
     *
     * <p>Ordre des contrôles, dicté par la nature de chaque refus :
     * <ol>
     *   <li>forme de la note → {@code 400 NOTE_INVALIDE} (RG7) : le refus porte
     *       sur la requête elle-même, comme le ferait la validation des DTO ;</li>
     *   <li>relecture inconnue → {@code 404} ;</li>
     *   <li>auto-relecture → {@code 403 AUTO_RELECTURE} (RG4) ;</li>
     *   <li>session clôturée → {@code 409 SESSION_CLOTUREE} (RG14), contrôlé avant
     *       « déjà rendue » : une session clôturée interdit aussi la correction de
     *       l'EF7, renvoyer vers elle serait trompeur ;</li>
     *   <li>note déjà rendue → {@code 409 RELECTURE_DEJA_RENDUE} : la correction
     *       d'une note rendue est l'EF7, sous un autre chemin.</li>
     * </ol>
     *
     * <p>Dans la même transaction, l'exercice relu passe au statut {@code RELU}
     * (D4) : une note enregistrée et un exercice resté « en attente » seraient
     * incohérents.
     */
    @Transactional
    public RelectureRendueReponse rendre(Long relectureId, SoumissionRelectureRequete requete) {
        int note = validerNote(requete.note());

        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.RELECTURE_INCONNUE, HttpStatus.NOT_FOUND,
                        "La relecture %d est introuvable.".formatted(relectureId)));

        // RG4, dernier filet : l'assignation écarte déjà l'auteur du pool et
        // ck_relecture_pas_auto_relecture interdit la ligne en base. Ce contrôle
        // couvre le cas d'une donnée introduite hors application.
        if (relecture.getAuteurId().equals(relecture.getRelecteurId())) {
            throw new ExceptionMetier(CodeErreur.AUTO_RELECTURE, HttpStatus.FORBIDDEN,
                    "Un étudiant ne peut pas rendre une relecture de son propre exercice.");
        }

        Exercice exercice = exerciceRepository.findById(relecture.getExerciceId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.EXERCICE_INCONNU, HttpStatus.NOT_FOUND,
                        "L'exercice %d est introuvable.".formatted(relecture.getExerciceId())));

        // RG14 : la clôture gèle toute notation de la session. Contrôlé avant
        // « déjà rendue », car une session clôturée interdit aussi la correction
        // de l'EF7 : renvoyer l'appelant vers elle serait trompeur.
        Session session = sessionRepository.findById(exercice.getSessionId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.NOT_FOUND,
                        "La session %d est inconnue.".formatted(exercice.getSessionId())));
        if (session.isCloturee()) {
            throw new ExceptionMetier(CodeErreur.SESSION_CLOTUREE, HttpStatus.CONFLICT,
                    "La session est clôturée : plus aucune note ne peut être créée ni corrigée.");
        }

        if (relecture.getStatut() == StatutRelecture.RENDUE) {
            throw new ExceptionMetier(CodeErreur.RELECTURE_DEJA_RENDUE, HttpStatus.CONFLICT,
                    "Cette relecture a déjà été rendue.");
        }

        relecture.rendre(note, requete.commentaire());
        relectureRepository.save(relecture);

        exercice.marquerRelu();

        return new RelectureRendueReponse(relecture.getId(), exercice.getId(), relecture.getNote(),
                relecture.getCommentaire(), relecture.getStatut());
    }

    /**
     * Corrige une note déjà rendue (EF7), tant que la session n'est pas clôturée
     * (RG8).
     *
     * <p>Ordre des contrôles, aligné sur celui de {@link #rendre} :
     * <ol>
     *   <li>forme de la note → {@code 400 NOTE_INVALIDE} (RG7) ;</li>
     *   <li>relecture inconnue → {@code 404} ;</li>
     *   <li>auto-relecture → {@code 403 AUTO_RELECTURE} (RG4, garde-fou) ;</li>
     *   <li>session clôturée → {@code 409 SESSION_CLOTUREE} (RG8), contrôlé avant
     *       l'état de la relecture : une session clôturée interdit toute notation,
     *       correction comprise ;</li>
     *   <li>relecture jamais rendue → {@code 409 RELECTURE_NON_RENDUE} : il n'y a
     *       rien à corriger, l'EF6 est l'opération qui convient.</li>
     * </ol>
     *
     * <p><strong>RG8</strong> : la note remplacée est archivée dans
     * {@code correction_relecture} <em>avant</em> d'être remplacée, dans la même
     * transaction — une correction qui aurait échoué après l'archivage ne laisse
     * donc rien derrière elle. La note courante reste celle de {@code relecture},
     * et l'exercice reste {@code RELU} (D4) : seul le contenu de la note change.
     */
    @Transactional
    public RelectureRendueReponse corriger(Long relectureId, SoumissionRelectureRequete requete) {
        int note = validerNote(requete.note());

        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.RELECTURE_INCONNUE, HttpStatus.NOT_FOUND,
                        "La relecture %d est introuvable.".formatted(relectureId)));

        // Même garde-fou que pour le rendu (RG4) : inatteignable par l'API, mais
        // une correction ne doit pas davantage porter sur sa propre copie.
        if (relecture.getAuteurId().equals(relecture.getRelecteurId())) {
            throw new ExceptionMetier(CodeErreur.AUTO_RELECTURE, HttpStatus.FORBIDDEN,
                    "Un étudiant ne peut pas corriger une relecture de son propre exercice.");
        }

        Exercice exercice = exerciceRepository.findById(relecture.getExerciceId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.EXERCICE_INCONNU, HttpStatus.NOT_FOUND,
                        "L'exercice %d est introuvable.".formatted(relecture.getExerciceId())));

        Session session = sessionRepository.findById(exercice.getSessionId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.NOT_FOUND,
                        "La session %d est inconnue.".formatted(exercice.getSessionId())));
        if (session.isCloturee()) {
            throw new ExceptionMetier(CodeErreur.SESSION_CLOTUREE, HttpStatus.CONFLICT,
                    "La session est clôturée : plus aucune note ne peut être créée ni corrigée.");
        }

        if (relecture.getStatut() != StatutRelecture.RENDUE) {
            throw new ExceptionMetier(CodeErreur.RELECTURE_NON_RENDUE, HttpStatus.CONFLICT,
                    "Cette relecture n'a pas encore été rendue : rendez d'abord la note.");
        }

        correctionRelectureRepository.save(new CorrectionRelecture(relecture.getId(),
                relecture.getNote(), relecture.getCommentaire(), LocalDateTime.now()));

        relecture.corriger(note, requete.commentaire());
        relectureRepository.save(relecture);

        return new RelectureRendueReponse(relecture.getId(), exercice.getId(), relecture.getNote(),
                relecture.getCommentaire(), relecture.getStatut());
    }

    /**
     * Missions encore à rendre de ce relecteur (EF6), dans l'ordre d'assignation.
     *
     * <p>Seules les relectures au statut {@code EN_ATTENTE} sont renvoyées, ce que
     * dit le nom de l'opération ; le statut figure malgré tout dans la réponse,
     * conformément au schéma du contrat.
     */
    @Transactional(readOnly = true)
    public List<MissionRelecteurReponse> listerMissionsEnAttente(Long relecteurId) {
        List<Relecture> relectures = relectureRepository
                .findByRelecteurIdAndStatutOrderByIdAsc(relecteurId, StatutRelecture.EN_ATTENTE);
        if (relectures.isEmpty()) {
            return List.of();
        }

        Map<Long, Exercice> exercices = exerciceRepository
                .findAllById(relectures.stream().map(Relecture::getExerciceId).toList())
                .stream()
                .collect(Collectors.toMap(Exercice::getId, Function.identity()));

        return relectures.stream()
                .filter(relecture -> exercices.containsKey(relecture.getExerciceId()))
                .map(relecture -> {
                    Exercice exercice = exercices.get(relecture.getExerciceId());
                    return new MissionRelecteurReponse(relecture.getId(), exercice.getId(),
                            exercice.getSessionId(), exercice.getLien(), relecture.getStatut());
                })
                .toList();
    }

    /**
     * Notes reçues par un étudiant relu (EF8), les plus récentes d'abord.
     *
     * <p>Une relecture non encore rendue apparaît malgré tout, avec une note et un
     * commentaire nuls (RG9) : l'étudiant voit que son exercice est en attente au
     * lieu de le croire ignoré. Aucune identité de relecteur n'est exposée (RG6),
     * et l'exercice suffit à identifier la ligne côté étudiant.
     *
     * <p>Un exercice <strong>sans relecture assignée</strong> (aucun étudiant
     * éligible au dépôt, EF5) n'apparaît pas : cette opération liste des
     * relectures, pas des exercices.
     */
    @Transactional(readOnly = true)
    public List<NoteRecueReponse> listerRelecturesRecues(Long etudiantId) {
        return relectureRepository.findByAuteurIdOrderByIdDesc(etudiantId).stream()
                .map(relecture -> new NoteRecueReponse(relecture.getExerciceId(), relecture.getStatut(),
                        relecture.getNote(), relecture.getCommentaire()))
                .toList();
    }

    /**
     * RG7 : la note doit être un entier de 0 à 20. Une valeur absente, décimale
     * ({@code 15.5}) ou hors bornes ({@code -1}, {@code 21}) est refusée en
     * {@code 400 NOTE_INVALIDE}, comme le prévoit le contrat.
     */
    private int validerNote(BigDecimal note) {
        if (note == null
                || note.stripTrailingZeros().scale() > 0
                || note.compareTo(NOTE_MINIMALE) < 0
                || note.compareTo(NOTE_MAXIMALE) > 0) {
            throw new ExceptionMetier(CodeErreur.NOTE_INVALIDE, HttpStatus.BAD_REQUEST,
                    "La note doit être un entier compris entre 0 et 20.");
        }
        return note.intValueExact();
    }
}
