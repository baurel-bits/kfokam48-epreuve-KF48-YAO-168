package com.kfokam48.app.features.presence.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test d'intégration (B6) de l'opération imposée {@code POST /api/presences}
 * sur base H2 en mémoire, schéma créé par les migrations Flyway.
 * Les sessions sont ouvertes par l'API imposée de l'EF1 (issue #8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceControllerTest {

    private static final long PROMOTION_DEMO = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("POST /api/presences répond 201 et enregistre la présence avec source = ETUDIANT")
    void marque_la_presence_avec_201() throws Exception {
        String code = ouvrirSession("Cours EF2 nominal");
        long sessionId = idDeLaSession(code);

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence(code, 1L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").value((int) sessionId))
                .andExpect(jsonPath("$.etudiantId").value(1))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));

        // La présence est bien persistée : elle alimentera le tableau du formateur (EF9).
        Integer enregistrees = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM presence WHERE session_id = ? AND etudiant_id = ? AND source = 'ETUDIANT'",
                Integer.class, sessionId, 1L);
        assertThat(enregistrees).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/presences avec un code inconnu répond 400 CODE_INCONNU")
    void code_inconnu_renvoie_400() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence("ZZZZZZ", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/presences deux fois pour le même étudiant répond 409 DEJA_PRESENT")
    void presence_en_double_renvoie_409() throws Exception {
        String code = ouvrirSession("Cours EF2 doublon");

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence(code, 2L)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence(code, 2L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/presences avec un code de plus de 15 minutes répond 410 CODE_EXPIRE")
    void code_expire_renvoie_410() throws Exception {
        String code = ouvrirSession("Cours EF2 expiré");
        expirerLaSession(code);

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence(code, 4L)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/presences : 5 échecs bloquent le couple 2 minutes, puis 429 TROP_DE_TENTATIVES")
    void cinq_echecs_renvoient_429() throws Exception {
        String code = ouvrirSession("Cours EF2 blocage");
        expirerLaSession(code);

        for (int tentative = 1; tentative <= 5; tentative++) {
            mockMvc.perform(post("/api/presences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpsPresence(code, 3L)))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
        }

        // RG3 : le blocage est contrôlé avant la validation du code (D3).
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence(code, 3L)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private String ouvrirSession(String titre) throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("titre", titre, "promotionId", PROMOTION_DEMO))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("code").asText();
    }

    private String corpsPresence(String code, long etudiantId) throws Exception {
        return objectMapper.writeValueAsString(Map.of("code", code, "etudiantId", etudiantId));
    }

    private long idDeLaSession(String code) {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
        return id == null ? -1L : id;
    }

    /** Ramène la session à une ouverture il y a 30 minutes : son code a expiré il y a 15 minutes. */
    private void expirerLaSession(String code) {
        LocalDateTime ouverture = LocalDateTime.now().minusMinutes(30);
        jdbcTemplate.update("UPDATE session SET ouverture_at = ?, expiration_at = ? WHERE code = ?",
                ouverture, ouverture.plusMinutes(15), code);
    }
}
