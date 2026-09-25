package com.kfokam48.app.features.promotion.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test d'intégration du prérequis Q1 du contrat, dont dépend l'issue #9 :
 * l'étudiant est choisi dans une liste, sans authentification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/promotions/{id}/etudiants renvoie les étudiants de la promotion")
    void liste_les_etudiants_de_la_promotion() throws Exception {
        mockMvc.perform(get("/api/promotions/1/etudiants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].prenom").isNotEmpty())
                .andExpect(jsonPath("$[0].nom").isNotEmpty())
                // DTO minimal : ni email ni entité JPA n'est exposé (B3).
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].promotionId").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/promotions/{id}/etudiants avec une promotion inconnue répond 404")
    void promotion_inconnue_renvoie_404() throws Exception {
        mockMvc.perform(get("/api/promotions/999999/etudiants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
