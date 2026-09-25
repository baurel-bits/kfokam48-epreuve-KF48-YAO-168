package com.kfokam48.app.features.relecture.presentation;

import com.kfokam48.app.features.relecture.application.dto.MissionRelecteurReponse;
import com.kfokam48.app.features.relecture.application.service.RelectureService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les deux lectures du relecteur : la mission d'un exercice donné (EF5) et
 * l'ensemble de ses missions en attente (EF6). Aucune ne divulgue l'identité de
 * l'auteur (RG6).
 */
@RestController
public class MissionRelecteurController {

    private final RelectureService relectureService;

    public MissionRelecteurController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    /**
     * {@code GET /api/exercices/{exerciceId}/relecteur?relecteurId=} — opération
     * imposée du contrat. Codes : {@code 200}, {@code 403 APPELANT_NON_AUTORISE}
     * (RG6), {@code 404 RELECTURE_INCONNUE}.
     */
    @GetMapping("/api/exercices/{exerciceId}/relecteur")
    public MissionRelecteurReponse consulterMission(@PathVariable Long exerciceId,
                                                    @RequestParam Long relecteurId) {
        return relectureService.consulterMission(exerciceId, relecteurId);
    }

    /**
     * {@code GET /api/relecteurs/{etudiantId}/relectures-en-attente} — les
     * relectures assignées et non encore rendues.
     *
     * <p>Un identifiant inconnu renvoie une liste vide : le contrat ne déclare que
     * {@code 200} sur cette opération, et un relecteur sans mission n'est pas une
     * erreur.
     */
    @GetMapping("/api/relecteurs/{etudiantId}/relectures-en-attente")
    public List<MissionRelecteurReponse> listerMissionsEnAttente(@PathVariable Long etudiantId) {
        return relectureService.listerMissionsEnAttente(etudiantId);
    }
}
