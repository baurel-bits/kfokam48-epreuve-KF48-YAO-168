package com.kfokam48.app.features.presence.domain.entity;

/**
 * Origine d'une présence (D2). La source n'est jamais choisie par le client :
 * elle est déterminée par l'opération appelée (EF2 → {@code ETUDIANT},
 * EF10 → {@code FORMATEUR}).
 */
public enum SourcePresence {
    ETUDIANT,
    FORMATEUR
}
