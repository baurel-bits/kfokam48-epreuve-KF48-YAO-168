package com.kfokam48.app.common.error;

/**
 * Corps d'erreur imposé par le contrat : exactement deux champs, jamais de stack trace.
 */
public record ReponseErreur(String code, String message) {
}
