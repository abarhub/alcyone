package org.alcyone.alcyone.logs.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryParserTest {

    @Test
    void emptyQuery() {
        Query q = QueryParser.parse("   ");
        assertThat(q.search()).isNull();
        assertThat(q.stages()).isEmpty();
    }

    @Test
    void singleWordIsTextMatch() {
        Query q = QueryParser.parse("erreur");
        assertThat(q.search()).isEqualTo(new BoolExpr.TextMatch("erreur"));
    }

    @Test
    void quotedTermKeepsSpaces() {
        Query q = QueryParser.parse("\"connection timeout\"");
        assertThat(q.search()).isEqualTo(new BoolExpr.TextMatch("connection timeout"));
    }

    @Test
    void juxtapositionIsImplicitAnd() {
        Query q = QueryParser.parse("a b");
        assertThat(q.search()).isEqualTo(
                new BoolExpr.And(new BoolExpr.TextMatch("a"), new BoolExpr.TextMatch("b")));
    }

    @Test
    void orHasLowerPrecedenceThanAnd() {
        // a OR b c  ==  a OR (b AND c)
        Query q = QueryParser.parse("a OR b c");
        assertThat(q.search()).isEqualTo(new BoolExpr.Or(
                new BoolExpr.TextMatch("a"),
                new BoolExpr.And(new BoolExpr.TextMatch("b"), new BoolExpr.TextMatch("c"))));
    }

    @Test
    void notBindsTighterThanAnd() {
        // NOT a b  ==  (NOT a) AND b
        Query q = QueryParser.parse("NOT a b");
        assertThat(q.search()).isEqualTo(new BoolExpr.And(
                new BoolExpr.Not(new BoolExpr.TextMatch("a")),
                new BoolExpr.TextMatch("b")));
    }

    @Test
    void doubleNot() {
        Query q = QueryParser.parse("NOT NOT a");
        assertThat(q.search()).isEqualTo(
                new BoolExpr.Not(new BoolExpr.Not(new BoolExpr.TextMatch("a"))));
    }

    @Test
    void parenthesesOverridePrecedence() {
        Query q = QueryParser.parse("NOT (titi OR tutu)");
        assertThat(q.search()).isEqualTo(new BoolExpr.Not(
                new BoolExpr.Or(new BoolExpr.TextMatch("titi"), new BoolExpr.TextMatch("tutu"))));
    }

    @Test
    void comparisonNumericAndNested() {
        Query q = QueryParser.parse("| filter .b.c >= 15");
        Stage.Filter filter = (Stage.Filter) q.stages().getFirst();
        BoolExpr.Comparison cmp = (BoolExpr.Comparison) filter.predicate();
        assertThat(cmp.field().path()).containsExactly("b", "c");
        assertThat(cmp.op()).isEqualTo(Operator.GE);
        assertThat(cmp.value().isNumeric()).isTrue();
        assertThat(cmp.value().number()).isEqualTo(15.0);
    }

    @Test
    void comparisonWithoutSpaces() {
        Query q = QueryParser.parse("| filter .nb>10");
        Stage.Filter filter = (Stage.Filter) q.stages().getFirst();
        BoolExpr.Comparison cmp = (BoolExpr.Comparison) filter.predicate();
        assertThat(cmp.field().path()).containsExactly("nb");
        assertThat(cmp.op()).isEqualTo(Operator.GT);
    }

    @Test
    void equalsAliasesToEq() {
        Query q = QueryParser.parse("| filter .level = ERROR");
        BoolExpr.Comparison cmp = (BoolExpr.Comparison) ((Stage.Filter) q.stages().getFirst()).predicate();
        assertThat(cmp.op()).isEqualTo(Operator.EQ);
        assertThat(cmp.value().text()).isEqualTo("ERROR");
        assertThat(cmp.value().isNumeric()).isFalse();
    }

    @Test
    void selectMultipleFields() {
        Query q = QueryParser.parse("| select .a, .b.c");
        Stage.Select select = (Stage.Select) q.stages().getFirst();
        assertThat(select.fields()).hasSize(2);
        assertThat(select.fields().get(0).path()).containsExactly("a");
        assertThat(select.fields().get(1).path()).containsExactly("b", "c");
    }

    @Test
    void fullPipeline() {
        Query q = QueryParser.parse("erreur (titi OR tutu) | filter .nb > 15 | select .a, .b.c");
        assertThat(q.search()).isEqualTo(new BoolExpr.And(
                new BoolExpr.TextMatch("erreur"),
                new BoolExpr.Or(new BoolExpr.TextMatch("titi"), new BoolExpr.TextMatch("tutu"))));
        assertThat(q.stages()).hasSize(2);
        assertThat(q.stages().get(0)).isInstanceOf(Stage.Filter.class);
        assertThat(q.stages().get(1)).isInstanceOf(Stage.Select.class);
        assertThat(q.selectedFields()).hasSize(2);
        assertThat(q.predicates()).hasSize(2); // recherche de tête + 1 filter
    }

    @Test
    void newlinesAreWhitespace() {
        Query q = QueryParser.parse("a\nb");
        assertThat(q.search()).isEqualTo(
                new BoolExpr.And(new BoolExpr.TextMatch("a"), new BoolExpr.TextMatch("b")));
    }

    @Test
    void unbalancedParenthesisFails() {
        assertThatThrownBy(() -> QueryParser.parse("(a OR b"))
                .isInstanceOf(QueryParseException.class);
    }

    @Test
    void missingValueFails() {
        assertThatThrownBy(() -> QueryParser.parse("| filter .nb >"))
                .isInstanceOf(QueryParseException.class);
    }

    @Test
    void unknownCommandFails() {
        assertThatThrownBy(() -> QueryParser.parse("| stats .a"))
                .isInstanceOf(QueryParseException.class)
                .hasMessageContaining("Commande inconnue");
    }

    @Test
    void unterminatedStringFails() {
        assertThatThrownBy(() -> QueryParser.parse("\"abc"))
                .isInstanceOf(QueryParseException.class)
                .hasMessageContaining("non terminée");
    }

    @Test
    void fieldWithoutOperatorFails() {
        assertThatThrownBy(() -> QueryParser.parse("| filter .nb 15"))
                .isInstanceOf(QueryParseException.class);
    }
}
