package com.kfokam48.app.features.session.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.app.features.session.application.dto.SessionOuverteReponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test d'intégration (B6) de l'opération imposée {@code POST /api/sessions}
 * sur base H2 en mémoire, schéma créé par les migrations Flyway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .andReturn();

        SessionOuverteReponse reponse = objectMapper.readValue(
                resultat.getResponse().getContentAsString(), SessionOuverteReponse.class);

        assertThat(reponse.id()).isNotNull();
        assertThat(reponse.code()).hasSize(6);
        assertThat(Duration.between(reponse.ouvertureAt(), reponse.expirationAt()))
                .isEqualTo(Duration.ofMinutes(15));
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
}
