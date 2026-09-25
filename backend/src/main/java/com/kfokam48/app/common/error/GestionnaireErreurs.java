package com.kfokam48.app.common.error;

import com.kfokam48.app.common.exception.ExceptionMetier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Gestion centralisée des erreurs (B4) : toute réponse d'erreur respecte le format
 * imposé {@code { "code": "...", "message": "..." }} et n'expose jamais de stack trace.
 * L'héritage de {@link ResponseEntityExceptionHandler} conserve les statuts attendus
 * des erreurs Spring MVC (404, 405, ...) tout en reformatant leur corps.
 */
@RestControllerAdvice
public class GestionnaireErreurs extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    @ExceptionHandler(ExceptionMetier.class)
    public ResponseEntity<ReponseErreur> gererErreurMetier(ExceptionMetier erreur) {
        return ResponseEntity.status(erreur.getStatut())
                .body(new ReponseErreur(erreur.getCode().code(), erreur.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException erreur, HttpHeaders headers, HttpStatusCode statut, WebRequest requete) {
        String message = erreur.getBindingResult().getFieldErrors().stream()
                .map(erreurChamp -> erreurChamp.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ReponseErreur(CodeErreur.VALIDATION_INVALIDE.code(),
                        message.isBlank() ? "La requête est invalide." : message));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException erreur, HttpHeaders headers, HttpStatusCode statut, WebRequest requete) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ReponseErreur(CodeErreur.REQUETE_ILLISIBLE.code(),
                        "Le corps de la requête est absent ou n'est pas un JSON valide."));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception erreur, Object corps, HttpHeaders headers, HttpStatusCode statut, WebRequest requete) {
        boolean erreurCliente = statut.is4xxClientError();
        CodeErreur code = erreurCliente ? CodeErreur.DEMANDE_INVALIDE : CodeErreur.ERREUR_INTERNE;
        String message = erreurCliente
                ? "La requête n'a pas pu être traitée (HTTP " + statut.value() + ")."
                : "Une erreur interne est survenue.";
        if (!erreurCliente) {
            log.error("Erreur non gérée par un gestionnaire dédié", erreur);
        }
        return ResponseEntity.status(statut).body(new ReponseErreur(code.code(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> gererErreurInattendue(Exception erreur) {
        log.error("Erreur inattendue", erreur);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ReponseErreur(CodeErreur.ERREUR_INTERNE.code(), "Une erreur interne est survenue."));
    }
}
