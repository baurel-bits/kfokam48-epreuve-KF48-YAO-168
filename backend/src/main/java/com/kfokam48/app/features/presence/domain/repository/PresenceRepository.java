package com.kfokam48.app.features.presence.domain.repository;

import com.kfokam48.app.features.presence.domain.entity.Presence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /** Matérialise la règle {@code 409 DEJA_PRESENT} (uk_presence_session_etudiant). */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /**
     * Présences d'une session, lues <strong>à l'instant</strong> de l'appel.
     * C'est ce qui permet à RG13 de recalculer le pool des relecteurs au moment de
     * l'assignation, présences manuelles (EF10) comme automatiques (EF2).
     */
    List<Presence> findBySessionId(Long sessionId);
}
