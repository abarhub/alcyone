package org.alcyone.alcyone.logs.domain;

import java.util.List;

/**
 * Histogramme temporel : nombre d'entrées par tranche de temps de largeur fixe.
 *
 * @param intervalMillis largeur d'une tranche en millisecondes (0 si aucune donnée)
 * @param buckets        tranches contiguës, dans l'ordre chronologique (zéros inclus)
 */
public record Histogram(long intervalMillis, List<Bucket> buckets) {

    /**
     * Une tranche de l'histogramme.
     *
     * @param startMillis début de la tranche (epoch millis, inclus)
     * @param count       nombre d'entrées dans [startMillis, startMillis + intervalMillis)
     */
    public record Bucket(long startMillis, long count) {}

    public static Histogram empty() {
        return new Histogram(0, List.of());
    }
}
