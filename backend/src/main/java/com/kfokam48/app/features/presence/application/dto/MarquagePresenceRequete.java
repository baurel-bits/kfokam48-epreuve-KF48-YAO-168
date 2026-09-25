package com.kfokam48.app.features.presence.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/presences}. Les noms de champs sont imposés par le
 * contrat d'API ({@code code}, {@code etudiantId}).
 */
public record MarquagePresenceRequete(

        @NotBlank(message = "Le code de présence est obligatoire.")
        String code,

        @NotNull(message = "L'étudiant est obligatoire.")
        Long etudiantId) {
}
