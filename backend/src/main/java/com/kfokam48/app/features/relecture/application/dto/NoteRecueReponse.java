package com.kfokam48.app.features.relecture.application.dto;

import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;

/**
 * Élément de {@code GET /api/etudiants/{etudiantId}/relectures-recues} (EF8).
 *
 * <p>Le contrat impose ces quatre champs et rien de plus : l'identité du
 * relecteur n'y figure jamais (RG6). Aucun autre identifiant n'est renvoyé, ni
 * celui de l'auteur — l'appelant sait seulement quel exercice est concerné.
 *
 * <p>{@code note} et {@code commentaire} restent nuls tant que la relecture est
 * {@code EN_ATTENTE} : l'exercice apparaît alors « en attente », sans note (RG9).
 */
public record NoteRecueReponse(
        Long exerciceId,
        StatutRelecture statut,
        Integer note,
        String commentaire) {
}
