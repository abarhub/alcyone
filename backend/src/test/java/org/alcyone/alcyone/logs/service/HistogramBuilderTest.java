package org.alcyone.alcyone.logs.service;

import org.alcyone.alcyone.logs.domain.Histogram;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistogramBuilderTest {

    private static long ms(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }

    @Test
    void emptyInputGivesEmptyHistogram() {
        Histogram h = HistogramBuilder.build(List.of());
        assertThat(h.intervalMillis()).isZero();
        assertThat(h.buckets()).isEmpty();
    }

    @Test
    void singleTimestampGivesOneBucket() {
        Histogram h = HistogramBuilder.build(List.of(ms("2026-06-20T08:00:00Z")));
        assertThat(h.buckets()).hasSize(1);
        assertThat(h.buckets().getFirst().count()).isEqualTo(1);
    }

    @Test
    void contiguousBucketsWithZerosAndCorrectCounts() {
        // Étendue d'une heure -> tranche choisie = 60s (3600s / 50 ≈ 72 -> palier 300s? vérifions le total).
        List<Long> epochs = List.of(
                ms("2026-06-20T08:00:00Z"),
                ms("2026-06-20T08:00:30Z"),
                ms("2026-06-20T09:00:00Z"));
        Histogram h = HistogramBuilder.build(epochs);

        // Tranches contiguës, dans l'ordre, total des counts = nombre d'entrées.
        long total = h.buckets().stream().mapToLong(Histogram.Bucket::count).sum();
        assertThat(total).isEqualTo(3);
        assertThat(h.buckets()).isSortedAccordingTo((a, b) -> Long.compare(a.startMillis(), b.startMillis()));
        // tranches contiguës d'intervalle constant
        for (int i = 1; i < h.buckets().size(); i++) {
            long delta = h.buckets().get(i).startMillis() - h.buckets().get(i - 1).startMillis();
            assertThat(delta).isEqualTo(h.intervalMillis());
        }
        // la première et la dernière entrée tombent dans des tranches distinctes
        assertThat(h.buckets().getFirst().count()).isEqualTo(2);
        assertThat(h.buckets().getLast().count()).isEqualTo(1);
    }
}
