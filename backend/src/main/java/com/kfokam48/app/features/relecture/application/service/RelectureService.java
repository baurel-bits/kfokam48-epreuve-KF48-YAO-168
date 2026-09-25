package com.kfokam48.app.features.relecture.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import com.kfokam48.app.features.exercice.domain.repository.ExerciceRepository;
import com.kfokam48.app.features.presence.domain.entity.Presence;
import com.kfokam48.app.features.presence.domain.repository.PresenceRepository;
import com.kfokam48.app.features.relecture.application.dto.MissionRelecteurReponse;
import com.kfokam48.app.features.relecture.domain.entity.Relecture;
import com.kfokam48.app.features.relecture.domain.repository.RelectureRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
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
 */
@Service
public class RelectureService implements AssignateurRelecteur {

    private final RelectureRepository relectureRepository;
    private final PresenceRepository presenceRepository;
    private final ExerciceRepository exerciceRepository;
    private final SecureRandom aleatoire = new SecureRandom();

    public RelectureService(RelectureRepository relectureRepository,
                            PresenceRepository presenceRepository,
                            ExerciceRepository exerciceRepository) {
        this.relectureRepository = relectureRepository;
        this.presenceRepository = presenceRepository;
        this.exerciceRepository = exerciceRepository;
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
}
