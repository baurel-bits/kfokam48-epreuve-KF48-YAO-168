package com.kfokam48.app.features.exercice.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/exercices}. Les noms de champs sont imposés par le
 * contrat d'API ({@code sessionId}, {@code etudiantId}, {@code lien}).
 *
 * <p>Le format du lien n'est pas validé ici mais dans le service : le contrat
 * impose le code d'erreur {@code LIEN_INVALIDE} pour un lien mal formé, alors
 * qu'une violation de contrainte Bean Validation produit {@code VALIDATION_INVALIDE}.
 */
public record DepotExerciceRequete(

        @NotNull(message = "La session est obligatoire.")
        Long sessionId,

        @NotNull(message = "L'étudiant est obligatoire.")
        Long etudiantId,

        String lien) {
}
