package com.kfokam48.app.features.relecture.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Corps de {@code POST /api/relectures/{id}} (EF6), imposé par le contrat :
 * {@code { "note": entier de 0 à 20, "commentaire": chaîne }}.
 *
 * <p>La note est reçue en {@link BigDecimal} et non en {@code int} : c'est le seul
 * moyen de refuser une valeur décimale avec le code du contrat
 * ({@code 400 NOTE_INVALIDE}). Un champ {@code int} laisserait Jackson échouer à
 * la désérialisation, ce qui produirait {@code REQUETE_ILLISIBLE} — un code
 * différent de celui annoncé.
 */
public record SoumissionRelectureRequete(
        BigDecimal note,
        @NotBlank(message = "Le commentaire est obligatoire.")
        @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères.")
        String commentaire) {
}
