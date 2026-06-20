package org.alcyone.alcyone.logs.domain;

import java.util.List;

/**
 * Une page d'entrées de logs, avec les métadonnées de pagination calculées par le backend.
 *
 * @param content       entrées de la page
 * @param source        source lue
 * @param page          index de page (0-based)
 * @param size          taille de page demandée
 * @param totalElements nombre total d'entrées correspondant au filtre
 * @param totalPages    nombre total de pages
 */
public record LogPage(
        List<LogEntry> content,
        String source,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static LogPage of(List<LogEntry> content, String source, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new LogPage(content, source, page, size, totalElements, totalPages);
    }
}
