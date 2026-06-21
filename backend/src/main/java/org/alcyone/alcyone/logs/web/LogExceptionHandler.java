package org.alcyone.alcyone.logs.web;

import org.alcyone.alcyone.logs.query.QueryParseException;
import org.alcyone.alcyone.logs.service.LogReadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les erreurs métier du module logs en réponses HTTP propres.
 */
@RestControllerAdvice(assignableTypes = LogController.class)
public class LogExceptionHandler {

    @ExceptionHandler(LogReadException.class)
    public ProblemDetail handleLogRead(LogReadException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Erreur de syntaxe de requête → 400, avec le message destiné à l'utilisateur. */
    @ExceptionHandler(QueryParseException.class)
    public ProblemDetail handleQueryParse(QueryParseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
