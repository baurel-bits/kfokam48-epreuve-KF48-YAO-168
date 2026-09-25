package com.kfokam48.app.features.promotion.application.dto;

/**
 * Élément de {@code GET /api/promotions/{promotionId}/etudiants} :
 * {@code { id, prenom, nom }}. Ni l'email ni l'entité JPA ne sont exposés (B3).
 */
public record EtudiantResume(Long id, String prenom, String nom) {
}
