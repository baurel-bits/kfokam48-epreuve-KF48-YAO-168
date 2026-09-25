package com.kfokam48.app.features.presence.application.dto;

import com.kfokam48.app.features.presence.domain.entity.SourcePresence;

/**
 * Réponse {@code 201} de {@code POST /api/presences} (EF2) :
 * {@code { id, sessionId, etudiantId, source }}. Aucune entité JPA n'est exposée
 * en JSON (B3).
 */
public record PresenceReponse(Long id, Long sessionId, Long etudiantId, SourcePresence source) {
}
