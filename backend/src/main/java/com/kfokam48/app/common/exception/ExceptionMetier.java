package com.kfokam48.app.common.exception;

import com.kfokam48.app.common.error.CodeErreur;
import org.springframework.http.HttpStatus;

/**
 * Erreur métier portée par un code stable et un statut HTTP conformes au contrat d'API.
 * Traduite en {@code { code, message }} par le gestionnaire d'erreurs centralisé (B4).
 */
public class ExceptionMetier extends RuntimeException {

    private final CodeErreur code;
    private final HttpStatus statut;

    public ExceptionMetier(CodeErreur code, HttpStatus statut, String message) {
        super(message);
        this.code = code;
        this.statut = statut;
    }

    public CodeErreur getCode() {
        return code;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}
