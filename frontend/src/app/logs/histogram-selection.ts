import { HistogramBucket } from './log.model';

/** Période issue d'une sélection sur l'histogramme. */
export interface SelectionRange {
  fromMillis: number;
  toMillis: number;
}

/**
 * Traduit une sélection horizontale (coordonnées dans le repère de largeur {@code width}) en une
 * période {@code [fromMillis, toMillis)} couvrant les tranches touchées. Un clic (x1 ≈ x2) donne
 * une seule tranche. Renvoie {@code null} s'il n'y a pas de tranche ou une largeur invalide.
 */
export function selectionToRange(
  x1: number,
  x2: number,
  width: number,
  buckets: HistogramBucket[],
  intervalMillis: number,
): SelectionRange | null {
  if (buckets.length === 0 || width <= 0) {
    return null;
  }
  const barWidth = width / buckets.length;
  const clamp = (i: number) => Math.max(0, Math.min(buckets.length - 1, i));
  const i1 = clamp(Math.floor(Math.min(x1, x2) / barWidth));
  const i2 = clamp(Math.floor(Math.max(x1, x2) / barWidth));
  return {
    fromMillis: buckets[i1].startMillis,
    toMillis: buckets[i2].startMillis + intervalMillis,
  };
}
