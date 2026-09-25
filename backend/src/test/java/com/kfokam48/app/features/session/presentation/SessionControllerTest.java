package com.kfokam48.app.features.session.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.app.features.session.application.dto.SessionOuverteReponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test d'intégration (B6) de l'opération imposée {@code POST /api/sessions}
 * (EF1) et de la clôture {@code POST /api/sessions/{id}/cloture} (EF11) sur base
 * H2 en mémoire, schéma créé par les migrations Flyway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerTest {

    /** RFC 3339 : un instant doit porter un décalage, `Z` ou `±HH:MM`. */
    private static final String OFFSET_RFC_3339 = ".*(Z|[+-][0-9]{2}:[0-9]{2})$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("POST /api/sessions répond 201 avec expirationAt = ouvertureAt + 15 min")
    void ouvre_une_session_et_renvoie_201_avec_expiration_a_15_minutes() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Cours du 25 septembre","promotionId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.ouvertureAt").isNotEmpty())
                .andExpect(jsonPath("$.expirationAt").isNotEmpty())
                .andExpect(jsonPath("$.ouvertureAt", matchesPattern(OFFSET_RFC_3339)))
                .andExpect(jsonPath("$.expirationAt", matchesPattern(OFFSET_RFC_3339)))
                .andReturn();

        SessionOuverteReponse reponse = objectMapper.readValue(
                resultat.getResponse().getContentAsString(), SessionOuverteReponse.class);

        assertThat(reponse.id()).isNotNull();
        assertThat(reponse.code()).hasSize(6);
        assertThat(Duration.between(reponse.ouvertureAt(), reponse.expirationAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("POST /api/sessions respecte le format date-time du contrat (RFC 3339, décalage explicite)")
    void respecte_le_format_date_time_du_contrat() throws Exception {
        String corpsJson = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Cours du 25 septembre","promotionId":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode corps = objectMapper.readTree(corpsJson);
        // OffsetDateTime.parse est strict : il échoue si le décalage horaire est absent.
        assertThat(OffsetDateTime.parse(corps.get("ouvertureAt").asText())).isNotNull();
        assertThat(OffsetDateTime.parse(corps.get("expirationAt").asText())).isNotNull();
    }

    @Test
    @DisplayName("POST /api/sessions sans titre répond 400 au format imposé, sans stack trace")
    void refuse_une_session_sans_titre() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"promotionId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_INVALIDE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("Le navigateur de l'écran formateur est autorisé à appeler POST /api/sessions (CORS)")
    void autorise_l_origine_du_frontend() throws Exception {
        mockMvc.perform(options("/api/sessions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    @DisplayName("POST /api/sessions sans promotion répond 400 au format imposé")
    void refuse_une_session_sans_promotion() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Cours du 25 septembre"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_INVALIDE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/sessions avec une promotion inconnue répond 400 PROMOTION_INCONNUE")
    void refuse_une_promotion_inconnue() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Cours du 25 septembre","promotionId":999999}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---------- EF11 : clôture d'une session ----------

    @Test
    @DisplayName("EF11 : POST /api/sessions/{id}/cloture répond 200 {id, cloturee} — et non 201")
    void cloture_une_session() throws Exception {
        long sessionId = ouvrirUneSession();

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) sessionId))
                .andExpect(jsonPath("$.cloturee").value(true))
                // Le contrat ne déclare que deux champs : rien d'autre n'est exposé.
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(cloturee(sessionId)).isTrue();
    }

    @Test
    @DisplayName("EF11 : reclôturer une session clôturée reste un 200 (idempotent)")
    void recloture_idempotente() throws Exception {
        long sessionId = ouvrirUneSession();

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloturee").value(true));

        assertThat(cloturee(sessionId)).isTrue();
    }

    @Test
    @DisplayName("EF11 : une session inconnue répond 404 SESSION_INCONNUE au format imposé")
    void cloture_une_session_inconnue() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/cloture", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    /** Ouvre une session par l'API (EF1) et renvoie son identifiant. */
    private long ouvrirUneSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Cours EF11","promotionId":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private boolean cloturee(long sessionId) {
        Boolean valeur = jdbcTemplate.queryForObject(
                "SELECT cloturee FROM session WHERE id = ?", Boolean.class, sessionId);
        return Boolean.TRUE.equals(valeur);
    }
}
