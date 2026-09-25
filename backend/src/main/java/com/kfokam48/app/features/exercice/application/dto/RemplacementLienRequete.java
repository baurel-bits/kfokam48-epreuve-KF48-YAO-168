package com.kfokam48.app.features.exercice.application.dto;

/**
 * Corps de {@code PUT /api/exercices/{id}/lien} (EF4) : {@code { "lien": "https://…" }}.
 *
 * <p>Comme au dépôt (EF3), le lien n'est soumis à <strong>aucune</strong> contrainte
 * Bean Validation : le contrat impose le code {@code LIEN_INVALIDE} pour un lien
 * mal formé ou absent, alors qu'une violation de contrainte produirait
 * {@code VALIDATION_INVALIDE}. Le contrôle est donc fait dans le service.
 *
 * <p>L'identifiant de l'exercice est dans le chemin, et l'auteur du remplacement
 * n'est pas transmis : le sujet exclut l'authentification (Q1).
 */
public record RemplacementLienRequete(String lien) {
}
