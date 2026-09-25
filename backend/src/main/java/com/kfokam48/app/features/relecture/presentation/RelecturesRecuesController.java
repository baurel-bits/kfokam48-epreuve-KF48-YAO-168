package com.kfokam48.app.features.relecture.presentation;

import com.kfokam48.app.features.relecture.application.dto.NoteRecueReponse;
import com.kfokam48.app.features.relecture.application.service.RelectureService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/etudiants/{etudiantId}/relectures-recues} — les notes reçues
 * par un étudiant relu (EF8).
 *
 * <p>Le contrat ne déclare que {@code 200} sur cette opération : un étudiant sans
 * relecture reçoit une liste vide, ce qui n'est pas une erreur. Aucune réponse ne
 * porte l'identité du relecteur (RG6).
 */
@RestController
@RequestMapping("/api/etudiants/{etudiantId}/relectures-recues")
public class RelecturesRecuesController {

    private final RelectureService relectureService;

    public RelecturesRecuesController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @GetMapping
    public List<NoteRecueReponse> listerNotesRecues(@PathVariable Long etudiantId) {
        return relectureService.listerRelecturesRecues(etudiantId);
    }
}
