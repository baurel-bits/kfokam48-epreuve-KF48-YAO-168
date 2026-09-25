package com.kfokam48.app.features.presence.domain.repository;

import com.kfokam48.app.features.presence.domain.entity.TentativeSaisie;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativeSaisieRepository extends JpaRepository<TentativeSaisie, Long> {

    /** Compteur RG3, cloisonné par couple (étudiant, session). */
    Optional<TentativeSaisie> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
