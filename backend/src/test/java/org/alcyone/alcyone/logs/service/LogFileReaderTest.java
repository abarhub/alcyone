package org.alcyone.alcyone.logs.service;

import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogFormat;
import org.alcyone.alcyone.logs.domain.LogPage;
import org.alcyone.alcyone.logs.parser.LogParserFactory;
import org.alcyone.alcyone.logs.query.Query;
import org.alcyone.alcyone.logs.query.QueryParser;
import org.alcyone.alcyone.logs.query.QueryEvaluator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test bout-en-bout du reader sur la source JSON d'exemple ({@code sample-logs/api.jsonl}).
 */
class LogFileReaderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LogFileReader reader =
            new LogFileReader(new LogParserFactory(mapper), new QueryEvaluator(mapper));

    private LogProperties.Source jsonSource() {
        LogProperties.Source source = new LogProperties.Source();
        source.setName("api-json");
        source.setPath("sample-logs/api.jsonl");
        source.setFormat(LogFormat.JSON);
        source.setTimestampField("@timestamp");
        source.setMessageField("message");
        source.setLevelField("level");
        return source;
    }

    private LogPage read(String query) {
        Query q = QueryParser.parse(query);
        return reader.read(jsonSource(), q, 0, 100);
    }

    @Test
    void noQueryReturnsAllEntries() {
        assertThat(read("").totalElements()).isEqualTo(20);
    }

    @Test
    void filterNumericField() {
        // status >= 500 : seule l'entrée 504 correspond.
        assertThat(read("| filter .status >= 500").totalElements()).isEqualTo(1);
        // status >= 400 : 504 et 409.
        assertThat(read("| filter .status >= 400").totalElements()).isEqualTo(2);
    }

    @Test
    void textSearchCombinedWithFilter() {
        LogPage page = read("paiement | filter .status == 200");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().getFirst().message()).contains("paiement");
    }

    @Test
    void selectProjectsMessageToCompactJson() {
        LogPage page = read("| select .level, .message");
        assertThat(page.content().getFirst().message())
                .isEqualTo("{\"level\":\"INFO\",\"message\":\"Demarrage de la passerelle API\"}");
        // Le raw d'origine reste disponible pour le dépliage.
        assertThat(page.content().getFirst().raw()).contains("\"logger\"");
    }

    @Test
    void notExcludesMatches() {
        long all = read("").totalElements();
        long errors = read("| filter .level == ERROR").totalElements();
        long notErrors = read("NOT .level == ERROR | filter .level != ZZZ").totalElements();
        // NOT .level==ERROR garde tout sauf les ERROR (tous ont un champ level).
        assertThat(notErrors).isEqualTo(all - errors);
    }
}
