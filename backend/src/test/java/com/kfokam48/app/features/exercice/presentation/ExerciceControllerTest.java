package com.kfokam48.app.features.exercice.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * (EF3) et de {@code PUT /api/exercices/{id}/lien} (EF4), sur base H2 en mémoire,
 * schéma créé par les migrations Flyway.
 * Les sessions sont ouvertes par l'API imposée de l'EF1 (issue #8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExerciceControllerTest {

    private static final long PROMOTION_DEMO = 1L;
    private static final String LIEN_VALIDE = "https://exemple.org/exercice-de-test.pdf";
    private static final String LIEN_REMPLACEMENT = "https://exemple.org/exercice-corrige.pdf";

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

    // ---------- EF4 : remplacer le lien tant qu'aucune relecture n'a commencé ----------

    @Test
    @DisplayName("EF4 : PUT /api/exercices/{id}/lien remplace le lien, statut inchangé")
    void remplace_le_lien() throws Exception {
        long sessionId = ouvrirSession("Cours EF4 nominal");
        long exerciceId = deposer(sessionId, 1L, LIEN_VALIDE);

        mockMvc.perform(put("/api/exercices/{id}/lien", exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lien", LIEN_REMPLACEMENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) exerciceId))
                // D4 : remplacer un lien ne franchit aucune transition d'état.
                .andExpect(jsonPath("$.statut").value("DEPOSE"));

        assertThat(lienEnregistre(exerciceId)).isEqualTo(LIEN_REMPLACEMENT);
        assertThat(statutDeLExercice(exerciceId)).isEqualTo("DEPOSE");
    }

    @Test
    @DisplayName("RG11 : une relecture commencée refuse le remplacement en 409 RELECTURE_COMMENCEE")
    void remplacement_refuse_apres_assignation() throws Exception {
        long sessionId = ouvrirSession("Cours EF4 relecture commencée");
        // L'auteur et un autre étudiant sont présents : le dépôt confie donc la
        // relecture à ce dernier (EF5), avant toute note.
        marquerPresence(sessionId, 1L);
        marquerPresence(sessionId, 2L);
        long exerciceId = deposer(sessionId, 1L, LIEN_VALIDE);
        assertThat(statutDeLExercice(exerciceId)).isEqualTo("EN_ATTENTE_RELECTURE");

        mockMvc.perform(put("/api/exercices/{id}/lien", exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lien", LIEN_REMPLACEMENT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_COMMENCEE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());

        // Le lien d'origine n'a pas bougé.
        assertThat(lienEnregistre(exerciceId)).isEqualTo(LIEN_VALIDE);
    }

    @Test
    @DisplayName("RG14 (EF4) : après clôture, le remplacement répond 409 SESSION_CLOTUREE")
    void remplacement_refuse_apres_cloture() throws Exception {
        long sessionId = ouvrirSession("Cours EF4 clôturé");
        long exerciceId = deposer(sessionId, 1L, LIEN_VALIDE);
        cloturerLaSession(sessionId);

        mockMvc.perform(put("/api/exercices/{id}/lien", exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lien", LIEN_REMPLACEMENT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        assertThat(lienEnregistre(exerciceId)).isEqualTo(LIEN_VALIDE);
    }

    @Test
    @DisplayName("EF4 : un lien mal formé est refusé en 400 LIEN_INVALIDE")
    void remplacement_avec_un_lien_invalide() throws Exception {
        long sessionId = ouvrirSession("Cours EF4 lien invalide");
        long exerciceId = deposer(sessionId, 1L, LIEN_VALIDE);

        mockMvc.perform(put("/api/exercices/{id}/lien", exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lien", "pas-une-url"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(lienEnregistre(exerciceId)).isEqualTo(LIEN_VALIDE);
    }

    @Test
    @DisplayName("EF4 : un exercice inconnu est refusé en 404 EXERCICE_INCONNU")
    void remplacement_d_un_exercice_inconnu() throws Exception {
        mockMvc.perform(put("/api/exercices/{id}/lien", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lien", LIEN_REMPLACEMENT))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
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

    /** Dépose un exercice et renvoie son identifiant, en vérifiant la création. */
    private long deposer(long sessionId, long etudiantId, String lien) throws Exception {
        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepot(sessionId, etudiantId, lien)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    /** Marque une présence par le code réel de la session (opération imposée EF2). */
    private void marquerPresence(long sessionId, long etudiantId) throws Exception {
        String code = jdbcTemplate.queryForObject(
                "SELECT code FROM session WHERE id = ?", String.class, sessionId);

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("code", code, "etudiantId", etudiantId))))
                .andExpect(status().isCreated());
    }

    private String lienEnregistre(long exerciceId) {
        return jdbcTemplate.queryForObject(
                "SELECT lien FROM exercice WHERE id = ?", String.class, exerciceId);
    }

    private String statutDeLExercice(long exerciceId) {
        return jdbcTemplate.queryForObject(
                "SELECT statut FROM exercice WHERE id = ?", String.class, exerciceId);
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
