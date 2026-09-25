package com.kfokam48.app.features.relecture.application.dto;

import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;

/**
 * Réponse {@code 200} de {@code POST /api/relectures/{id}} (EF6).
 *
 * <p>Le contrat ne décrit pas de corps pour cette opération ; celui-ci reprend
 * l'état réellement enregistré. Ni l'identité du relecteur ni celle de l'auteur
 * n'y figurent (RG6).
 */
public record RelectureRendueReponse(
        Long relectureId,
        Long exerciceId,
        Integer note,
        String commentaire,
        StatutRelecture statut) {
}
