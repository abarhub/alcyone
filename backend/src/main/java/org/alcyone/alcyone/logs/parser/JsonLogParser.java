package org.alcyone.alcyone.logs.parser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogEntry;

import java.util.List;

/**
 * Parser pour les logs au format JSON Lines (NDJSON) : un objet JSON par ligne.
 */
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper;
    private final TimestampParser timestampParser;
    private final String timestampField;
    private final String messageField;
    private final String levelField;

    public JsonLogParser(LogProperties.Source source, ObjectMapper objectMapper, TimestampParser timestampParser) {
        this.objectMapper = objectMapper;
        this.timestampParser = timestampParser;
        this.timestampField = source.getTimestampField();
        this.messageField = source.getMessageField();
        this.levelField = source.getLevelField();
    }

    @Override
    public boolean startsNewEntry(String line) {
        // En NDJSON, chaque ligne non vide est une entrée complète.
        return true;
    }

    @Override
    public LogEntry parse(List<String> lines, long startLine, String source) {
        String raw = String.join("\n", lines);
        try {
            JsonNode node = objectMapper.readTree(raw);
            String timestamp = text(node, timestampField);
            String level = text(node, levelField);
            String message = text(node, messageField);
            // Repli : si le champ message est absent, on affiche le JSON brut.
            if (message == null) {
                message = raw;
            }
            return new LogEntry(timestamp, timestampParser.toEpochMillis(timestamp),
                    level, message, raw, source, startLine);
        } catch (Exception e) {
            // Ligne JSON invalide : on l'expose en l'état plutôt que d'échouer.
            return new LogEntry(null, null, null, raw, raw, source, startLine);
        }
    }

    private static String text(JsonNode node, String field) {
        if (field == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }
}
