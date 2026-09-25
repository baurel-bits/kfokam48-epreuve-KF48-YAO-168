package com.kfokam48.app.features.exercice.domain.repository;

import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    /** Matérialise la règle {@code 409 EXERCICE_DEJA_DEPOSE} (uk_exercice_session_etudiant). */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
