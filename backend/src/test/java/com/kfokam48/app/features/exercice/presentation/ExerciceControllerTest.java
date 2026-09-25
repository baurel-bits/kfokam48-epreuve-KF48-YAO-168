package com.kfokam48.app.features.exercice.presentation;

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
 * Test d'intégration (B6) de l'opération imposée {@code POST /api/exercices}
 * sur base H2 en mémoire, schéma créé par les migrations Flyway.
 * Les sessions sont ouvertes par l'API imposée de l'EF1 (issue #8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExerciceControllerTest {

    private static final long PROMOTION_DEMO = 1L;
    private static final String LIEN_VALIDE = "https://exemple.org/exercice-de-test.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("POST /api/exercices répond 201 avec le statut DEPOSE")
    void depose_un_exercice() throws Exception {
        long sessionId = ouvrirSession("Cours EF3 nominal");

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, 1L, LIEN_VALIDE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("DEPOSE"));

        Integer enregistres = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exercice WHERE session_id = ? AND etudiant_id = ?",
                Integer.class, sessionId, 1L);
        assertThat(enregistres).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/exercices avec un lien mal formé répond 400 LIEN_INVALIDE")
    void lien_invalide_renvoie_400() throws Exception {
        long sessionId = ouvrirSession("Cours EF3 lien invalide");

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, 2L, "pas-une-url")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/exercices deux fois pour le même étudiant répond 409 EXERCICE_DEJA_DEPOSE")
    void second_depot_renvoie_409() throws Exception {
        long sessionId = ouvrirSession("Cours EF3 doublon");

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, 3L, LIEN_VALIDE)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, 3L, "https://exemple.org/autre.pdf")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/exercices sur une session clôturée répond 409 SESSION_CLOTUREE (RG10/RG14)")
    void depot_apres_cloture_renvoie_409() throws Exception {
        long sessionId = ouvrirSession("Cours EF3 clôturé");
        cloturerLaSession(sessionId);

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, 4L, LIEN_VALIDE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("RG10 : le dépôt reste possible après l'expiration du code de présence")
    void depot_possible_apres_expiration_du_code() throws Exception {
        long sessionId = ouvrirSession("Cours EF3 tardif");
        faireExpirerLeCode(sessionId);

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, 5L, LIEN_VALIDE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    private long ouvrirSession(String titre) throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("titre", titre, "promotionId", PROMOTION_DEMO))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String code = objectMapper.readTree(corps).get("code").asText();
        Long id = jdbcTemplate.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
        return id == null ? -1L : id;
    }

    private String corpsDepot(long sessionId, long etudiantId, String lien) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("sessionId", sessionId, "etudiantId", etudiantId, "lien", lien));
    }

    /** Ouvre la session il y a 30 minutes : son code a expiré il y a 15 minutes. */
    private void faireExpirerLeCode(long sessionId) {
        LocalDateTime ouverture = LocalDateTime.now().minusMinutes(30);
        jdbcTemplate.update("UPDATE session SET ouverture_at = ?, expiration_at = ? WHERE id = ?",
                ouverture, ouverture.plusMinutes(15), sessionId);
    }

    private void cloturerLaSession(long sessionId) {
        jdbcTemplate.update("UPDATE session SET cloturee = TRUE WHERE id = ?", sessionId);
    }
}
