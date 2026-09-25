package com.kfokam48.app.features.relecture.domain.repository;

import com.kfokam48.app.features.relecture.domain.entity.CorrectionRelecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionRelectureRepository extends JpaRepository<CorrectionRelecture, Long> {

    /**
     * Historique des corrections d'une relecture, de la plus ancienne à la plus
     * récente (RG8). L'ordre suit l'identifiant : deux corrections successives
     * dans la même milliseconde ne doivent pas s'inverser.
     */
    List<CorrectionRelecture> findByRelectureIdOrderByIdAsc(Long relectureId);
}
