package com.kfokam48.app.features.presence.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/presences/manuelles} (EF10). Les deux champs sont
 * imposés par le contrat ({@code sessionId}, {@code etudiantId}).
 *
 * <p>La {@code source} n'y figure pas, et c'est délibéré : elle est décidée par
 * l'opération appelée (RG12), jamais par le client. Un appelant ne peut donc pas
 * fabriquer une présence « étudiant » en passant par le guichet du formateur, ni
 * l'inverse. Comme le contrat ne prévoit pas de champ {@code code}, l'ajout
 * manuel ne dépend d'aucun code de présence.
 */
public record AjoutPresenceManuelleRequete(

        @NotNull(message = "La session est obligatoire.")
        Long sessionId,

        @NotNull(message = "L'étudiant est obligatoire.")
        Long etudiantId) {
}
