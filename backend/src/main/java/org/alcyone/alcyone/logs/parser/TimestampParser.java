package org.alcyone.alcyone.logs.parser;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Parse l'horodatage d'un log en {@link Instant}.
 * <p>
 * Si un motif est fourni il est utilisé ; sinon un parsing souple est tenté (ISO avec zone,
 * décalage, ou date-heure locale — l'espace séparateur étant accepté à la place du {@code T}).
 * Quand l'horodatage ne porte pas de fuseau, la zone configurée est appliquée (UTC par défaut).
 */
public class TimestampParser {

    private final DateTimeFormatter formatter;
    private final ZoneId zone;

    public TimestampParser(String pattern, String zoneId) {
        this.formatter = (pattern == null || pattern.isBlank()) ? null : DateTimeFormatter.ofPattern(pattern);
        this.zone = (zoneId == null || zoneId.isBlank()) ? ZoneOffset.UTC : ZoneId.of(zoneId);
    }

    /** @return l'horodatage en millisecondes epoch, ou {@code null} si non parsable. */
    public Long toEpochMillis(String text) {
        Instant instant = parse(text);
        return instant == null ? null : instant.toEpochMilli();
    }

    public Instant parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        if (formatter != null) {
            try {
                return toInstant(formatter.parse(value));
            } catch (DateTimeException e) {
                return null;
            }
        }
        return parseFlexible(value);
    }

    private Instant parseFlexible(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeException ignored) {
            // pas un instant ISO avec 'Z'
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeException ignored) {
            // pas un date-heure avec décalage
        }
        String iso = value.contains("T") ? value : value.replaceFirst(" ", "T");
        try {
            return LocalDateTime.parse(iso).atZone(zone).toInstant();
        } catch (DateTimeException ignored) {
            // pas un date-heure local
        }
        try {
            return LocalDate.parse(iso).atStartOfDay(zone).toInstant();
        } catch (DateTimeException ignored) {
            // pas une date seule
        }
        return null;
    }

    private Instant toInstant(TemporalAccessor parsed) {
        try {
            return Instant.from(parsed);
        } catch (DateTimeException ignored) {
            // pas d'instant complet
        }
        try {
            return OffsetDateTime.from(parsed).toInstant();
        } catch (DateTimeException ignored) {
            // pas de décalage
        }
        try {
            return LocalDateTime.from(parsed).atZone(zone).toInstant();
        } catch (DateTimeException ignored) {
            // pas de date-heure local
        }
        return LocalDate.from(parsed).atStartOfDay(zone).toInstant();
    }
}
