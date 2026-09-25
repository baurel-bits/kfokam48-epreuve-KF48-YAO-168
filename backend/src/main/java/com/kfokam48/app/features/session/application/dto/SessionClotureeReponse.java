package com.kfokam48.app.features.session.application.dto;

/**
 * Réponse de {@code POST /api/sessions/{id}/cloture} (EF11) — les deux champs
 * exigés par le contrat, et rien de plus : ni le titre, ni le code de présence,
 * ni l'instant de clôture (le schéma ne les déclare pas, et la table n'a pas
 * d'horodatage de clôture : `session.cloturee` est un booléen).
 *
 * <p>{@code cloturee} est renvoyé par le serveur plutôt que déduit par l'appelant :
 * c'est lui qui fait foi, y compris lorsqu'une session déjà clôturée est
 * reclôturée (l'opération est idempotente).
 */
public record SessionClotureeReponse(Long id, boolean cloturee) {
}
