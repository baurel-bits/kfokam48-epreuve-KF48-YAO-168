package com.kfokam48.app.features.session.domain.repository;

import com.kfokam48.app.features.session.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /** Vérifie l'unicité du code de présence (contrainte {@code uk_session_code}). */
    boolean existsByCode(String code);
}
