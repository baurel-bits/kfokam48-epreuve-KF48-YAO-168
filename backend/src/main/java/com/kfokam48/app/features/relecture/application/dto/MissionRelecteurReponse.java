package com.kfokam48.app.features.relecture.application.dto;

import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;

/**
 * Réponse {@code 200} de {@code GET /api/exercices/{exerciceId}/relecteur} (EF5).
 *
 * <p>Contrat : {@code { relectureId, exerciceId, sessionId, lien, statut }}.
 * L'identité de l'auteur comme celle du relecteur n'y figurent pas : le relecteur
 * sait ce qu'il doit relire, jamais qui l'a écrit (RG6 dans l'autre sens).
 */
public record MissionRelecteurReponse(
        Long relectureId,
        Long exerciceId,
        Long sessionId,
        String lien,
        StatutRelecture statut) {
}
