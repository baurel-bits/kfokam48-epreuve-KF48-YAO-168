package com.kfokam48.app.features.relecture.domain.entity;

/**
 * État d'une relecture (D2) : {@code EN_ATTENTE} tant que la note n'est pas
 * rendue, {@code RENDUE} ensuite (EF6).
 */
public enum StatutRelecture {
    EN_ATTENTE,
    RENDUE
}
