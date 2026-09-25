package com.kfokam48.app.features.exercice.application.dto;

import com.kfokam48.app.features.exercice.domain.entity.StatutExercice;

/**
 * Réponse {@code 201} de {@code POST /api/exercices} (EF3) : {@code { id, statut }}.
 * Aucune entité JPA n'est exposée en JSON (B3).
 */
public record ExerciceDeposeReponse(Long id, StatutExercice statut) {
}
