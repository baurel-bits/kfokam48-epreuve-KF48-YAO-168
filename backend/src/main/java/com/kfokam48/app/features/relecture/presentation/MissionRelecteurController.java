package com.kfokam48.app.features.relecture.presentation;

import com.kfokam48.app.features.relecture.application.dto.MissionRelecteurReponse;
import com.kfokam48.app.features.relecture.application.service.RelectureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/exercices/{exerciceId}/relecteur?relecteurId=} — opération
 * imposée du contrat (EF5/EF6).
 *
 * <p>Codes de statut possibles : {@code 200}, {@code 403 APPELANT_NON_AUTORISE}
 * (RG6), {@code 404 RELECTURE_INCONNUE}.
 */
@RestController
@RequestMapping("/api/exercices/{exerciceId}/relecteur")
public class MissionRelecteurController {

    private final RelectureService relectureService;

    public MissionRelecteurController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @GetMapping
    public MissionRelecteurReponse consulterMission(@PathVariable Long exerciceId,
                                                    @RequestParam Long relecteurId) {
        return relectureService.consulterMission(exerciceId, relecteurId);
    }
}
