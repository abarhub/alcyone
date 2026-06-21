package org.alcyone.alcyone.logs.service;

import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogPage;
import org.alcyone.alcyone.logs.parser.TimestampParser;
import org.alcyone.alcyone.logs.query.Query;
import org.alcyone.alcyone.logs.query.QueryParser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Point d'entrée métier du module logs : liste des sources et lecture paginée/filtrée.
 */
@Service
public class LogService {

    private final LogProperties properties;
    private final LogFileReader reader;

    public LogService(LogProperties properties, LogFileReader reader) {
        this.properties = properties;
        this.reader = reader;
    }

    /** @return la liste des sources configurées. */
    public List<LogProperties.Source> listSources() {
        return properties.getSources();
    }

    /**
     * Lit une page de logs d'une source, en normalisant et bornant les paramètres de pagination.
     *
     * @throws LogReadException                                        si la source est inconnue ou le fichier illisible
     * @throws org.alcyone.alcyone.logs.query.QueryParseException si la requête est syntaxiquement invalide
     */
    public LogPage read(String sourceName, String search, String from, String to,
                        Integer page, Integer size) {
        LogProperties.Source source = findSource(sourceName);

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? properties.getDefaultPageSize() : size;
        safeSize = Math.min(safeSize, properties.getMaxPageSize());

        Query query = QueryParser.parse(search);

        TimestampParser timestampParser =
                new TimestampParser(source.getTimestampFormat(), source.getTimestampZone());
        Long fromMillis = timestampParser.toEpochMillis(from);
        Long toMillis = timestampParser.toEpochMillis(to);

        return reader.read(source, query, fromMillis, toMillis, safePage, safeSize);
    }

    private LogProperties.Source findSource(String name) {
        Optional<LogProperties.Source> found = properties.getSources().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst();
        return found.orElseThrow(() -> new LogReadException("Source de logs inconnue : '" + name + "'"));
    }
}
