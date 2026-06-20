package org.alcyone.alcyone.logs.service;

import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogEntry;
import org.alcyone.alcyone.logs.domain.LogPage;
import org.alcyone.alcyone.logs.domain.LogQuery;
import org.alcyone.alcyone.logs.parser.LogParser;
import org.alcyone.alcyone.logs.parser.LogParserFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lit un fichier de logs en streaming (jamais tout le fichier en mémoire), regroupe les lignes
 * en entrées, applique le filtre de recherche et ne conserve que la page demandée.
 * <p>
 * Comme les fichiers sont statiques, un balayage complet par requête est acceptable : il permet
 * de connaître le nombre total d'entrées (pour la pagination) tout en ne gardant en mémoire que
 * l'entrée en cours de construction et le contenu de la page.
 */
@Component
public class LogFileReader {

    private final LogParserFactory parserFactory;

    public LogFileReader(LogParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    public LogPage read(LogProperties.Source source, LogQuery query) {
        Path path = Path.of(source.getPath());
        if (!Files.exists(path)) {
            throw new LogReadException("Fichier introuvable pour la source '" + source.getName() + "' : " + path);
        }
        if (!Files.isReadable(path)) {
            throw new LogReadException("Fichier illisible pour la source '" + source.getName() + "' : " + path);
        }

        LogParser parser = parserFactory.create(source);
        String needle = query.hasSearch() ? query.search().toLowerCase(Locale.ROOT) : null;

        long from = query.offset();
        long to = from + query.size();

        List<LogEntry> pageContent = new ArrayList<>();
        long matchCount = 0;

        // Accumulateur de l'entrée courante (première ligne + continuations).
        List<String> current = new ArrayList<>();
        long currentStartLine = 1;
        long lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!current.isEmpty() && parser.startsNewEntry(line)) {
                    matchCount = flush(parser, current, currentStartLine, source.getName(),
                            needle, from, to, matchCount, pageContent);
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
                        needle, from, to, matchCount, pageContent);
            }
        } catch (IOException e) {
            throw new LogReadException("Erreur de lecture de la source '" + source.getName() + "'", e);
        }

        return LogPage.of(pageContent, source.getName(), query.page(), query.size(), matchCount);
    }

    /**
     * Construit l'entrée à partir des lignes accumulées, applique le filtre et, si elle correspond,
     * incrémente le compteur et l'ajoute à la page si elle tombe dans la fenêtre [from, to).
     *
     * @return le nouveau nombre d'entrées correspondant au filtre
     */
    private long flush(LogParser parser, List<String> lines, long startLine, String source,
                       String needle, long from, long to, long matchCount, List<LogEntry> pageContent) {
        LogEntry entry = parser.parse(lines, startLine, source);
        if (needle != null && !entry.raw().toLowerCase(Locale.ROOT).contains(needle)) {
            return matchCount;
        }
        if (matchCount >= from && matchCount < to) {
            pageContent.add(entry);
        }
        return matchCount + 1;
    }
}
