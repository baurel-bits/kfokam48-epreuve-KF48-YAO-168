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

    /** Requête HTTP refusée par Spring MVC (404, 405, ...). */
    DEMANDE_INVALIDE,

    /** Filet de sécurité : aucune erreur interne ne doit exposer de stack trace (ENF4). */
    ERREUR_INTERNE;

    public String code() {
        return name();
    }
}
