package com.kfokam48.app.features.presence.domain.repository;

import com.kfokam48.app.features.presence.domain.entity.TentativeSaisie;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativeSaisieRepository extends JpaRepository<TentativeSaisie, Long> {

    /** Compteur RG3, cloisonné par couple (étudiant, session). */
    Optional<TentativeSaisie> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /**
     * Compteur RG3 des codes non attribuables à une session (code inconnu).
     * La recherche utilise {@code IS NULL} : une comparaison {@code = NULL} ne
     * renverrait jamais la ligne et le compteur ne pourrait pas s'incrémenter.
     */
    Optional<TentativeSaisie> findBySessionIdIsNullAndEtudiantId(Long etudiantId);
}
