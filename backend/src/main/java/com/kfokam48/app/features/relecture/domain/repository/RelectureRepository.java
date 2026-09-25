package com.kfokam48.app.features.relecture.domain.repository;

import com.kfokam48.app.features.relecture.domain.entity.Relecture;
import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /** {@code uk_relecture_exercice} : au plus une relecture par exercice (RG5). */
    Optional<Relecture> findByExerciceId(Long exerciceId);

    /**
     * Missions d'un relecteur dans un état donné (EF6), dans l'ordre
     * d'assignation : l'API renvoie ainsi une liste stable plutôt que l'ordre
     * décidé par le plan d'exécution de la base.
     */
    List<Relecture> findByRelecteurIdAndStatutOrderByIdAsc(Long relecteurId, StatutRelecture statut);

    /**
     * Relectures portant sur les exercices d'un étudiant (EF8), les plus récentes
     * d'abord. {@code auteurId} est dénormalisé depuis l'exercice et maintenu par
     * la clé étrangère composite : la lecture est donc exacte sans jointure.
     */
    List<Relecture> findByAuteurIdOrderByIdDesc(Long auteurId);
}
