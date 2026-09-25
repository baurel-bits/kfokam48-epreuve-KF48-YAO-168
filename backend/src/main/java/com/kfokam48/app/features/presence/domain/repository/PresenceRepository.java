package com.kfokam48.app.features.presence.domain.repository;

import com.kfokam48.app.features.presence.domain.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /** Matérialise la règle {@code 409 DEJA_PRESENT} (uk_presence_session_etudiant). */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
