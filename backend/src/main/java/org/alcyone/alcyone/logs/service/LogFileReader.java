package org.alcyone.alcyone.logs.service;

import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogEntry;
import org.alcyone.alcyone.logs.domain.LogFormat;
import org.alcyone.alcyone.logs.domain.LogPage;
import org.alcyone.alcyone.logs.parser.LogParser;
import org.alcyone.alcyone.logs.parser.LogParserFactory;
import org.alcyone.alcyone.logs.query.Query;
import org.alcyone.alcyone.logs.query.QueryEvaluator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lit un fichier de logs en streaming (jamais tout le fichier en mémoire), regroupe les lignes
 * en entrées, applique la requête (filtre + projection select) et ne conserve que la page demandée.
 * <p>
 * Comme les fichiers sont statiques, un balayage complet par requête est acceptable : il permet
 * de connaître le nombre total d'entrées (pour la pagination) tout en ne gardant en mémoire que
 * l'entrée en cours de construction et le contenu de la page.
 */
@Component
public class LogFileReader {

    private final LogParserFactory parserFactory;
    private final QueryEvaluator evaluator;

    public LogFileReader(LogParserFactory parserFactory, QueryEvaluator evaluator) {
        this.parserFactory = parserFactory;
        this.evaluator = evaluator;
    }

    public LogPage read(LogProperties.Source source, Query query, int page, int size) {
        Path path = Path.of(source.getPath());
        if (!Files.exists(path)) {
            throw new LogReadException("Fichier introuvable pour la source '" + source.getName() + "' : " + path);
        }
        if (!Files.isReadable(path)) {
            throw new LogReadException("Fichier illisible pour la source '" + source.getName() + "' : " + path);
        }

        LogParser parser = parserFactory.create(source);
        // On ne parse le JSON (pour les champs) que si la source est JSON et que la requête en a besoin.
        boolean parseJson = source.getFormat() == LogFormat.JSON && QueryEvaluator.usesFields(query);

        long from = (long) page * size;
        long to = from + size;

        List<LogEntry> pageContent = new ArrayList<>();
        long matchCount = 0;

        // Accumulateur de l'entrée courante (première ligne + continuations).
        List<String> current = new ArrayList<>();
        long currentStartLine = 1;
        long lineNumber = 0;

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith("?");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(path), decoder))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!current.isEmpty() && parser.startsNewEntry(line)) {
                    matchCount = flush(parser, current, currentStartLine, source.getName(),
                            query, parseJson, from, to, matchCount, pageContent);
                    current.clear();
                    currentStartLine = lineNumber;
                } else if (current.isEmpty()) {
                    currentStartLine = lineNumber;
                }
                current.add(line);
            }
            // Dernière entrée.
            if (!current.isEmpty()) {
                matchCount = flush(parser, current, currentStartLine, source.getName(),
                        query, parseJson, from, to, matchCount, pageContent);
            }
        } catch (IOException e) {
            throw new LogReadException("Erreur de lecture de la source '" + source.getName() + "'", e);
        }

        return LogPage.of(pageContent, source.getName(), page, size, matchCount);
    }

    /**
     * Construit l'entrée, applique la requête et, si elle correspond, incrémente le compteur et
     * l'ajoute à la page (avec projection select) si elle tombe dans la fenêtre [from, to).
     *
     * @return le nouveau nombre d'entrées correspondant à la requête
     */
    private long flush(LogParser parser, List<String> lines, long startLine, String source,
                       Query query, boolean parseJson, long from, long to, long matchCount,
                       List<LogEntry> pageContent) {
        LogEntry entry = parser.parse(lines, startLine, source);
        JsonNode node = parseJson ? evaluator.parse(entry.raw()) : null;
        if (!evaluator.matchesAll(query, entry.raw(), node)) {
            return matchCount;
        }
        if (matchCount >= from && matchCount < to) {
            pageContent.add(project(entry, query, node));
        }
        return matchCount + 1;
    }

    /** Applique la projection {@code select} : remplace le message par le JSON projeté, le cas échéant. */
    private LogEntry project(LogEntry entry, Query query, JsonNode node) {
        String projected = evaluator.project(query, node);
        if (projected == null) {
            return entry;
        }
        return new LogEntry(entry.timestamp(), entry.level(), projected, entry.raw(),
                entry.source(), entry.lineNumber());
    }
}
