package com.kfokam48.app.features.session.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/sessions}. Les noms de champs sont imposés par le
 * contrat d'API ({@code titre}, {@code promotionId}).
 */
public record CreationSessionRequete(

        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères.")
        String titre,

        @NotNull(message = "La promotion est obligatoire.")
        Long promotionId) {
}
