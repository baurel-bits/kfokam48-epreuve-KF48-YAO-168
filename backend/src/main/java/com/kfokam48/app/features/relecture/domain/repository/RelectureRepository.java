package com.kfokam48.app.features.relecture.domain.repository;

import com.kfokam48.app.features.relecture.domain.entity.Relecture;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /** {@code uk_relecture_exercice} : au plus une relecture par exercice (RG5). */
    Optional<Relecture> findByExerciceId(Long exerciceId);
}
