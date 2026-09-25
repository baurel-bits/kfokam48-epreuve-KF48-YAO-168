package com.kfokam48.app.features.tableau.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
 * Test d'intégration (B6) de l'EF9 : {@code GET /api/tableau} (opération imposée).
 *
 * <p>Chaque test se construit sa <strong>propre promotion</strong>, avec ses
 * propres étudiants : la base H2 est partagée entre classes de test, un comptage
 * global serait donc faux. Les identifiants sont uniques (nom de promotion et
 * email d'étudiant sont contraints par {@code UNIQUE}), ce qui rend les
 * assertions exactes au lieu d'approximatives.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TableauControllerTest {

    private static final String LIEN = "https://exemple.org/exercice-ef9.pdf";

    /** Sous-ensemble des champs réellement déclarés par le contrat (B2). */
    private static final Set<String> CHAMPS_DU_CONTRAT = Set.of(
            "etudiantId", "nom", "presences", "exercicesDeposes", "moyenne", "relecturesEnAttente");

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("EF9 : une promotion inconnue est refusée en 404 PROMOTION_INCONNUE, au format imposé")
    void promotion_inconnue() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                // B4 : jamais de stack trace.
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("EF9 : sans promotionId, la requête est refusée en 400 DEMANDE_INVALIDE, au format imposé")
    void promotion_id_absent() throws Exception {
        mockMvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEMANDE_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("EF9 : les étudiants sans activité figurent dans le tableau, moyenne nulle et non zéro")
    void etudiants_sans_activite_figurent_dans_le_tableau() throws Exception {
        List<Long> etudiants = creerPromotion(2);

        JsonNode tableau = tableauDe(promotionDes(etudiants.get(0)));

        assertThat(tableau).hasSize(2);
        for (long etudiantId : etudiants) {
            JsonNode ligne = ligneDe(tableau, etudiantId);
            // Le contrat n'expose que ces six champs, ni email ni identifiants internes.
            assertThat(champsDe(ligne)).isEqualTo(CHAMPS_DU_CONTRAT);
            assertThat(ligne.get("presences").asLong()).isZero();
            assertThat(ligne.get("exercicesDeposes").asLong()).isZero();
            assertThat(ligne.get("relecturesEnAttente").asLong()).isZero();
            // Critère explicite : aucune note ⇒ `null`, jamais 0.
            assertThat(ligne.get("moyenne").isNull()).isTrue();
        }
    }

    @Test
    @DisplayName("EF9 : présences, dépôts, moyenne des notes reçues et relectures en attente sont exacts")
    void agrege_les_indicateurs_de_la_promotion() throws Exception {
        List<Long> etudiants = creerPromotion(2);
        long auteur = etudiants.get(0);
        long relecteur = etudiants.get(1);
        long promotionId = promotionDes(auteur);

        // Deux sessions pour l'auteur : chaque dépôt confie une relecture au second
        // étudiant, seul autre présent (RG5/RG13), qui note ensuite.
        long premiereRelecture = deposerEtRelecteur(promotionId, auteur, relecteur);
        long secondeRelecture = deposerEtRelecteur(promotionId, auteur, relecteur);

        JsonNode tableau = tableauDe(promotionId);
        assertThat(ligneDe(tableau, auteur).get("presences").asLong()).isEqualTo(2);
        assertThat(ligneDe(tableau, auteur).get("exercicesDeposes").asLong()).isEqualTo(2);
        assertThat(ligneDe(tableau, auteur).get("relecturesEnAttente").asLong()).isEqualTo(2);
        assertThat(ligneDe(tableau, auteur).get("moyenne").isNull()).isTrue();
        // Le relecteur est présent, mais n'a rien déposé.
        assertThat(ligneDe(tableau, relecteur).get("presences").asLong()).isEqualTo(2);
        assertThat(ligneDe(tableau, relecteur).get("exercicesDeposes").asLong()).isZero();
        assertThat(ligneDe(tableau, relecteur).get("relecturesEnAttente").asLong()).isZero();

        // Une seule note rendue : elle compte seule, l'autre relecture reste en attente.
        rendre(premiereRelecture, 10);
        tableau = tableauDe(promotionId);
        assertThat(ligneDe(tableau, auteur).get("moyenne").asDouble()).isEqualTo(10.0);
        assertThat(ligneDe(tableau, auteur).get("relecturesEnAttente").asLong()).isEqualTo(1);

        // Les deux notes rendues ⇒ moyenne des deux, plus rien en attente.
        rendre(secondeRelecture, 20);
        tableau = tableauDe(promotionId);
        assertThat(ligneDe(tableau, auteur).get("moyenne").asDouble()).isEqualTo(15.0);
        assertThat(ligneDe(tableau, auteur).get("relecturesEnAttente").asLong()).isZero();
    }

    @Test
    @DisplayName("EF9 : le nombre de requêtes SQL ne dépend pas de l'effectif (pas de N+1)")
    void aucun_n_plus_un() throws Exception {
        long petite = promotionDes(creerPromotion(1).get(0));
        long grande = promotionDes(creerPromotion(12).get(0));

        Statistics statistiques = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistiques.clear();
        tableauDe(petite);
        long requetesPetite = statistiques.getPrepareStatementCount();

        statistiques.clear();
        tableauDe(grande);
        long requetesGrande = statistiques.getPrepareStatementCount();

        // Douze fois plus d'étudiants, exactement autant de requêtes : la propriété
        // qui rend ENF3 tenable quelle que soit la promotion.
        assertThat(requetesGrande).isEqualTo(requetesPetite);
        // Six requêtes en tout : existence de la promotion, identités des
        // étudiants, puis les quatre agrégations. Un nombre qui ne bouge pas
        // avec l'effectif — c'est précisément ce que « pas de N+1 » signifie.
        assertThat(requetesPetite).isEqualTo(6L);
    }

    // ------------------------------------------------------------------

    private JsonNode tableauDe(long promotionId) throws Exception {
        String corps = mockMvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promotionId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps);
    }

    /** La ligne du tableau portant cet étudiant, ou un échec explicite s'il n'y figure pas. */
    private JsonNode ligneDe(JsonNode tableau, long etudiantId) {
        for (JsonNode ligne : tableau) {
            if (ligne.get("etudiantId").asLong() == etudiantId) {
                return ligne;
            }
        }
        throw new AssertionError("L'étudiant %d devrait figurer dans le tableau".formatted(etudiantId));
    }

    private Set<String> champsDe(JsonNode ligne) {
        Set<String> champs = new HashSet<>();
        ligne.fieldNames().forEachRemaining(champs::add);
        return champs;
    }

    /**
     * Ouvre une session pour la promotion, fait marquer les deux étudiants, dépose
     * l'exercice de l'auteur et renvoie l'identifiant de la relecture confiée au
     * relecteur (seul autre présent ⇒ tirage déterministe).
     */
    private long deposerEtRelecteur(long promotionId, long auteur, long relecteur) throws Exception {
        String code = ouvrirSession(promotionId);
        long sessionId = idDeLaSession(code);
        marquerPresence(code, auteur);
        marquerPresence(code, relecteur);
        long exerciceId = deposer(sessionId, auteur);

        Long relectureId = jdbcTemplate.queryForObject(
                "SELECT id FROM relecture WHERE exercice_id = ?", Long.class, exerciceId);
        if (relectureId == null) {
            throw new AssertionError("L'exercice %d aurait dû recevoir un relecteur".formatted(exerciceId));
        }
        return relectureId;
    }

    private void rendre(long relectureId, int note) throws Exception {
        mockMvc.perform(post("/api/relectures/{id}", relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("note", note, "commentaire", "Rendu par le relecteur EF9."))))
                .andExpect(status().isOk());
    }

    private String ouvrirSession(long promotionId) throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("titre", "Cours EF9", "promotionId", promotionId))))
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

    /**
     * Promotion neuve, créée directement en base : ni la seed de démonstration ni
     * les promotions des autres classes de test ne peuvent fausser les comptages.
     */
    private List<Long> creerPromotion(int nbEtudiants) {
        String nom = "Promotion EF9 %d-%d".formatted(COMPTEUR.incrementAndGet(), System.nanoTime());
        jdbcTemplate.update("INSERT INTO promotion (nom, annee) VALUES (?, ?)", nom, 2026);
        long promotionId = jdbcTemplate.queryForObject(
                "SELECT id FROM promotion WHERE nom = ?", Long.class, nom);

        List<Long> etudiantIds = new ArrayList<>();
        for (int rang = 0; rang < nbEtudiants; rang++) {
            String email = "ef9-%d-%d@exemple.org".formatted(promotionId, rang);
            jdbcTemplate.update(
                    "INSERT INTO etudiant (prenom, nom, email, promotion_id) VALUES (?, ?, ?, ?)",
                    "Prenom", "Nom%02d".formatted(rang), email, promotionId);
            etudiantIds.add(jdbcTemplate.queryForObject(
                    "SELECT id FROM etudiant WHERE email = ?", Long.class, email));
        }
        return etudiantIds;
    }

    private long promotionDes(long etudiantId) {
        Long promotionId = jdbcTemplate.queryForObject(
                "SELECT promotion_id FROM etudiant WHERE id = ?", Long.class, etudiantId);
        return promotionId == null ? -1L : promotionId;
    }
}
