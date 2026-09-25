package com.kfokam48.app.features.tableau.application.dto;

/**
 * Projection d'identité d'un étudiant pour le tableau de bord (EF9).
 *
 * <p>Seules ces trois colonnes sont lues : charger l'entité {@code Etudiant}
 * entière pour n'en afficher que le nom serait le premier pas vers le N+1 que
 * l'EF9 doit éviter. Le libellé affiché est composé par le service
 * (« prénom nom »), pas par le client (F3).
 */
public record IdentiteEtudiant(Long etudiantId, String prenom, String nom) {
}
