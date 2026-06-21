package org.alcyone.alcyone.logs.query;

import java.util.List;

/**
 * Une étape de pipeline, après un {@code |}.
 */
public sealed interface Stage {

    /** {@code | filter <predicat>} : ne garde que les entrées satisfaisant le prédicat. */
    record Filter(BoolExpr predicate) implements Stage {}

    /** {@code | select .a, .b.c} : restreint les champs affichés (sources JSON). */
    record Select(List<FieldRef> fields) implements Stage {}
}
