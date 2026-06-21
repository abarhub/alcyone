package org.alcyone.alcyone.logs.service;

import org.alcyone.alcyone.logs.domain.Histogram;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit un {@link Histogram} à partir d'horodatages (epoch millis), en choisissant
 * automatiquement une largeur de tranche « ronde » pour viser ~{@value #TARGET_BUCKETS} barres.
 */
public final class HistogramBuilder {

    /** Nombre de tranches visé pour déterminer la largeur automatique. */
    private static final int TARGET_BUCKETS = 50;

    /** Largeurs « rondes » candidates, en millisecondes (1s → 1an). */
    private static final long[] LADDER = {
            1_000L, 5_000L, 10_000L, 30_000L,
            60_000L, 300_000L, 600_000L, 1_800_000L,
            3_600_000L, 10_800_000L, 21_600_000L, 43_200_000L,
            86_400_000L, 604_800_000L, 2_592_000_000L, 31_536_000_000L
    };

    private static final long DAY_MILLIS = 86_400_000L;

    private HistogramBuilder() {
    }

    public static Histogram build(List<Long> epochMillis) {
        if (epochMillis.isEmpty()) {
            return Histogram.empty();
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (long e : epochMillis) {
            min = Math.min(min, e);
            max = Math.max(max, e);
        }

        long interval = chooseInterval(max - min);
        long first = Math.floorDiv(min, interval) * interval;
        long last = Math.floorDiv(max, interval) * interval;
        int count = (int) ((last - first) / interval) + 1;

        long[] counts = new long[count];
        for (long e : epochMillis) {
            int index = (int) ((Math.floorDiv(e, interval) * interval - first) / interval);
            counts[index]++;
        }

        List<Histogram.Bucket> buckets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            buckets.add(new Histogram.Bucket(first + (long) i * interval, counts[i]));
        }
        return new Histogram(interval, buckets);
    }

    private static long chooseInterval(long span) {
        if (span <= 0) {
            return LADDER[0];
        }
        long raw = span / TARGET_BUCKETS;
        for (long candidate : LADDER) {
            if (candidate >= raw) {
                return candidate;
            }
        }
        // Étendue plus large que l'échelle : arrondir au multiple de jour supérieur.
        long days = (raw + DAY_MILLIS - 1) / DAY_MILLIS;
        return days * DAY_MILLIS;
    }
}
