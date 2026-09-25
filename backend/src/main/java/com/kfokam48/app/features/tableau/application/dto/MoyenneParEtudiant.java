package com.kfokam48.app.features.tableau.application.dto;

/**
 * Moyenne des notes reçues par un étudiant (EF9), calculée par la base.
 *
 * <p>{@code moyenne} est nulle quand l'étudiant n'a encore aucune note rendue :
 * le contrat impose cette distinction (« moyenne renvoyée est {@code null} et non
 * {@code 0} »), et le SQL la produit naturellement ({@code AVG} sur zéro ligne).
 */
public record MoyenneParEtudiant(Long etudiantId, Double moyenne) {
}
