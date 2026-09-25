package com.kfokam48.app.features.session.application.dto;

import java.time.LocalDateTime;

/**
 * Réponse {@code 201} de {@code POST /api/sessions} (EF1). Aucune entité JPA n'est
 * exposée en JSON (B3) et le client ne recalcule rien (F3).
 */
public record SessionOuverteReponse(Long id, String code, LocalDateTime ouvertureAt, LocalDateTime expirationAt) {
}
