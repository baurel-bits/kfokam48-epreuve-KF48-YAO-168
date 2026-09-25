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

    @Test
    @DisplayName("POST /api/presences : 5 codes inconnus bloquent l'étudiant 2 minutes (429)")
    void cinq_codes_inconnus_renvoient_429() throws Exception {
        long etudiantId = 5L;

        for (int tentative = 1; tentative <= 5; tentative++) {
            mockMvc.perform(post("/api/presences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpsPresence("ZZZZZZ", etudiantId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }

        // 6e échec : le blocage RG3 prime, le code n'est même plus évalué.
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence("ZZZZZZ", etudiantId)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---------- EF10 : le formateur ajoute une présence manuellement ----------

    @Test
    @DisplayName("EF10 : POST /api/presences/manuelles répond 201 avec source = FORMATEUR")
    void ajout_manuel_repond_201() throws Exception {
        long sessionId = idDeLaSession(ouvrirSession("Cours EF10 nominal"));

        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").value((int) sessionId))
                .andExpect(jsonPath("$.etudiantId").value(1))
                .andExpect(jsonPath("$.source").value("FORMATEUR"));

        Integer enregistrees = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM presence WHERE session_id = ? AND etudiant_id = ? AND source = 'FORMATEUR'",
                Integer.class, sessionId, 1L);
        assertThat(enregistrees).isEqualTo(1);
    }

    @Test
    @DisplayName("EF10 : l'ajout manuel fonctionne avec un code de présence expiré (aucun code fourni)")
    void ajout_manuel_sans_code_valide() throws Exception {
        String code = ouvrirSession("Cours EF10 expiré");
        expirerLaSession(code);
        long sessionId = idDeLaSession(code);

        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 3L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    @DisplayName("EF10/RG14 : l'ajout manuel reste possible après clôture — RG14 ne gèle que dépôts et notation")
    void ajout_manuel_apres_cloture() throws Exception {
        long sessionId = idDeLaSession(ouvrirSession("Cours EF10 après clôture"));

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 4L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    @DisplayName("EF10 : un doublon répond 409, même si la première présence vient de l'étudiant")
    void ajout_manuel_doublon_repond_409() throws Exception {
        String code = ouvrirSession("Cours EF10 doublon");
        long sessionId = idDeLaSession(code);

        // L'étudiant marque lui-même sa présence (EF2), avec le code…
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsPresence(code, 2L)))
                .andExpect(status().isCreated());

        // …et le formateur ne peut pas la doubler : l'unicité ne dépend pas de la source.
        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 2L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("EF10 : session inconnue → 404 SESSION_INCONNUE, étudiant inconnu → 404 ETUDIANT_INCONNU")
    void ajout_manuel_references_inconnues() throws Exception {
        long sessionId = idDeLaSession(ouvrirSession("Cours EF10 références"));

        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", 999_999L, "etudiantId", 1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 999_999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("EF10 : un corps sans étudiant est refusé au format imposé (VALIDATION_INVALIDE)")
    void ajout_manuel_corps_incomplet() throws Exception {
        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_INVALIDE"))
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
