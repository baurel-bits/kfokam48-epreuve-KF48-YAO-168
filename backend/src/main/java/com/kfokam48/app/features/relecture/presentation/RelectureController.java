package com.kfokam48.app.features.relecture.presentation;

import com.kfokam48.app.features.relecture.application.dto.RelectureRendueReponse;
import com.kfokam48.app.features.relecture.application.dto.SoumissionRelectureRequete;
import com.kfokam48.app.features.relecture.application.service.RelectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/relectures/{id}} — opération <strong>imposée</strong> par le
 * sujet (EF6) : le relecteur rend sa note et son commentaire.
 *
 * <p>Codes de statut du contrat : {@code 200} rendue, {@code 400 NOTE_INVALIDE}
 * (RG7), {@code 403 AUTO_RELECTURE} (RG4), {@code 409 RELECTURE_DEJA_RENDUE}.
 * Un identifiant de relecture inconnu, cas non prévu par le contrat, renvoie
 * {@code 404 RELECTURE_INCONNUE} plutôt qu'une erreur interne.
 */
@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    /** Le contrat annonce {@code 200} et non {@code 201} : la relecture préexiste. */
    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public RelectureRendueReponse rendre(@PathVariable Long id,
                                         @Valid @RequestBody SoumissionRelectureRequete requete) {
        return relectureService.rendre(id, requete);
    }
}
