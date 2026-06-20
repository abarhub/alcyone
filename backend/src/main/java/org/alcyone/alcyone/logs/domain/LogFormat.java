package org.alcyone.alcyone.logs.domain;

/**
 * Format d'un fichier de logs.
 */
public enum LogFormat {

    /** Logs en texte brut, une entrée par ligne (avec regroupement des lignes de continuation). */
    TEXT,

    /** Logs au format JSON Lines (NDJSON) : un objet JSON par ligne. */
    JSON
}
