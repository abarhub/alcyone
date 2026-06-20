package org.alcyone.alcyone.logs.web;

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
}
