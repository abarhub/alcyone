package org.alcyone.alcyone.logs.query;

import java.util.List;

/**
 * Requête compilée : une expression de recherche optionnelle suivie d'étapes de pipeline.
 *
 * @param search expression de recherche de tête, ou {@code null} si absente
 * @param stages étapes de pipeline ({@code filter}, {@code select}), dans l'ordre
 */
public record Query(BoolExpr search, List<Stage> stages) {

    /** @return les champs sélectionnés (union de tous les {@code select}), jamais {@code null}. */
    public List<FieldRef> selectedFields() {
        return stages.stream()
                .filter(s -> s instanceof Stage.Select)
                .map(s -> ((Stage.Select) s).fields())
                .flatMap(List::stream)
                .toList();
    }

    /** @return les prédicats de tous les {@code filter}, plus la recherche de tête si présente. */
    public List<BoolExpr> predicates() {
        var predicates = new java.util.ArrayList<BoolExpr>();
        if (search != null) {
            predicates.add(search);
        }
        for (Stage stage : stages) {
            if (stage instanceof Stage.Filter filter) {
                predicates.add(filter.predicate());
            }
        }
        return predicates;
    }
}
