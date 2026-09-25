package com.kfokam48.app.common.error;

/**
 * Identifiants stables d'erreur, en majuscules, renvoyés dans le champ {@code code}
 * du format imposé par le contrat ({@code { "code": "...", "message": "..." }}).
 */
public enum CodeErreur {

    /** Entrée refusée par la validation des DTO (B4). */
    VALIDATION_INVALIDE,

    /** Corps de requête absent ou non désérialisable. */
    REQUETE_ILLISIBLE,

    /** La promotion visée n'existe pas. */
    PROMOTION_INCONNUE,

    /** L'étudiant visé n'existe pas. */
    ETUDIANT_INCONNU,

    /** EF2 — aucun session ne porte le code de présence saisi (contrat : {@code 400}). */
    CODE_INCONNU,

    /** EF2 (RG1) — le code saisi a dépassé ses 15 minutes (contrat : {@code 410}). */
    CODE_EXPIRE,

    /** EF2 — le couple (session, étudiant) a déjà une présence (contrat : {@code 409}). */
    DEJA_PRESENT,

    /** EF2 (RG3) — 5 échecs de saisie atteints, étudiant bloqué 2 minutes (contrat : {@code 429}). */
    TROP_DE_TENTATIVES,

    /** EF3 — la session visée n'existe pas. */
    SESSION_INCONNUE,

    /** EF3 — le lien de l'exercice n'est pas une URL http(s) exploitable (contrat : {@code 400}). */
    LIEN_INVALIDE,

    /** EF3 — l'étudiant a déjà un exercice sur cette session (contrat : {@code 409}). */
    EXERCICE_DEJA_DEPOSE,

    /** EF3 (RG10/RG14) — la session est clôturée, plus aucun dépôt n'est accepté (contrat : {@code 409}). */
    SESSION_CLOTUREE,

    /** Requête HTTP refusée par Spring MVC (404, 405, ...). */
    DEMANDE_INVALIDE,

    /** Filet de sécurité : aucune erreur interne ne doit exposer de stack trace (ENF4). */
    ERREUR_INTERNE;

    public String code() {
        return name();
    }
}
