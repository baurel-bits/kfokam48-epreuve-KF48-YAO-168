package com.kfokam48.app.features.promotion.domain.repository;

import com.kfokam48.app.features.promotion.domain.entity.Etudiant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    /** Liste stable et lisible pour le sélecteur d'étudiant (prérequis Q1). */
    List<Etudiant> findByPromotionIdOrderByNomAscPrenomAsc(Long promotionId);
}
