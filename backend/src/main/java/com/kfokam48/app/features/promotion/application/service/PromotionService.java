package com.kfokam48.app.features.promotion.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.promotion.application.dto.EtudiantResume;
import com.kfokam48.app.features.promotion.domain.repository.EtudiantRepository;
import com.kfokam48.app.features.promotion.domain.repository.PromotionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liste des étudiants d'une promotion — prérequis Q1 (l'étudiant est choisi dans
 * une liste, le sujet excluant l'authentification). Cet endpoint alimente les
 * écrans étudiant (issue #9) et formateur (issues #14, #18).
 */
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;

    public PromotionService(PromotionRepository promotionRepository, EtudiantRepository etudiantRepository) {
        this.promotionRepository = promotionRepository;
        this.etudiantRepository = etudiantRepository;
    }

    /**
     * @throws ExceptionMetier {@code 404 PROMOTION_INCONNUE} si la promotion n'existe pas.
     */
    @Transactional(readOnly = true)
    public List<EtudiantResume> listerEtudiants(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new ExceptionMetier(CodeErreur.PROMOTION_INCONNUE, HttpStatus.NOT_FOUND,
                    "La promotion %d est inconnue.".formatted(promotionId));
        }

        return etudiantRepository.findByPromotionIdOrderByNomAscPrenomAsc(promotionId).stream()
                .map(etudiant -> new EtudiantResume(etudiant.getId(), etudiant.getPrenom(), etudiant.getNom()))
                .toList();
    }
}
