package org.alcyone.alcyone.logs.service;

/**
 * Erreur fonctionnelle lors de la lecture d'une source de logs (source inconnue, fichier absent, ...).
 */
public class LogReadException extends RuntimeException {

    public LogReadException(String message) {
        super(message);
    }

    public LogReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
