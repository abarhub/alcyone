package org.alcyone.alcyone.logs.parser;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampParserTest {

    private final TimestampParser utc = new TimestampParser(null, "UTC");

    @Test
    void parsesTextTimestampWithSpaceAsUtc() {
        Instant expected = Instant.parse("2026-06-20T08:00:01.123Z");
        assertThat(utc.parse("2026-06-20 08:00:01.123")).isEqualTo(expected);
    }

    @Test
    void parsesIsoInstantWithZone() {
        assertThat(utc.parse("2026-06-20T09:00:00.100Z"))
                .isEqualTo(Instant.parse("2026-06-20T09:00:00.100Z"));
    }

    @Test
    void parsesOffsetDateTime() {
        assertThat(utc.parse("2026-06-20T10:00:00+02:00"))
                .isEqualTo(Instant.parse("2026-06-20T08:00:00Z"));
    }

    @Test
    void parsesDateTimeWithoutSeconds() {
        assertThat(utc.parse("2026-06-20T08:00"))
                .isEqualTo(Instant.parse("2026-06-20T08:00:00Z"));
    }

    @Test
    void appliesConfiguredZone() {
        TimestampParser paris = new TimestampParser(null, "Europe/Paris");
        // 08:00 à Paris (heure d'été = +02:00) -> 06:00 UTC
        assertThat(paris.parse("2026-06-20 08:00:00"))
                .isEqualTo(Instant.parse("2026-06-20T06:00:00Z"));
    }

    @Test
    void usesExplicitPattern() {
        TimestampParser custom = new TimestampParser("dd/MM/yyyy HH:mm:ss", "UTC");
        assertThat(custom.parse("20/06/2026 08:00:00"))
                .isEqualTo(Instant.parse("2026-06-20T08:00:00Z"));
    }

    @Test
    void returnsNullForBlankOrInvalid() {
        assertThat(utc.parse(null)).isNull();
        assertThat(utc.parse("   ")).isNull();
        assertThat(utc.parse("pas une date")).isNull();
        assertThat(utc.toEpochMillis("pas une date")).isNull();
    }
}
