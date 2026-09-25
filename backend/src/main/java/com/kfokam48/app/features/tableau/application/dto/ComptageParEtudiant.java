package com.kfokam48.app.features.tableau.application.dto;

/**
 * Comptage agrégé par étudiant (EF9) : présences, exercices déposés ou
 * relectures en attente.
 *
 * <p>C'est le résultat d'un {@code GROUP BY} : une ligne par étudiant
 * <em>ayant</em> des données, ce qui évite une requête par étudiant. Les
 * étudiants absents de ce résultat sont complétés par zéro côté service — ils
 * doivent figurer dans le tableau même sans activité (critère d'acceptation
 * de l'EF9).
 */
public record ComptageParEtudiant(Long etudiantId, Long total) {
}
