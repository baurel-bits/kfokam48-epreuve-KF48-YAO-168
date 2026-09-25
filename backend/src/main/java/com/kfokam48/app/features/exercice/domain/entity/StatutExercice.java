package com.kfokam48.app.features.exercice.domain.entity;

/**
 * Cycle de vie d'un exercice (D4) : {@code DEPOSE} à l'ouverture du dépôt, puis
 * {@code EN_ATTENTE_RELECTURE} à l'assignation d'un relecteur (EF5) et
 * {@code RELU} une fois la note rendue (EF6).
 */
public enum StatutExercice {
    DEPOSE,
    EN_ATTENTE_RELECTURE,
    RELU
}
