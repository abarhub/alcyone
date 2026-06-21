package org.alcyone.alcyone.logs.parser;

import tools.jackson.databind.ObjectMapper;
import org.alcyone.alcyone.logs.config.LogProperties;
import org.springframework.stereotype.Component;

/**
 * Fabrique le {@link LogParser} adapté au format d'une source.
 */
@Component
public class LogParserFactory {

    private final ObjectMapper objectMapper;

    public LogParserFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LogParser create(LogProperties.Source source) {
        TimestampParser timestampParser =
                new TimestampParser(source.getTimestampFormat(), source.getTimestampZone());
        return switch (source.getFormat()) {
            case TEXT -> new TextLogParser(source, timestampParser);
            case JSON -> new JsonLogParser(source, objectMapper, timestampParser);
        };
    }
}
