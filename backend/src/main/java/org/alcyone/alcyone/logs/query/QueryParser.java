package org.alcyone.alcyone.logs.query;

import org.alcyone.alcyone.logs.query.QueryLexer.Token;
import org.alcyone.alcyone.logs.query.QueryLexer.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parseur à descente récursive du mini-langage de requête.
 * <pre>
 * pipeline   := boolExpr? ( '|' command )*
 * command    := 'select' fieldRef (',' fieldRef)* | 'filter' boolExpr
 * orExpr     := andExpr ( 'OR' andExpr )*
 * andExpr    := notExpr ( 'AND'? notExpr )*        // juxtaposition = AND
 * notExpr    := 'NOT' notExpr | unary
 * unary      := '(' orExpr ')' | comparison | word
 * comparison := fieldRef op value
 * </pre>
 */
public final class QueryParser {

    private final List<Token> tokens;
    private int pos = 0;

    private QueryParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Analyse une requête. Une chaîne vide/blanche donne une requête sans recherche ni étape. */
    public static Query parse(String input) {
        List<Token> tokens = new QueryLexer(input).tokenize();
        return new QueryParser(tokens).parsePipeline();
    }

    private Query parsePipeline() {
        BoolExpr search = null;
        if (!check(TokenType.PIPE) && !check(TokenType.EOF)) {
            search = parseOr();
        }
        List<Stage> stages = new ArrayList<>();
        while (match(TokenType.PIPE)) {
            stages.add(parseCommand());
        }
        expect(TokenType.EOF, "Fin de requête attendue");
        return new Query(search, stages);
    }

    private Stage parseCommand() {
        Token cmd = expect(TokenType.WORD, "Commande attendue après '|' (select ou filter)");
        String name = cmd.text().toLowerCase();
        return switch (name) {
            case "select" -> parseSelect();
            case "filter" -> new Stage.Filter(parseOr());
            default -> throw new QueryParseException("Commande inconnue : '" + cmd.text() + "'", cmd.pos());
        };
    }

    private Stage parseSelect() {
        List<FieldRef> fields = new ArrayList<>();
        fields.add(parseFieldRef());
        while (match(TokenType.COMMA)) {
            fields.add(parseFieldRef());
        }
        return new Stage.Select(fields);
    }

    private FieldRef parseFieldRef() {
        Token t = expect(TokenType.FIELD, "Champ attendu (ex. .champ)");
        return new FieldRef(Arrays.asList(t.text().split("\\.")));
    }

    private BoolExpr parseOr() {
        BoolExpr left = parseAnd();
        while (matchWord("OR")) {
            left = new BoolExpr.Or(left, parseAnd());
        }
        return left;
    }

    private BoolExpr parseAnd() {
        BoolExpr left = parseNot();
        while (true) {
            if (matchWord("AND") || isOperandStart()) {
                left = new BoolExpr.And(left, parseNot());
            } else {
                return left;
            }
        }
    }

    private BoolExpr parseNot() {
        if (matchWord("NOT")) {
            return new BoolExpr.Not(parseNot());
        }
        return parseUnary();
    }

    private BoolExpr parseUnary() {
        if (match(TokenType.LPAREN)) {
            BoolExpr expr = parseOr();
            expect(TokenType.RPAREN, "Parenthèse fermante ')' attendue");
            return expr;
        }
        if (check(TokenType.FIELD)) {
            return parseComparison();
        }
        if (check(TokenType.WORD)) {
            Token word = advance();
            return new BoolExpr.TextMatch(word.text());
        }
        Token t = peek();
        throw new QueryParseException("Terme de recherche attendu", t.pos());
    }

    private BoolExpr parseComparison() {
        FieldRef field = parseFieldRef();
        Token opToken = expect(TokenType.OP, "Opérateur de comparaison attendu après le champ");
        Operator op = toOperator(opToken);
        Token valueToken = expect(TokenType.WORD, "Valeur attendue après l'opérateur");
        return new BoolExpr.Comparison(field, op, Value.of(valueToken.text()));
    }

    private Operator toOperator(Token t) {
        return switch (t.text()) {
            case "=", "==" -> Operator.EQ;
            case "!=" -> Operator.NE;
            case ">" -> Operator.GT;
            case "<" -> Operator.LT;
            case ">=" -> Operator.GE;
            case "<=" -> Operator.LE;
            default -> throw new QueryParseException("Opérateur inconnu : '" + t.text() + "'", t.pos());
        };
    }

    /** @return true si le token courant peut démarrer un opérande (pour l'AND implicite). */
    private boolean isOperandStart() {
        Token t = peek();
        return switch (t.type()) {
            case FIELD, LPAREN -> true;
            case WORD -> !t.text().equals("OR") && !t.text().equals("AND");
            default -> false;
        };
    }

    // --- utilitaires de flux de tokens -------------------------------------

    private Token peek() {
        return tokens.get(pos);
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean matchWord(String keyword) {
        Token t = peek();
        if (t.type() == TokenType.WORD && t.text().equals(keyword)) {
            pos++;
            return true;
        }
        return false;
    }

    private Token expect(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw new QueryParseException(message, peek().pos());
    }
}
