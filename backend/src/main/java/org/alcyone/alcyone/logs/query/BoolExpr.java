package org.alcyone.alcyone.logs.query;

/**
 * Expression booléenne d'une requête (partie recherche ou prédicat d'un {@code filter}).
 */
public sealed interface BoolExpr {

    /** Conjonction : {@code left AND right}. */
    record And(BoolExpr left, BoolExpr right) implements BoolExpr {}

    /** Disjonction : {@code left OR right}. */
    record Or(BoolExpr left, BoolExpr right) implements BoolExpr {}

    /** Négation : {@code NOT expr}. */
    record Not(BoolExpr expr) implements BoolExpr {}

    /** Critère texte : « la ligne brute contient {@code term} » (insensible à la casse). */
    record TextMatch(String term) implements BoolExpr {}

    /** Comparaison d'un champ à une valeur, ex. {@code .nb > 10}. */
    record Comparison(FieldRef field, Operator op, Value value) implements BoolExpr {}
}
