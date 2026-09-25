package com.kfokam48.app.features.session.application.dto;

import java.time.OffsetDateTime;

/**
 * Réponse {@code 201} de {@code POST /api/sessions} (EF1).
 *
 * <p>Les deux instants sont des {@link OffsetDateTime} : le contrat déclare
 * {@code format: date-time}, c'est-à-dire du RFC 3339, qui exige un décalage
 * horaire explicite ({@code +01:00} ou {@code Z}). Un {@code LocalDateTime}
 * serait sérialisé sans décalage et ne respecterait donc pas le contrat.
 *
 * <p>Aucune entité JPA n'est exposée en JSON (B3) et le client ne recalcule
 * rien : il affiche ces instants tels quels (F3).
 */
public record SessionOuverteReponse(Long id, String code, OffsetDateTime ouvertureAt, OffsetDateTime expirationAt) {
}
