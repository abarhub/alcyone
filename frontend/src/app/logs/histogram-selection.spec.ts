import { describe, expect, it } from 'vitest';
import { selectionToRange } from './histogram-selection';
import { HistogramBucket } from './log.model';

describe('selectionToRange', () => {
  const interval = 1000;
  // 10 tranches de 1s à partir de t=0, largeur de repère = 1000 (donc 100 / tranche).
  const buckets: HistogramBucket[] = Array.from({ length: 10 }, (_, i) => ({
    startMillis: i * interval,
    count: i,
  }));

  it('un clic sélectionne une seule tranche', () => {
    // x ≈ 250 -> tranche index 2 (250 / 100)
    const range = selectionToRange(250, 250, 1000, buckets, interval);
    expect(range).toEqual({ fromMillis: 2000, toMillis: 3000 });
  });

  it('un glisser couvre toutes les tranches touchées', () => {
    // de la tranche 2 (x=250) à la tranche 6 (x=650)
    const range = selectionToRange(250, 650, 1000, buckets, interval);
    expect(range).toEqual({ fromMillis: 2000, toMillis: 7000 });
  });

  it('le sens du glisser est indifférent', () => {
    expect(selectionToRange(650, 250, 1000, buckets, interval)).toEqual(
      selectionToRange(250, 650, 1000, buckets, interval),
    );
  });

  it('borne les coordonnées hors limites à la dernière tranche', () => {
    // x = 1000 tomberait sur l'index 10 (hors plage) -> borné à 9
    const range = selectionToRange(0, 1000, 1000, buckets, interval);
    expect(range).toEqual({ fromMillis: 0, toMillis: 10_000 });
  });

  it('renvoie null sans tranche ou avec une largeur invalide', () => {
    expect(selectionToRange(0, 100, 1000, [], interval)).toBeNull();
    expect(selectionToRange(0, 100, 0, buckets, interval)).toBeNull();
  });
});
