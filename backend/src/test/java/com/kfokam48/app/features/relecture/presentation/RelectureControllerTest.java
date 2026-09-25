package com.kfokam48.app.features.relecture.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test d'intégration (B6) de l'EF6 : {@code POST /api/relectures/{id}} (opération
 * imposée) et {@code GET /api/relecteurs/{etudiantId}/relectures-en-attente}.
 *
 * <p>Le cas {@code 403 AUTO_RELECTURE} n'a pas de test d'intégration, et ce n'est
 * pas un oubli : {@code ck_relecture_pas_auto_relecture} interdit d'écrire la
 * ligne qui le déclencherait (ce que prouve {@code RelectureContraintesTest}).
 * Le garde-fou reste couvert par un test unitaire du service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelectureControllerTest {

    private static final long PROMOTION_DEMO = 1L;
    private static final String LIEN = "https://exemple.org/exercice-ef6.pdf";
    private static final String COMMENTAIRE = "Bien argumenté, conclusion à préciser.";

    /** Le relecteur est l'étudiant 2 : l'auteur (1) est présent mais écarté (RG4). */
    private static final long RELECTEUR_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest(name = "note acceptée : {0}")
    @ValueSource(strings = {"0", "20", "15"})
    @DisplayName("EF6 : une note entière de 0 à 20 rend la relecture et répond 200")
    void rend_la_note(String note) throws Exception {
        Contexte contexte = preparerScenario();

        mockMvc.perform(post("/api/relectures/{id}", contexte.relectureId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("note", Integer.parseInt(note), "commentaire", COMMENTAIRE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relectureId").value((int) contexte.relectureId()))
                .andExpect(jsonPath("$.exerciceId").value((int) contexte.exerciceId()))
                .andExpect(jsonPath("$.note").value(Integer.parseInt(note)))
                .andExpect(jsonPath("$.commentaire").value(COMMENTAIRE))
                .andExpect(jsonPath("$.statut").value("RENDUE"))
                // RG6 : ni l'auteur ni le relecteur n'apparaissent dans la réponse.
                .andExpect(jsonPath("$.auteurId").doesNotExist())
                .andExpect(jsonPath("$.relecteurId").doesNotExist());

        assertThat(statutDeLaRelecture(contexte.relectureId())).isEqualTo("RENDUE");
        assertThat(noteEnregistree(contexte.relectureId())).isEqualTo(Integer.parseInt(note));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT rendu_at FROM relecture WHERE id = ?", Timestamp.class, contexte.relectureId()))
                .isNotNull();
        // D4 : l'exercice relu quitte EN_ATTENTE_RELECTURE.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT statut FROM exercice WHERE id = ?", String.class, contexte.exerciceId()))
                .isEqualTo("RELU");
    }

    @ParameterizedTest(name = "note refusée : {0}")
    @ValueSource(strings = {"-1", "21", "15.5"})
    @DisplayName("EF6 : une note hors bornes ou décimale est refusée en 400 NOTE_INVALIDE")
    void refuse_une_note_invalide(String note) throws Exception {
        Contexte contexte = preparerScenario();
        // Valeur envoyée en JSON brut : 15.5 doit être refusé par le service avec le
        // code du contrat, et non échouer à la désérialisation (autre code).
        String corps = "{\"note\": " + note + ", \"commentaire\": "
                + objectMapper.writeValueAsString(COMMENTAIRE) + "}";

        mockMvc.perform(post("/api/relectures/{id}", contexte.relectureId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());

        // RG7 par la validation : rien n'a été enregistré.
        assertThat(statutDeLaRelecture(contexte.relectureId())).isEqualTo("EN_ATTENTE");
    }

    @Test
    @DisplayName("EF6 : une note absente est refusée en 400 NOTE_INVALIDE")
    void refuse_une_note_absente() throws Exception {
        Contexte contexte = preparerScenario();

        mockMvc.perform(post("/api/relectures/{id}", contexte.relectureId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("commentaire", COMMENTAIRE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    @DisplayName("EF6 : un commentaire absent est refusé en 400 avec le message du DTO")
    void refuse_un_commentaire_absent() throws Exception {
        Contexte contexte = preparerScenario();

        mockMvc.perform(post("/api/relectures/{id}", contexte.relectureId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": 15}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_INVALIDE"))
                .andExpect(jsonPath("$.message").value("Le commentaire est obligatoire."));
    }

    @Test
    @DisplayName("EF6 : une seconde note sur la même relecture est refusée en 409")
    void refuse_une_seconde_note() throws Exception {
        Contexte contexte = preparerScenario();
        rendre(contexte.relectureId(), 15);

        mockMvc.perform(post("/api/relectures/{id}", contexte.relectureId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("note", 18, "commentaire", COMMENTAIRE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));

        // La note rendue est conservée : sa correction relève de l'EF7.
        assertThat(noteEnregistree(contexte.relectureId())).isEqualTo(15);
    }

    @Test
    @DisplayName("EF6 : une relecture inconnue est refusée en 404 RELECTURE_INCONNUE")
    void refuse_une_relecture_inconnue() throws Exception {
        mockMvc.perform(post("/api/relectures/{id}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("note", 15, "commentaire", COMMENTAIRE))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }

    @Test
    @DisplayName("EF6 : le relecteur retrouve sa mission, qui disparaît de la liste une fois rendue")
    void liste_les_missions_en_attente() throws Exception {
        Contexte contexte = preparerScenario();

        // La base H2 est partagée par toutes les classes de test : on vérifie la
        // présence de NOTRE mission plutôt qu'un nombre total d'éléments.
        JsonNode missions = listerMissionsEnAttente(RELECTEUR_ID);
        JsonNode mission = missionDe(missions, contexte.relectureId());
        if (mission == null) {
            throw new AssertionError("La mission %d devrait figurer dans la liste du relecteur %d"
                    .formatted(contexte.relectureId(), RELECTEUR_ID));
        }

        assertThat(mission.get("exerciceId").asLong()).isEqualTo(contexte.exerciceId());
        assertThat(mission.get("sessionId").asLong()).isEqualTo(contexte.sessionId());
        assertThat(mission.get("lien").asText()).isEqualTo(LIEN);
        assertThat(mission.get("statut").asText()).isEqualTo("EN_ATTENTE");
        // RG6 : l'identité de l'auteur n'est jamais exposée au relecteur.
        assertThat(missions.findValues("auteurId")).isEmpty();

        rendre(contexte.relectureId(), 15);

        assertThat(missionDe(listerMissionsEnAttente(RELECTEUR_ID), contexte.relectureId())).isNull();
    }

    @Test
    @DisplayName("EF6 : un relecteur sans mission reçoit une liste vide (le contrat ne déclare que 200)")
    void liste_vide_pour_un_relecteur_sans_mission() throws Exception {
        mockMvc.perform(get("/api/relecteurs/{etudiantId}/relectures-en-attente", 999_999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------

    private record Contexte(long sessionId, long exerciceId, long relectureId) {
    }

    /** Ouvre une session, fait marquer 1 puis 2, dépose pour 1 : la relecture revient à 2. */
    private Contexte preparerScenario() throws Exception {
        String code = ouvrirSession("Cours EF6");
        long sessionId = idDeLaSession(code);
        marquerPresence(code, 1L);
        marquerPresence(code, 2L);
        long exerciceId = deposer(sessionId, 1L);

        Long relectureId = jdbcTemplate.queryForObject(
                "SELECT id FROM relecture WHERE exercice_id = ?", Long.class, exerciceId);
        return new Contexte(sessionId, exerciceId, relectureId == null ? -1L : relectureId);
    }

    private void rendre(long relectureId, int note) throws Exception {
        mockMvc.perform(post("/api/relectures/{id}", relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("note", note, "commentaire", COMMENTAIRE))))
                .andExpect(status().isOk());
    }

    private JsonNode listerMissionsEnAttente(long etudiantId) throws Exception {
        String corps = mockMvc.perform(get("/api/relecteurs/{etudiantId}/relectures-en-attente", etudiantId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps);
    }

    /** La mission portant cet identifiant, ou {@code null} si la liste ne la contient pas. */
    private JsonNode missionDe(JsonNode missions, long relectureId) {
        for (JsonNode mission : missions) {
            if (mission.get("relectureId").asLong() == relectureId) {
                return mission;
            }
        }
        return null;
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

    private long deposer(long sessionId, long etudiantId) throws Exception {
        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("sessionId", sessionId, "etudiantId", etudiantId, "lien", LIEN))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private String statutDeLaRelecture(long relectureId) {
        return jdbcTemplate.queryForObject("SELECT statut FROM relecture WHERE id = ?", String.class, relectureId);
    }

    private Integer noteEnregistree(long relectureId) {
        return jdbcTemplate.queryForObject("SELECT note FROM relecture WHERE id = ?", Integer.class, relectureId);
    }
}
