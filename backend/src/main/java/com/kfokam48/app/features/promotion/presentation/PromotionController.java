package com.kfokam48.app.features.promotion.presentation;

import com.kfokam48.app.features.promotion.application.dto.EtudiantResume;
import com.kfokam48.app.features.promotion.application.service.PromotionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/promotions/{promotionId}/etudiants} — prérequis Q1 du contrat :
 * l'étudiant est choisi dans une liste, aucun mot de passe n'est utilisé.
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/{promotionId}/etudiants")
    public List<EtudiantResume> listerEtudiants(@PathVariable Long promotionId) {
        return promotionService.listerEtudiants(promotionId);
    }
}
