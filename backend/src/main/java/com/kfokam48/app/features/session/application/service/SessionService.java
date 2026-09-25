package com.kfokam48.app.features.session.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.promotion.domain.repository.PromotionRepository;
import com.kfokam48.app.features.session.application.dto.CreationSessionRequete;
import com.kfokam48.app.features.session.application.dto.SessionClotureeReponse;
import com.kfokam48.app.features.session.application.dto.SessionOuverteReponse;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ouverture d'une session de présence (EF1, RG1).
 */
@Service
public class SessionService {

    /**
     * Alphabet sans caractères ambigus (I, O, 0, 1) : le code est lu puis saisi
     * à la main par les étudiants (ENF1, saisie mobile).
     */
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LONGUEUR_CODE = 6;
    private static final int TENTATIVES_CODE_MAX = 10;

    private final SessionRepository sessionRepository;
    private final PromotionRepository promotionRepository;
    private final int validiteCodeMinutes;
    private final SecureRandom aleatoire = new SecureRandom();

    public SessionService(SessionRepository sessionRepository,
                          PromotionRepository promotionRepository,
                          @Value("${app.session.code-validity-minutes}") int validiteCodeMinutes) {
        this.sessionRepository = sessionRepository;
        this.promotionRepository = promotionRepository;
        this.validiteCodeMinutes = validiteCodeMinutes;
    }

    /**
     * Ouvre une session pour une promotion existante et renvoie son code de présence.
     *
     * @throws ExceptionMetier {@code 400 PROMOTION_INCONNUE} si la promotion n'existe pas.
     */
    @Transactional
    public SessionOuverteReponse ouvrir(CreationSessionRequete requete) {
        if (!promotionRepository.existsById(requete.promotionId())) {
            throw new ExceptionMetier(CodeErreur.PROMOTION_INCONNUE, HttpStatus.BAD_REQUEST,
                    "La promotion %d est inconnue.".formatted(requete.promotionId()));
        }

        OffsetDateTime ouvertureAt = OffsetDateTime.now();
        // RG1 : le code expire 15 minutes après l'ouverture de la session.
        OffsetDateTime expirationAt = ouvertureAt.plusMinutes(validiteCodeMinutes);

        // La base stocke des TIMESTAMP sans fuseau (cf. V1 et D2) : on n'y conserve que
        // le mur d'horloge du serveur.
        Session session = new Session(requete.titre().trim(), genererCodeUnique(), requete.promotionId(),
                ouvertureAt.toLocalDateTime(), expirationAt.toLocalDateTime());
        Session enregistree = sessionRepository.save(session);

        // Le contrat impose `format: date-time` (RFC 3339) : le décalage horaire, absent
        // du stockage, est réattaché aux instants relus juste avant de les exposer.
        return new SessionOuverteReponse(enregistree.getId(), enregistree.getCode(),
                enregistree.getOuvertureAt().atOffset(ouvertureAt.getOffset()),
                enregistree.getExpirationAt().atOffset(expirationAt.getOffset()));
    }

    /**
     * Clôture une session (EF11, RG14) : les dépôts et les notes de cette session
     * sont ensuite refusés, sans que les données déjà enregistrées soient
     * touchées. Les relectures encore en attente le restent définitivement.
     *
     * <p>Le contrat ne déclare que {@code 200} et {@code 404} sur cette opération :
     * clôturer une session <strong>déjà</strong> clôturée répond donc {@code 200}
     * sans rien réécrire, plutôt qu'un {@code 409} qui n'y figure pas.
     *
     * @throws ExceptionMetier {@code 404 SESSION_INCONNUE} si la session n'existe pas.
     */
    @Transactional
    public SessionClotureeReponse cloturer(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ExceptionMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.NOT_FOUND,
                        "La session %d est inconnue.".formatted(sessionId)));

        if (!session.isCloturee()) {
            session.cloturer();
            sessionRepository.save(session);
        }

        return new SessionClotureeReponse(session.getId(), session.isCloturee());
    }

    /** Tire un code de présence, en régénérant tant qu'il est déjà attribué (uk_session_code). */
    private String genererCodeUnique() {
        for (int tentative = 0; tentative < TENTATIVES_CODE_MAX; tentative++) {
            String code = genererCode();
            if (!sessionRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new ExceptionMetier(CodeErreur.ERREUR_INTERNE, HttpStatus.INTERNAL_SERVER_ERROR,
                "Impossible de générer un code de présence unique.");
    }

    private String genererCode() {
        StringBuilder code = new StringBuilder(LONGUEUR_CODE);
        for (int position = 0; position < LONGUEUR_CODE; position++) {
            code.append(ALPHABET[aleatoire.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}
