package com.kfokam48.app.features.presence.presentation;

import com.kfokam48.app.features.presence.application.dto.AjoutPresenceManuelleRequete;
import com.kfokam48.app.features.presence.application.dto.MarquagePresenceRequete;
import com.kfokam48.app.features.presence.application.dto.PresenceReponse;
import com.kfokam48.app.features.presence.application.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/presences} — opération imposée du contrat (EF2) — et
 * {@code POST /api/presences/manuelles} (EF10).
 *
 * <p>EF2 : {@code 201}, {@code 400 CODE_INCONNU}, {@code 409 DEJA_PRESENT},
 * {@code 410 CODE_EXPIRE}, {@code 429 TROP_DE_TENTATIVES}.
 * EF10 : {@code 201}, {@code 404 SESSION_INCONNUE} / {@code ETUDIANT_INCONNU},
 * {@code 409 DEJA_PRESENT}.
 */
@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceReponse marquer(@Valid @RequestBody MarquagePresenceRequete requete) {
        return presenceService.marquerPresence(requete);
    }

    /**
     * {@code POST /api/presences/manuelles} — EF10. Le chemin est au <strong>pluriel</strong>
     * ({@code /manuelles}) et non au singulier, comme le déclare le contrat ; la
     * réponse est {@code 201} : la présence est créée, même si le formateur la saisit
     * après coup.
     */
    @PostMapping("/manuelles")
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceReponse ajouterManuellement(@Valid @RequestBody AjoutPresenceManuelleRequete requete) {
        return presenceService.ajouterPresenceManuelle(requete);
    }
}
