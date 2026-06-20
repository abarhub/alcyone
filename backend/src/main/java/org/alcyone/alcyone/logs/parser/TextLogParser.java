package org.alcyone.alcyone.logs.parser;

import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogEntry;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser pour les logs en texte brut. Découpe chaque entrée via la regex configurée et regroupe
 * les lignes de continuation (stack traces, messages multi-lignes) dans l'entrée précédente.
 */
public class TextLogParser implements LogParser {

    private final Pattern pattern;

    public TextLogParser(LogProperties.Source source) {
        this.pattern = Pattern.compile(source.getLineRegex());
    }

    @Override
    public boolean startsNewEntry(String line) {
        return pattern.matcher(line).find();
    }

    @Override
    public LogEntry parse(List<String> lines, long startLine, String source) {
        String first = lines.getFirst();
        String raw = String.join("\n", lines);

        Matcher matcher = pattern.matcher(first);
        String timestamp = null;
        String level = null;
        String message;

        if (matcher.find()) {
            timestamp = group(matcher, "timestamp");
            level = group(matcher, "level");
            String head = group(matcher, "message");
            // On rattache les lignes de continuation au message affiché.
            message = lines.size() > 1
                    ? head + "\n" + String.join("\n", lines.subList(1, lines.size()))
                    : head;
        } else {
            // Aucune correspondance : on affiche la ligne telle quelle.
            message = raw;
        }

        return new LogEntry(timestamp, level, message, raw, source, startLine);
    }

    private static String group(Matcher matcher, String name) {
        try {
            String value = matcher.group(name);
            return (value == null || value.isBlank()) ? null : value.trim();
        } catch (IllegalArgumentException e) {
            // Le groupe nommé n'existe pas dans la regex personnalisée.
            return null;
        }
    }
}
