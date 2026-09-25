package com.kfokam48.app.features.presence.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.presence.application.dto.MarquagePresenceRequete;
import com.kfokam48.app.features.presence.application.dto.PresenceReponse;
import com.kfokam48.app.features.presence.domain.entity.Presence;
import com.kfokam48.app.features.presence.domain.entity.SourcePresence;
import com.kfokam48.app.features.presence.domain.entity.TentativeSaisie;
import com.kfokam48.app.features.presence.domain.repository.PresenceRepository;
import com.kfokam48.app.features.presence.domain.repository.TentativeSaisieRepository;
import com.kfokam48.app.features.promotion.domain.repository.EtudiantRepository;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marquage d'une présence par un étudiant (EF2 ; RG1, RG2, RG3).
 *
 * <p>Ordre des contrôles, conforme au diagramme D3 :
 * <ol>
 *   <li>recherche de la session par son code → {@code 400 CODE_INCONNU} ;</li>
 *   <li>blocage RG3 déjà actif pour ce couple (étudiant, session) → {@code 429} ;</li>
 *   <li>code expiré (RG1) → {@code 410} et incrément du compteur d'échecs (RG3) ;</li>
 *   <li>présence déjà enregistrée → {@code 409} ;</li>
 *   <li>sinon enregistrement de la présence avec {@code source = ETUDIANT}.</li>
 * </ol>
 *
 * <p>{@code noRollbackFor} est indispensable : le compteur d'échecs doit rester
 * enregistré alors même que la saisie est refusée par une exception métier.
 */
@Service
public class PresenceService {

    private final SessionRepository sessionRepository;
    private final PresenceRepository presenceRepository;
    private final TentativeSaisieRepository tentativeSaisieRepository;
    private final EtudiantRepository etudiantRepository;
    private final int maxEchecs;
    private final int blocageMinutes;

    public PresenceService(SessionRepository sessionRepository,
                           PresenceRepository presenceRepository,
                           TentativeSaisieRepository tentativeSaisieRepository,
                           EtudiantRepository etudiantRepository,
                           @Value("${app.presence.max-echecs}") int maxEchecs,
                           @Value("${app.presence.blocage-minutes}") int blocageMinutes) {
        this.sessionRepository = sessionRepository;
        this.presenceRepository = presenceRepository;
        this.tentativeSaisieRepository = tentativeSaisieRepository;
        this.etudiantRepository = etudiantRepository;
        this.maxEchecs = maxEchecs;
        this.blocageMinutes = blocageMinutes;
    }

    @Transactional(noRollbackFor = ExceptionMetier.class)
    public PresenceReponse marquerPresence(MarquagePresenceRequete requete) {
        String code = requete.code().trim().toUpperCase(Locale.ROOT);
        Long etudiantId = requete.etudiantId();

        // Le contrat ne déclare que 400/409/410/429 sur cette opération : un étudiant
        // inconnu est donc refusé en 400, sans jamais atteindre la clé étrangère.
        if (!etudiantRepository.existsById(etudiantId)) {
            throw new ExceptionMetier(CodeErreur.ETUDIANT_INCONNU, HttpStatus.BAD_REQUEST,
                    "L'étudiant %d est inconnu.".formatted(etudiantId));
        }

        Session session = sessionRepository.findByCode(code)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.CODE_INCONNU, HttpStatus.BAD_REQUEST,
                        "Aucune session ne correspond à ce code de présence."));

        LocalDateTime maintenant = LocalDateTime.now();
        TentativeSaisie tentative = tentativeSaisieRepository
                .findBySessionIdAndEtudiantId(session.getId(), etudiantId)
                .orElseGet(() -> new TentativeSaisie(session.getId(), etudiantId));
        reinitialiserSiBlocageTermine(tentative, maintenant);

        // RG3 : le blocage est évalué avant toute validation du code.
        if (blocageActif(tentative, maintenant)) {
            throw new ExceptionMetier(CodeErreur.TROP_DE_TENTATIVES, HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives : réessayez dans %d minute(s).".formatted(blocageMinutes));
        }

        // RG1 / RG2 : un code expiré est refusé et compte comme un échec de saisie (RG3).
        if (!session.getExpirationAt().isAfter(maintenant)) {
            enregistrerEchec(tentative, maintenant);
            throw new ExceptionMetier(CodeErreur.CODE_EXPIRE, HttpStatus.GONE,
                    "Le code de présence a expiré.");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw new ExceptionMetier(CodeErreur.DEJA_PRESENT, HttpStatus.CONFLICT,
                    "Cet étudiant a déjà marqué sa présence pour cette session.");
        }

        Presence presence = presenceRepository.save(
                new Presence(session.getId(), etudiantId, SourcePresence.ETUDIANT, maintenant));

        return new PresenceReponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(),
                presence.getSource());
    }

    private boolean blocageActif(TentativeSaisie tentative, LocalDateTime maintenant) {
        return tentative.getBloqueJusqua() != null && tentative.getBloqueJusqua().isAfter(maintenant);
    }

    /**
     * Un blocage de 2 minutes doit rendre au couple (étudiant, session) un nouveau
     * crédit de {@code maxEchecs} tentatives : sans remise à zéro, le compteur déjà
     * à 5 le bloquerait définitivement et les « 2 minutes » n'auraient aucun sens.
     */
    private void reinitialiserSiBlocageTermine(TentativeSaisie tentative, LocalDateTime maintenant) {
        if (tentative.getBloqueJusqua() != null && !tentative.getBloqueJusqua().isAfter(maintenant)) {
            tentative.setEchecs(0);
            tentative.setBloqueJusqua(null);
        }
    }

    private void enregistrerEchec(TentativeSaisie tentative, LocalDateTime maintenant) {
        tentative.setEchecs(tentative.getEchecs() + 1);
        if (tentative.getEchecs() >= maxEchecs) {
            tentative.setBloqueJusqua(maintenant.plusMinutes(blocageMinutes));
        }
        tentativeSaisieRepository.save(tentative);
    }
}
