package com.kfokam48.app.features.tableau.presentation;

import com.kfokam48.app.features.tableau.application.dto.LigneTableauReponse;
import com.kfokam48.app.features.tableau.application.service.TableauService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/tableau} — opération <strong>imposée</strong> du contrat (EF9).
 * Le contrôleur ne fait que recevoir le paramètre et déléguer au service (B3).
 *
 * <p>{@code promotionId} est déclaré obligatoire par le contrat : sans lui, Spring
 * MVC refuse la requête, et le gestionnaire central la reformate en
 * {@code 400 DEMANDE_INVALIDE} au format imposé (B4).
 */
@RestController
@RequestMapping("/api/tableau")
public class TableauController {

    private final TableauService tableauService;

    public TableauController(TableauService tableauService) {
        this.tableauService = tableauService;
    }

    @GetMapping
    public List<LigneTableauReponse> consulterLeTableau(@RequestParam Long promotionId) {
        return tableauService.tableauDeLaPromotion(promotionId);
    }
}
