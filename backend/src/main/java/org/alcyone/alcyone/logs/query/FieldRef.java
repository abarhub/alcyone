package org.alcyone.alcyone.logs.query;

import java.util.List;

/**
 * Référence à un champ JSON, éventuellement imbriqué (ex. {@code .b.c} → path [b, c]).
 *
 * @param path segments du chemin (sans le point initial)
 */
public record FieldRef(List<String> path) {

    /** @return le chemin sous forme pointée, ex. {@code "b.c"} (utilisé comme clé dans un select). */
    public String dotted() {
        return String.join(".", path);
    }
}
