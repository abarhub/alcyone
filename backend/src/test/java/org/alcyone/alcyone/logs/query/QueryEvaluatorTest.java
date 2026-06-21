package org.alcyone.alcyone.logs.query;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class QueryEvaluatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final QueryEvaluator evaluator = new QueryEvaluator(mapper);

    private static final String RAW =
            "{\"level\":\"ERROR\",\"nb\":20,\"b\":{\"c\":\"x\"},\"msg\":\"connection timeout\"}";

    private boolean matches(String query) {
        Query q = QueryParser.parse(query);
        return evaluator.matchesAll(q, RAW, evaluator.parse(RAW));
    }

    @Test
    void textMatchIsCaseInsensitive() {
        assertThat(matches("TIMEOUT")).isTrue();
        assertThat(matches("absent")).isFalse();
    }

    @Test
    void numericComparison() {
        assertThat(matches("| filter .nb > 10")).isTrue();
        assertThat(matches("| filter .nb > 30")).isFalse();
        assertThat(matches("| filter .nb >= 20")).isTrue();
        assertThat(matches("| filter .nb == 20")).isTrue();
    }

    @Test
    void stringComparison() {
        assertThat(matches("| filter .level = ERROR")).isTrue();
        assertThat(matches("| filter .level != INFO")).isTrue();
        assertThat(matches("| filter .level = INFO")).isFalse();
    }

    @Test
    void nestedField() {
        assertThat(matches("| filter .b.c = x")).isTrue();
    }

    @Test
    void absentFieldIsFalse() {
        assertThat(matches("| filter .nope = 1")).isFalse();
        assertThat(matches("| filter .nope > 0")).isFalse();
    }

    @Test
    void notNegates() {
        assertThat(matches("NOT timeout")).isFalse();
        assertThat(matches("NOT absent")).isTrue();
    }

    @Test
    void combinedSearchAndFilter() {
        assertThat(matches("timeout | filter .nb > 10")).isTrue();
        assertThat(matches("timeout | filter .nb > 100")).isFalse();
        assertThat(matches("absent | filter .nb > 10")).isFalse();
    }

    @Test
    void projectSelectedFieldsAsCompactJson() {
        Query q = QueryParser.parse("| select .level, .b.c");
        String projected = evaluator.project(q, evaluator.parse(RAW));
        assertThat(projected).isEqualTo("{\"level\":\"ERROR\",\"b.c\":\"x\"}");
    }

    @Test
    void projectMissingFieldBecomesNull() {
        Query q = QueryParser.parse("| select .level, .nope");
        String projected = evaluator.project(q, evaluator.parse(RAW));
        assertThat(projected).isEqualTo("{\"level\":\"ERROR\",\"nope\":null}");
    }

    @Test
    void textSourceIgnoresFields() {
        // node null = source texte : comparaison de champ fausse, select sans effet.
        Query filter = QueryParser.parse("| filter .nb > 10");
        assertThat(evaluator.matchesAll(filter, RAW, null)).isFalse();

        Query select = QueryParser.parse("| select .level");
        assertThat(evaluator.project(select, null)).isNull();
    }

    @Test
    void usesFieldsDetection() {
        assertThat(QueryEvaluator.usesFields(QueryParser.parse("erreur"))).isFalse();
        assertThat(QueryEvaluator.usesFields(QueryParser.parse("| filter .nb > 1"))).isTrue();
        assertThat(QueryEvaluator.usesFields(QueryParser.parse("| select .a"))).isTrue();
    }
}
