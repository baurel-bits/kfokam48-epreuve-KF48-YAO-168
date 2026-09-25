package com.kfokam48.app.features.session.presentation;

import com.kfokam48.app.features.session.application.dto.CreationSessionRequete;
import com.kfokam48.app.features.session.application.dto.SessionOuverteReponse;
import com.kfokam48.app.features.session.application.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/sessions} — opération imposée du contrat (EF1).
 * Le contrôleur ne fait que valider l'entrée et déléguer au service (B3).
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionOuverteReponse ouvrir(@Valid @RequestBody CreationSessionRequete requete) {
        return sessionService.ouvrir(requete);
    }
}
