package org.alcyone.alcyone.logs.query;

/**
 * Erreur de syntaxe lors de l'analyse d'une requête. Le message est destiné à l'utilisateur.
 */
public class QueryParseException extends RuntimeException {

    private final int position;

    public QueryParseException(String message, int position) {
        super(message + " (position " + position + ")");
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
