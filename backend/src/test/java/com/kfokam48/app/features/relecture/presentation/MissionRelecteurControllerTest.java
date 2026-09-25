package com.kfokam48.app.features.relecture.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Test d'intégration (B6) de l'EF5 : l'assignation déclenchée par le dépôt et
 * sa consultation par {@code GET /api/exercices/{exerciceId}/relecteur}.
 *
 * <p>Les scénarios sont construits pour être <strong>déterministes</strong> : le
 * pool ne contient qu'un seul étudiant éligible, le tirage aléatoire n'a donc
 * pas d'autre choix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MissionRelecteurControllerTest {

    private static final long PROMOTION_DEMO = 1L;
    private static final String LIEN = "https://exemple.org/exercice-ef5.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("EF5 : le relecteur est un étudiant présent, différent de l'auteur (RG4, RG5)")
    void assigne_un_present_autre_que_l_auteur() throws Exception {
        String code = ouvrirSession("Cours EF5 nominal");
        long sessionId = idDeLaSession(code);
        marquerPresence(code, 1L);
        marquerPresence(code, 2L);

        // L'auteur (1) est pourtant présent : il doit être écarté, laissant 2 seul éligible.
        long exerciceId = deposer(sessionId, 1L, LIEN);

        mockMvc.perform(get("/api/exercices/{exerciceId}/relecteur", exerciceId)
                        .param("relecteurId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relectureId").isNumber())
                .andExpect(jsonPath("$.exerciceId").value((int) exerciceId))
                .andExpect(jsonPath("$.sessionId").value((int) sessionId))
                .andExpect(jsonPath("$.lien").value(LIEN))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                // RG6 dans l'autre sens : le relecteur ne connaît pas l'auteur.
                .andExpect(jsonPath("$.auteurId").doesNotExist());
    }

    @Test
    @DisplayName("EF5 : le dépôt répond EN_ATTENTE_RELECTURE dès qu'un relecteur est assigné (D4)")
    void depot_avec_relecteur_passe_en_attente_de_relecture() throws Exception {
        String code = ouvrirSession("Cours EF5 statut");
        long sessionId = idDeLaSession(code);
        marquerPresence(code, 2L);

        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 1L, "lien", LIEN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Le relecteur assigné est le seul étudiant présent.
        long exerciceId = objectMapper.readTree(corps).get("id").asLong();
        mockMvc.perform(get("/api/exercices/{exerciceId}/relecteur", exerciceId)
                        .param("relecteurId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EF5 : sans étudiant présent, l'exercice reste sans relecteur")
    void sans_etudiant_present_aucun_relecteur() throws Exception {
        String code = ouvrirSession("Cours EF5 sans présent");
        long sessionId = idDeLaSession(code);

        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", 3L, "lien", LIEN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("DEPOSE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long exerciceId = objectMapper.readTree(corps).get("id").asLong();
        mockMvc.perform(get("/api/exercices/{exerciceId}/relecteur", exerciceId)
                        .param("relecteurId", "3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("RG6 : un autre appelant que le relecteur assigné reçoit 403 APPELANT_NON_AUTORISE")
    void autre_appelant_recoit_403() throws Exception {
        String code = ouvrirSession("Cours EF5 accès refusé");
        long sessionId = idDeLaSession(code);
        marquerPresence(code, 1L);
        marquerPresence(code, 2L);
        long exerciceId = deposer(sessionId, 1L, LIEN);

        mockMvc.perform(get("/api/exercices/{exerciceId}/relecteur", exerciceId)
                        .param("relecteurId", "3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPELANT_NON_AUTORISE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
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

    private long idDeLaSession(String code) {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
        return id == null ? -1L : id;
    }

    private void marquerPresence(String code, long etudiantId) throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("code", code, "etudiantId", etudiantId))))
                .andExpect(status().isCreated());
    }

    private long deposer(long sessionId, long etudiantId, String lien) throws Exception {
        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", etudiantId, "lien", lien))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(corps);
        return json.get("id").asLong();
    }
}
