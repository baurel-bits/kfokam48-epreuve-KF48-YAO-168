package com.kfokam48.app.features.tableau.application.dto;

/**
 * Ligne du tableau de bord du formateur (EF9) — exactement les six champs
 * déclarés par l'opération <strong>imposée</strong> {@code GET /api/tableau} :
 * aucun champ supplémentaire n'est exposé, ni l'email, ni le détail des
 * relectures (B2).
 *
 * <p>{@code moyenne} est la moyenne des notes <em>reçues</em> par l'étudiant
 * (moyenne des notes rendues sur ses propres exercices), nulle tant qu'aucune
 * note n'a été rendue — et non {@code 0}. Elle est arrondie à deux décimales par
 * le serveur : le frontend n'effectue aucun calcul (F3).
 *
 * <p>{@code relecturesEnAttente} compte les relectures des exercices de
 * l'étudiant qui ne sont pas encore rendues (RG9 : un exercice sans relecture
 * rendue doit apparaître comme tel dans le tableau).
 */
public record LigneTableauReponse(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        Double moyenne,
        long relecturesEnAttente) {
}
