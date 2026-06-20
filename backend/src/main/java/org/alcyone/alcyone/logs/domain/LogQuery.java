package org.alcyone.alcyone.logs.domain;

/**
 * Critères de lecture d'une source de logs : pagination et recherche texte.
 *
 * @param source nom de la source à lire
 * @param search texte recherché (filtre « contient », insensible à la casse) ; {@code null} ou vide = pas de filtre
 * @param page   index de page, commence à 0
 * @param size   nombre d'entrées par page
 */
public record LogQuery(String source, String search, int page, int size) {

    /** @return true si un filtre de recherche est actif. */
    public boolean hasSearch() {
        return search != null && !search.isBlank();
    }

    /** @return index (0-based) de la première entrée de la page. */
    public long offset() {
        return (long) page * size;
    }
}
