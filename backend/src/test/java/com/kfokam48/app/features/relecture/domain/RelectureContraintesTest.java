package com.kfokam48.app.features.relecture.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * EF5 — vérifie que RG4 et RG5 sont réellement garanties par le schéma (V1) et
 * pas seulement par le service : le diagramme D2 s'appuie sur
 * {@code uk_relecture_exercice} et {@code ck_relecture_pas_auto_relecture}.
 *
 * <p>Ces tests écrivent en SQL direct, sans passer par le service, précisément
 * pour prouver que la base refuse ce que le service écarte par ailleurs.
 */
@SpringBootTest
@ActiveProfiles("test")
class RelectureContraintesTest {

    /** Auteur de l'exercice : l'étudiant 1, présent dans le jeu de démonstration. */
    private static final long AUTEUR_ID = 1L;
    private static final long AUTRE_ETUDIANT_ID = 3L;

    /** Les codes de session sont uniques : un compteur suffit à isoler chaque test. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long exerciceId;

    @BeforeEach
    void creerUnExerciceDepose() {
        LocalDateTime maintenant = LocalDateTime.now();
        String code = "CTR%03d".formatted(COMPTEUR.incrementAndGet());

        jdbcTemplate.update("""
                INSERT INTO session (titre, code, promotion_id, ouverture_at, expiration_at, cloturee)
                VALUES ('Cours contraintes RG4/RG5', ?, 1, ?, ?, FALSE)
                """, code, maintenant, maintenant.plusMinutes(15));

        Long sessionId = jdbcTemplate.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);

        jdbcTemplate.update("""
                INSERT INTO exercice (session_id, etudiant_id, lien, statut, depot_at)
                VALUES (?, ?, 'https://exemple.org/contraintes.pdf', 'DEPOSE', ?)
                """, sessionId, AUTEUR_ID, maintenant);

        exerciceId = jdbcTemplate.queryForObject(
                "SELECT id FROM exercice WHERE session_id = ?", Long.class, sessionId);
    }

    @Test
    @DisplayName("RG5 : la base refuse un second relecteur pour le même exercice")
    void un_seul_relecteur_par_exercice() {
        insererRelecture(exerciceId, 2L, AUTEUR_ID);

        assertThatThrownBy(() -> insererRelecture(exerciceId, AUTRE_ETUDIANT_ID, AUTEUR_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("RG4 : la base refuse que l'auteur relise son propre exercice")
    void auteur_ne_peut_pas_etre_son_relecteur() {
        assertThatThrownBy(() -> insererRelecture(exerciceId, AUTEUR_ID, AUTEUR_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("RG7 : la base refuse une note hors 0-20, second filet après la validation (EF6)")
    void note_hors_bornes_refusee_par_la_base() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO relecture (exercice_id, relecteur_id, auteur_id, note, commentaire, statut)
                VALUES (?, 2, ?, 21, 'note hors bornes', 'RENDUE')
                """, exerciceId, AUTEUR_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insererRelecture(Long exerciceId, Long relecteurId, Long auteurId) {
        jdbcTemplate.update("""
                INSERT INTO relecture (exercice_id, relecteur_id, auteur_id, statut)
                VALUES (?, ?, ?, 'EN_ATTENTE')
                """, exerciceId, relecteurId, auteurId);
    }
}
