package org.alcyone.alcyone.logs.parser;

import org.alcyone.alcyone.logs.domain.LogEntry;

import java.util.List;

/**
 * Transforme les lignes brutes d'un fichier en {@link LogEntry}.
 * <p>
 * La lecture du fichier (et le regroupement des lignes en entrées) est pilotée par le reader,
 * qui s'appuie sur {@link #startsNewEntry(String)} pour savoir où commence une nouvelle entrée.
 */
public interface LogParser {

    /**
     * Indique si la ligne marque le début d'une nouvelle entrée de log.
     * Pour le JSON (une entrée par ligne), c'est toujours vrai. Pour le texte, c'est vrai
     * lorsque la ligne commence par un horodatage ; les autres lignes sont des continuations.
     */
    boolean startsNewEntry(String line);

    /**
     * Construit une entrée à partir des lignes qui la composent (la première + d'éventuelles continuations).
     *
     * @param lines      lignes brutes de l'entrée (au moins une)
     * @param startLine  numéro (1-based) de la première ligne dans le fichier
     * @param source     nom de la source
     */
    LogEntry parse(List<String> lines, long startLine, String source);
}
