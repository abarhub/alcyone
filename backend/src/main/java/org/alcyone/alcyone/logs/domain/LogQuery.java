package org.alcyone.alcyone.logs.domain;

import java.util.List;
import java.util.Locale;

/**
 * Critères de lecture d'une source de logs : pagination et recherche texte.
 * <p>
 * Le champ {@code search} peut contenir plusieurs lignes : chaque ligne non vide est un critère,
 * et une entrée correspond si elle contient <em>tous</em> les critères (ET, insensible à la casse).
 *
 * @param source nom de la source à lire
 * @param search texte recherché ; {@code null} ou vide = pas de filtre
 * @param page   index de page, commence à 0
 * @param size   nombre d'entrées par page
 */
public record LogQuery(String source, String search, int page, int size) {

    /** @return true si au moins un critère de recherche est actif. */
    public boolean hasSearch() {
        return !searchTerms().isEmpty();
    }

    /** @return les critères de recherche (une ligne = un critère), en minuscules, sans lignes vides. */
    public List<String> searchTerms() {
        if (search == null || search.isBlank()) {
            return List.of();
        }
        return search.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    /** @return index (0-based) de la première entrée de la page. */
    public long offset() {
        return (long) page * size;
    }
}
