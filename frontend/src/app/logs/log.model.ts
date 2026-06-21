/** Format d'une source de logs (aligné sur l'enum backend). */
export type LogFormat = 'TEXT' | 'JSON';

/** Une source de logs configurée côté backend. */
export interface LogSource {
  name: string;
  format: LogFormat;
}

/** Une entrée de log normalisée. */
export interface LogEntry {
  timestamp: string | null;
  epochMillis: number | null;
  level: string | null;
  message: string;
  raw: string;
  source: string;
  lineNumber: number;
}

/** Une tranche de l'histogramme temporel. */
export interface HistogramBucket {
  startMillis: number;
  count: number;
}

/** Histogramme temporel : nombre d'entrées par tranche de largeur fixe. */
export interface Histogram {
  intervalMillis: number;
  buckets: HistogramBucket[];
}

/** Une page d'entrées, avec la pagination calculée par le backend. */
export interface LogPage {
  content: LogEntry[];
  source: string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
