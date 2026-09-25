package com.kfokam48.app.features.relecture.application.service;

import java.util.Optional;

/**
 * Contrat d'assignation d'un relecteur, seul point par lequel la feature
 * {@code exercice} touche à la relecture (EF5).
 *
 * <p>Le dépôt d'un exercice (EF3) doit déclencher l'assignation sans connaître
 * la feature {@code relecture} : l'appelant ne reçoit donc que l'identifiant du
 * relecteur désigné, jamais l'entité {@code Relecture}. Deux conséquences :
 * <ul>
 *   <li>le couplage entre features passe par ce contrat, et non par la classe
 *       {@link RelectureService} ;</li>
 *   <li>le comportement d'assignation peut être remplacé dans les tests unitaires
 *       d'EF3 sans démarrer Spring.</li>
 * </ul>
 */
public interface AssignateurRelecteur {

    /**
     * Désigne un relecteur pour un exercice fraîchement déposé (RG4, RG5, RG13).
     *
     * @return l'identifiant du relecteur désigné, ou {@link Optional#empty()} si
     *         aucun étudiant n'est éligible — auquel cas aucun relecteur n'est
     *         attaché et l'exercice reste au statut {@code DEPOSE}.
     */
    Optional<Long> assignerUnRelecteur(Long exerciceId, Long sessionId, Long auteurId);
}
