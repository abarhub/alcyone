package org.alcyone.alcyone.logs.domain;

/**
 * Une entrée de log normalisée, quel que soit le format d'origine (texte ou JSON).
 *
 * @param timestamp   date/heure de l'entrée, telle qu'extraite du log (peut être {@code null})
 * @param epochMillis date/heure parsée en millisecondes epoch (UTC), ou {@code null} si non parsable
 * @param level       niveau de log (INFO, WARN, ERROR, ...) si détectable (peut être {@code null})
 * @param message     message à afficher dans la grande colonne
 * @param raw         ligne(s) brute(s) d'origine, pour le détail
 * @param source      nom de la source (fichier) d'origine
 * @param lineNumber  numéro de la première ligne de l'entrée dans le fichier (commence à 1)
 */
public record LogEntry(
        String timestamp,
        Long epochMillis,
        String level,
        String message,
        String raw,
        String source,
        long lineNumber
) {
}
