package org.alcyone.alcyone.logs.query;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;

/**
 * Évalue une requête contre une entrée de log : test du prédicat et projection {@code select}.
 * <p>
 * L'accès aux champs ({@code .a.b}) suppose une source JSON ; pour le texte, le nœud est {@code null}
 * et toute comparaison de champ est fausse (un {@code select} est alors sans effet).
 */
@Component
public class QueryEvaluator {

    private final ObjectMapper objectMapper;

    public QueryEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** @return true si la requête référence un champ (comparaison ou select) → besoin de parser le JSON. */
    public static boolean usesFields(Query query) {
        if (!query.selectedFields().isEmpty()) {
            return true;
        }
        return query.predicates().stream().anyMatch(QueryEvaluator::hasComparison);
    }

    private static boolean hasComparison(BoolExpr expr) {
        return switch (expr) {
            case BoolExpr.And a -> hasComparison(a.left()) || hasComparison(a.right());
            case BoolExpr.Or o -> hasComparison(o.left()) || hasComparison(o.right());
            case BoolExpr.Not n -> hasComparison(n.expr());
            case BoolExpr.Comparison ignored -> true;
            case BoolExpr.TextMatch ignored -> false;
        };
    }

    /** Parse une ligne JSON, ou {@code null} si elle n'est pas du JSON valide. */
    public JsonNode parse(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return true si l'entrée satisfait tous les prédicats de la requête (recherche + filtres). */
    public boolean matchesAll(Query query, String raw, JsonNode node) {
        String rawLower = raw.toLowerCase(Locale.ROOT);
        for (BoolExpr predicate : query.predicates()) {
            if (!matches(predicate, rawLower, node)) {
                return false;
            }
        }
        return true;
    }

    boolean matches(BoolExpr expr, String rawLower, JsonNode node) {
        return switch (expr) {
            case BoolExpr.And a -> matches(a.left(), rawLower, node) && matches(a.right(), rawLower, node);
            case BoolExpr.Or o -> matches(o.left(), rawLower, node) || matches(o.right(), rawLower, node);
            case BoolExpr.Not n -> !matches(n.expr(), rawLower, node);
            case BoolExpr.TextMatch t -> rawLower.contains(t.term().toLowerCase(Locale.ROOT));
            case BoolExpr.Comparison c -> compare(node, c);
        };
    }

    private boolean compare(JsonNode node, BoolExpr.Comparison c) {
        if (node == null) {
            return false;
        }
        JsonNode field = FieldAccessor.resolve(node, c.field().path());
        if (field == null || field.isNull() || field.isMissingNode()) {
            return false;
        }
        Value value = c.value();
        if (value.isNumeric()) {
            Double fieldNumber = asDouble(field);
            if (fieldNumber != null) {
                return applyNumeric(fieldNumber, c.op(), value.number());
            }
        }
        return applyString(field.asString(), c.op(), value.text());
    }

    private static Double asDouble(JsonNode field) {
        if (field.isNumber()) {
            return field.doubleValue();
        }
        try {
            return Double.parseDouble(field.asString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean applyNumeric(double a, Operator op, double b) {
        return switch (op) {
            case EQ -> a == b;
            case NE -> a != b;
            case GT -> a > b;
            case LT -> a < b;
            case GE -> a >= b;
            case LE -> a <= b;
        };
    }

    private static boolean applyString(String a, Operator op, String b) {
        int cmp = a.compareTo(b);
        return switch (op) {
            case EQ -> cmp == 0;
            case NE -> cmp != 0;
            case GT -> cmp > 0;
            case LT -> cmp < 0;
            case GE -> cmp >= 0;
            case LE -> cmp <= 0;
        };
    }

    /**
     * Projette les champs sélectionnés en JSON compact à clés plates, ex. {@code {"a":1,"b.c":"x"}}.
     *
     * @return le JSON projeté, ou {@code null} si aucun select ou nœud absent (texte)
     */
    public String project(Query query, JsonNode node) {
        List<FieldRef> fields = query.selectedFields();
        if (fields.isEmpty() || node == null) {
            return null;
        }
        ObjectNode out = objectMapper.createObjectNode();
        for (FieldRef field : fields) {
            JsonNode value = FieldAccessor.resolve(node, field.path());
            if (value == null || value.isMissingNode()) {
                out.putNull(field.dotted());
            } else {
                out.set(field.dotted(), value);
            }
        }
        return objectMapper.writeValueAsString(out);
    }
}
