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
 * en entrées et applique la requête (plage de dates + filtre + projection select).
 * <p>
 * Comme les fichiers sont statiques, un balayage complet par requête est acceptable : il permet
 * de connaître le nombre total d'entrées (pour la pagination) tout en ne gardant en mémoire que
 * l'entrée en cours de construction et le contenu de la page.
 */
@Component
public class LogFileReader {

    /** Reçoit chaque entrée correspondant à la requête, avec son nœud JSON parsé si disponible. */
    @FunctionalInterface
    private interface MatchHandler {
        void handle(LogEntry entry, JsonNode node);
    }

    private final LogParserFactory parserFactory;
    private final QueryEvaluator evaluator;

    public LogFileReader(LogParserFactory parserFactory, QueryEvaluator evaluator) {
        this.parserFactory = parserFactory;
        this.evaluator = evaluator;
    }

    /** Lit une page d'entrées correspondant à la requête et à la plage de dates. */
    public LogPage read(LogProperties.Source source, Query query, Long fromMillis, Long toMillis,
                        int page, int size) {
        long from = (long) page * size;
        long to = from + size;

        List<LogEntry> pageContent = new ArrayList<>();
        long[] matchCount = {0};

        scan(source, query, fromMillis, toMillis, (entry, node) -> {
            long index = matchCount[0]++;
            if (index >= from && index < to) {
                pageContent.add(project(entry, query, node));
            }
        });

        return LogPage.of(pageContent, source.getName(), page, size, matchCount[0]);
    }

    /** Collecte les horodatages (epoch millis) des entrées correspondantes, pour l'histogramme. */
    public List<Long> collectEpochs(LogProperties.Source source, Query query,
                                    Long fromMillis, Long toMillis) {
        List<Long> epochs = new ArrayList<>();
        scan(source, query, fromMillis, toMillis, (entry, node) -> {
            if (entry.epochMillis() != null) {
                epochs.add(entry.epochMillis());
            }
        });
        return epochs;
    }

    /** Balaye le fichier et invoque {@code handler} pour chaque entrée correspondant à la requête. */
    private void scan(LogProperties.Source source, Query query, Long fromMillis, Long toMillis,
                      MatchHandler handler) {
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
                    handleEntry(parser, current, currentStartLine, source.getName(),
                            query, parseJson, fromMillis, toMillis, handler);
                    current.clear();
                    currentStartLine = lineNumber;
                } else if (current.isEmpty()) {
                    currentStartLine = lineNumber;
                }
                current.add(line);
            }
            // Dernière entrée.
            if (!current.isEmpty()) {
                handleEntry(parser, current, currentStartLine, source.getName(),
                        query, parseJson, fromMillis, toMillis, handler);
            }
        } catch (IOException e) {
            throw new LogReadException("Erreur de lecture de la source '" + source.getName() + "'", e);
        }
    }

    /** Construit l'entrée et, si elle satisfait plage de dates et requête, la transmet au handler. */
    private void handleEntry(LogParser parser, List<String> lines, long startLine, String source,
                             Query query, boolean parseJson, Long fromMillis, Long toMillis,
                             MatchHandler handler) {
        LogEntry entry = parser.parse(lines, startLine, source);
        if (!inRange(entry, fromMillis, toMillis)) {
            return;
        }
        JsonNode node = parseJson ? evaluator.parse(entry.raw()) : null;
        if (!evaluator.matchesAll(query, entry.raw(), node)) {
            return;
        }
        handler.handle(entry, node);
    }

    /** @return true si l'entrée est dans la plage [fromMillis, toMillis). Une entrée sans date est exclue si une borne est posée. */
    private static boolean inRange(LogEntry entry, Long fromMillis, Long toMillis) {
        if (fromMillis == null && toMillis == null) {
            return true;
        }
        Long epoch = entry.epochMillis();
        if (epoch == null) {
            return false;
        }
        if (fromMillis != null && epoch < fromMillis) {
            return false;
        }
        return toMillis == null || epoch < toMillis;
    }

    /** Applique la projection {@code select} : remplace le message par le JSON projeté, le cas échéant. */
    private LogEntry project(LogEntry entry, Query query, JsonNode node) {
        String projected = evaluator.project(query, node);
        if (projected == null) {
            return entry;
        }
        return new LogEntry(entry.timestamp(), entry.epochMillis(), entry.level(), projected,
                entry.raw(), entry.source(), entry.lineNumber());
    }
}
