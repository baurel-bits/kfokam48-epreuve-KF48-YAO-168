package com.kfokam48.app.features.presence.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.presence.application.dto.AjoutPresenceManuelleRequete;
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
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marquage d'une présence par un étudiant (EF2 ; RG1, RG2, RG3).
 *
 * <p>Ordre des contrôles, conforme au diagramme D3 :
 * <ol>
 *   <li>blocage RG3 actif pour cet étudiant (codes non attribuables) → {@code 429} ;</li>
 *   <li>recherche de la session par son code → {@code 400 CODE_INCONNU} ;</li>
 *   <li>blocage RG3 actif pour ce couple (étudiant, session) → {@code 429} ;</li>
 *   <li>code expiré (RG1) → {@code 410} et incrément du compteur d'échecs (RG3) ;</li>
 *   <li>présence déjà enregistrée → {@code 409} ;</li>
 *   <li>sinon enregistrement de la présence avec {@code source = ETUDIANT}.</li>
 * </ol>
 *
 * <p>RG3 a deux portées, parce que le contrat n'envoie que {@code { code, etudiantId }} :
 * <ul>
 *   <li><strong>par couple (étudiant, session)</strong> pour un code expiré, donc
 *       attribuable à une session (décision de la section 7 du cahier des charges) ;</li>
 *   <li><strong>par étudiant</strong> pour un code inconnu, qui n'est rattachable à
 *       aucune session : sans cette seconde portée, cinq codes erronés — l'échec le
 *       plus courant — ne déclenchaient jamais le blocage de 2 minutes.</li>
 * </ul>
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

        LocalDateTime maintenant = LocalDateTime.now();

        // RG3 (portée étudiant) : compteur des codes que l'on ne peut rattacher à
        // aucune session. Contrôlé avant la lecture du code, conformément à D3.
        TentativeSaisie tentativeSansSession = tentativeSaisieRepository
                .findBySessionIdIsNullAndEtudiantId(etudiantId)
                .orElseGet(() -> TentativeSaisie.sansSession(etudiantId));
        reinitialiserSiBlocageTermine(tentativeSansSession, maintenant);
        verifierBlocage(tentativeSansSession, maintenant);

        Optional<Session> sessionTrouvee = sessionRepository.findByCode(code);
        if (sessionTrouvee.isEmpty()) {
            enregistrerEchec(tentativeSansSession, maintenant);
            throw new ExceptionMetier(CodeErreur.CODE_INCONNU, HttpStatus.BAD_REQUEST,
                    "Aucune session ne correspond à ce code de présence.");
        }

        Session session = sessionTrouvee.get();

        // RG3 (portée couple) : le compteur est cloisonné par session, donc être
        // bloqué sur une session n'empêche pas de saisir un code pour une autre.
        TentativeSaisie tentativeDuCouple = tentativeSaisieRepository
                .findBySessionIdAndEtudiantId(session.getId(), etudiantId)
                .orElseGet(() -> new TentativeSaisie(session.getId(), etudiantId));
        reinitialiserSiBlocageTermine(tentativeDuCouple, maintenant);
        verifierBlocage(tentativeDuCouple, maintenant);

        // RG1 / RG2 : un code expiré est refusé et compte comme un échec de saisie (RG3).
        if (!session.getExpirationAt().isAfter(maintenant)) {
            enregistrerEchec(tentativeDuCouple, maintenant);
            throw new ExceptionMetier(CodeErreur.CODE_EXPIRE, HttpStatus.GONE,
                    "Le code de présence a expiré.");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw new ExceptionMetier(CodeErreur.DEJA_PRESENT, HttpStatus.CONFLICT,
                    "Cet étudiant a déjà marqué sa présence pour cette session.");
        }

        try {
            Presence presence = presenceRepository.save(
                    new Presence(session.getId(), etudiantId, SourcePresence.ETUDIANT, maintenant));

            return new PresenceReponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(),
                    presence.getSource());
        } catch (DataIntegrityViolationException echec) {
            // Deux envois simultanés du même étudiant : le contrôle ci-dessus les a
            // laissés passer tous les deux, et c'est uk_presence_session_etudiant qui
            // tranche. Sans cette reprise, la course produirait un 500 au lieu du 409
            // du contrat. Même reprise que l'EF10 (ajouterPresenceManuelle).
            throw new ExceptionMetier(CodeErreur.DEJA_PRESENT, HttpStatus.CONFLICT,
                    "Cet étudiant a déjà marqué sa présence pour cette session.");
        }
    }

    /**
     * Ajout manuel d'une présence par le formateur (EF10, RG12).
     *
     * <p>Ordre des contrôles : session, étudiant, doublon.
     *
     * <p>Cette opération <strong>ne consulte ni le code de présence, ni son
     * expiration</strong> : elle fonctionne sans code, et après les 15 minutes de
     * RG1. Elle n'est pas davantage gelée par la clôture, RG14 ne portant que sur
     * les dépôts et les notations — le contrat le formule ainsi (« gèle tout dépôt
     * et toute notation ») et ne mentionne pas la présence.
     *
     * <p>{@code source = FORMATEUR} est décidée ici, jamais reçue du client : c'est
     * ce qui rend la présence manuelle distinguable (RG12).
     *
     * @throws ExceptionMetier {@code 404 SESSION_INCONNUE}, {@code 404 ETUDIANT_INCONNU}
     *         ou {@code 409 DEJA_PRESENT}, conformément au contrat.
     */
    @Transactional
    public PresenceReponse ajouterPresenceManuelle(AjoutPresenceManuelleRequete requete) {
        Session session = sessionRepository.findById(requete.sessionId())
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.NOT_FOUND,
                        "La session %d est inconnue.".formatted(requete.sessionId())));

        // 404 et non 400 comme sur l'EF2 : le contrat déclare ETUDIANT_INCONNU en 404
        // sur cette opération-là, et 400 sur l'opération imposée. Même règle, deux
        // statuts différents selon le guichet.
        if (!etudiantRepository.existsById(requete.etudiantId())) {
            throw new ExceptionMetier(CodeErreur.ETUDIANT_INCONNU, HttpStatus.NOT_FOUND,
                    "L'étudiant %d est inconnu.".formatted(requete.etudiantId()));
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), requete.etudiantId())) {
            throw new ExceptionMetier(CodeErreur.DEJA_PRESENT, HttpStatus.CONFLICT,
                    "Cet étudiant a déjà une présence pour cette session.");
        }

        try {
            // Le flush est indispensable : sans lui, la violation de
            // uk_presence_session_etudiant ne surviendrait qu'au commit, donc hors du
            // bloc où on la rattrape.
            Presence presence = presenceRepository.saveAndFlush(new Presence(session.getId(),
                    requete.etudiantId(), SourcePresence.FORMATEUR, LocalDateTime.now()));
            return new PresenceReponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(),
                    presence.getSource());
        } catch (DataIntegrityViolationException echec) {
            // Deux clics simultanés du formateur : le contrôle ci-dessus les a laissés
            // passer tous les deux, et c'est la contrainte d'unicité qui tranche. Sans
            // cette reprise, la course produirait un 500 au lieu du 409 du contrat.
            throw new ExceptionMetier(CodeErreur.DEJA_PRESENT, HttpStatus.CONFLICT,
                    "Cet étudiant a déjà une présence pour cette session.");
        }
    }

    private void verifierBlocage(TentativeSaisie tentative, LocalDateTime maintenant) {
        if (tentative.getBloqueJusqua() != null && tentative.getBloqueJusqua().isAfter(maintenant)) {
            throw new ExceptionMetier(CodeErreur.TROP_DE_TENTATIVES, HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives : réessayez dans %d minute(s).".formatted(blocageMinutes));
        }
    }

    /**
     * Un blocage de 2 minutes doit rendre un nouveau crédit de {@code maxEchecs}
     * tentatives : sans remise à zéro, le compteur déjà à 5 bloquerait définitivement
     * et les « 2 minutes » n'auraient aucun sens.
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
