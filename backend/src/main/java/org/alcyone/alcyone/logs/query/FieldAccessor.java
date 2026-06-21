package org.alcyone.alcyone.logs.query;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Résout une référence de champ ({@code .a.b.c}) sur un nœud JSON.
 */
public final class FieldAccessor {

    private FieldAccessor() {
    }

    /**
     * @return le nœud au chemin demandé, ou {@code null} si une étape est absente ou si un
     *         intermédiaire n'est pas un objet.
     */
    public static JsonNode resolve(JsonNode node, List<String> path) {
        JsonNode current = node;
        for (String segment : path) {
            if (current == null || !current.isObject()) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }
}
