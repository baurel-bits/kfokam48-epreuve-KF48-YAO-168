package com.kfokam48.app.features.exercice.presentation;

import com.kfokam48.app.features.exercice.application.dto.DepotExerciceRequete;
import com.kfokam48.app.features.exercice.application.dto.ExerciceDeposeReponse;
import com.kfokam48.app.features.exercice.application.dto.RemplacementLienRequete;
import com.kfokam48.app.features.exercice.application.service.ExerciceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/exercices} — opération imposée du contrat (EF3), et
 * {@code PUT /api/exercices/{id}/lien} — EF4.
 *
 * <p>Codes de statut d'EF3 : {@code 201}, {@code 400 LIEN_INVALIDE},
 * {@code 409 EXERCICE_DEJA_DEPOSE} / {@code 409 SESSION_CLOTUREE}.
 * Codes de statut d'EF4 : {@code 200}, {@code 400 LIEN_INVALIDE},
 * {@code 404 EXERCICE_INCONNU}, {@code 409 RELECTURE_COMMENCEE} /
 * {@code SESSION_CLOTUREE}.
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

    /**
     * {@code PUT /api/exercices/{id}/lien} — EF4 : l'étudiant remplace le lien de
     * son exercice tant qu'aucune relecture n'a été commencée dessus (RG11) et que
     * la session n'est pas clôturée (RG14).
     *
     * <p>{@code PUT} sur un sous-chemin : la ressource remplacée — le lien de
     * l'exercice — est désignée par l'URL, et l'appel répété laisse le même état
     * final. Le contrat déclare {@code 200 {id, statut}} : la réponse est celle du
     * dépôt, le statut n'ayant pas changé.
     */
    @PutMapping("/{id}/lien")
    @ResponseStatus(HttpStatus.OK)
    public ExerciceDeposeReponse remplacerLien(@PathVariable Long id,
                                              @Valid @RequestBody RemplacementLienRequete requete) {
        return exerciceService.remplacerLien(id, requete);
    }
}
