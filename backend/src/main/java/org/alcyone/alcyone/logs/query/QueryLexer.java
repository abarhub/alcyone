package org.alcyone.alcyone.logs.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Découpe une requête en tokens. Les sauts de ligne sont traités comme des espaces.
 * <p>
 * Règles : un token commençant par {@code .} est un champ ({@code .a.b}) ; {@code < > = !}
 * débutent un opérateur ; les mots-clés ({@code AND}/{@code OR}/{@code NOT}, {@code select}/
 * {@code filter}) sont émis comme des {@code WORD} et interprétés par le parseur.
 */
public final class QueryLexer {

    public enum TokenType { WORD, FIELD, OP, PIPE, LPAREN, RPAREN, COMMA, EOF }

    public record Token(TokenType type, String text, int pos) {}

    private final String input;
    private int i = 0;

    public QueryLexer(String input) {
        this.input = input == null ? "" : input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (i >= input.length()) {
                tokens.add(new Token(TokenType.EOF, "", i));
                return tokens;
            }
            char c = input.charAt(i);
            int start = i;
            switch (c) {
                case '|' -> { i++; tokens.add(new Token(TokenType.PIPE, "|", start)); }
                case '(' -> { i++; tokens.add(new Token(TokenType.LPAREN, "(", start)); }
                case ')' -> { i++; tokens.add(new Token(TokenType.RPAREN, ")", start)); }
                case ',' -> { i++; tokens.add(new Token(TokenType.COMMA, ",", start)); }
                case '"' -> tokens.add(readQuoted());
                case '<', '>', '=', '!' -> tokens.add(readOperator());
                case '.' -> tokens.add(readField());
                default -> tokens.add(readWord());
            }
        }
    }

    private void skipWhitespace() {
        while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
            i++;
        }
    }

    private Token readQuoted() {
        int start = i;
        i++; // guillemet ouvrant
        StringBuilder sb = new StringBuilder();
        while (i < input.length() && input.charAt(i) != '"') {
            sb.append(input.charAt(i));
            i++;
        }
        if (i >= input.length()) {
            throw new QueryParseException("Chaîne non terminée", start);
        }
        i++; // guillemet fermant
        return new Token(TokenType.WORD, sb.toString(), start);
    }

    private Token readOperator() {
        int start = i;
        char c = input.charAt(i);
        i++;
        switch (c) {
            case '>' -> { if (peekIs('=')) { i++; return op(">=", start); } return op(">", start); }
            case '<' -> { if (peekIs('=')) { i++; return op("<=", start); } return op("<", start); }
            case '=' -> { if (peekIs('=')) { i++; return op("==", start); } return op("=", start); }
            case '!' -> {
                if (peekIs('=')) { i++; return op("!=", start); }
                throw new QueryParseException("Opérateur invalide : '!' doit être suivi de '='", start);
            }
            default -> throw new QueryParseException("Opérateur invalide", start);
        }
    }

    private Token op(String text, int start) {
        return new Token(TokenType.OP, text, start);
    }

    private boolean peekIs(char c) {
        return i < input.length() && input.charAt(i) == c;
    }

    private Token readField() {
        int start = i;
        i++; // point initial
        StringBuilder sb = new StringBuilder();
        readSegment(sb, start);
        while (peekIs('.')) {
            i++;
            sb.append('.');
            readSegment(sb, start);
        }
        return new Token(TokenType.FIELD, sb.toString(), start);
    }

    private void readSegment(StringBuilder sb, int fieldStart) {
        int before = sb.length();
        while (i < input.length() && isIdentChar(input.charAt(i))) {
            sb.append(input.charAt(i));
            i++;
        }
        if (sb.length() == before) {
            throw new QueryParseException("Nom de champ attendu après '.'", fieldStart);
        }
    }

    private Token readWord() {
        int start = i;
        StringBuilder sb = new StringBuilder();
        while (i < input.length() && !isDelimiter(input.charAt(i))) {
            sb.append(input.charAt(i));
            i++;
        }
        return new Token(TokenType.WORD, sb.toString(), start);
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Délimiteurs de mot. Le point n'en fait pas partie (ex. {@code 10.5} reste un seul mot). */
    private static boolean isDelimiter(char c) {
        return Character.isWhitespace(c)
                || c == '|' || c == '(' || c == ')' || c == ','
                || c == '"' || c == '<' || c == '>' || c == '=' || c == '!';
    }
}
