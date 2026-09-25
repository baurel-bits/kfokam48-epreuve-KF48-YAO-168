package com.kfokam48.app.features.exercice.presentation;

import com.kfokam48.app.features.exercice.application.dto.DepotExerciceRequete;
import com.kfokam48.app.features.exercice.application.dto.ExerciceDeposeReponse;
import com.kfokam48.app.features.exercice.application.service.ExerciceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/exercices} — opération imposée du contrat (EF3).
 * Codes de statut possibles : {@code 201}, {@code 400 LIEN_INVALIDE},
 * {@code 409 EXERCICE_DEJA_DEPOSE} / {@code 409 SESSION_CLOTUREE}.
 */
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciceDeposeReponse deposer(@Valid @RequestBody DepotExerciceRequete requete) {
        return exerciceService.deposer(requete);
    }
}
