import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { Histogram, LogEntry, LogPage, LogSource } from './log.model';
import { LogService } from './log.service';

/**
 * Écran principal : barre de recherche (façon Splunk) + liste paginée des logs.
 * La pagination et le filtrage sont délégués au backend.
 */
@Component({
  selector: 'app-logs',
  imports: [],
  templateUrl: './logs.html',
  styleUrl: './logs.scss',
})
export class Logs implements OnInit {
  private readonly logService = inject(LogService);

  protected readonly sources = signal<LogSource[]>([]);
  protected readonly selectedSource = signal('');
  protected readonly search = signal('');
  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly page = signal(0);
  protected readonly size = signal(100);

  protected readonly result = signal<LogPage | null>(null);
  protected readonly histogram = signal<Histogram | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Géométrie du graphique en barres (unités SVG), dérivée de l'histogramme. */
  protected readonly chart = computed(() => {
    const h = this.histogram();
    if (!h || h.buckets.length === 0) {
      return null;
    }
    const width = 1000;
    const height = 80;
    const barWidth = width / h.buckets.length;
    const max = Math.max(1, ...h.buckets.map((b) => b.count));
    const bars = h.buckets.map((b, i) => {
      const barHeight = (b.count / max) * height;
      return {
        x: i * barWidth,
        y: height - barHeight,
        w: Math.max(barWidth - 1, 0.5),
        h: barHeight,
        count: b.count,
        label: this.formatInstant(b.startMillis),
      };
    });
    return {
      width,
      height,
      bars,
      max,
      total: h.buckets.reduce((sum, b) => sum + b.count, 0),
      first: this.formatInstant(h.buckets[0].startMillis),
      last: this.formatInstant(h.buckets[h.buckets.length - 1].startMillis),
    };
  });

  /** Numéros de ligne des entrées dépliées (affichage de la ligne brute). */
  private readonly expanded = signal<Set<number>>(new Set());

  /** Numéro humain de la page courante (1-based) pour l'affichage. */
  protected readonly displayPage = computed(() => {
    const total = this.result()?.totalPages ?? 0;
    return total === 0 ? 0 : this.page() + 1;
  });

  protected readonly canPrev = computed(() => this.page() > 0);
  protected readonly canNext = computed(() => {
    const total = this.result()?.totalPages ?? 0;
    return this.page() + 1 < total;
  });

  private readonly searchInput$ = new Subject<string>();

  ngOnInit(): void {
    // Recherche débouncée : on attend une pause de frappe avant d'interroger le backend.
    this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((value) => {
        this.search.set(value);
        this.page.set(0);
        this.fetch();
      });

    this.logService.getSources().subscribe({
      next: (sources) => {
        this.sources.set(sources);
        if (sources.length > 0) {
          this.selectedSource.set(sources[0].name);
          this.fetch();
        }
      },
      error: () => this.error.set('Impossible de charger la liste des sources.'),
    });
  }

  protected onSourceChange(name: string): void {
    this.selectedSource.set(name);
    this.page.set(0);
    this.fetch();
  }

  protected onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  protected onFromChange(value: string): void {
    this.from.set(value);
    this.page.set(0);
    this.fetch();
  }

  protected onToChange(value: string): void {
    this.to.set(value);
    this.page.set(0);
    this.fetch();
  }

  protected prevPage(): void {
    if (this.canPrev()) {
      this.page.update((p) => p - 1);
      this.fetch();
    }
  }

  protected nextPage(): void {
    if (this.canNext()) {
      this.page.update((p) => p + 1);
      this.fetch();
    }
  }

  protected toggle(entry: LogEntry): void {
    this.expanded.update((set) => {
      const next = new Set(set);
      next.has(entry.lineNumber) ? next.delete(entry.lineNumber) : next.add(entry.lineNumber);
      return next;
    });
  }

  protected isExpanded(entry: LogEntry): boolean {
    return this.expanded().has(entry.lineNumber);
  }

  protected levelClass(level: string | null): string {
    return level ? `level level--${level.toUpperCase()}` : 'level';
  }

  private fetch(): void {
    const source = this.selectedSource();
    if (!source) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.expanded.set(new Set());
    this.logService
      .getLogs(source, this.search(), this.from(), this.to(), this.page(), this.size())
      .subscribe({
        next: (page) => {
          this.result.set(page);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.toErrorMessage(err));
          this.result.set(null);
          this.loading.set(false);
        },
      });
    this.logService.getHistogram(source, this.search(), this.from(), this.to()).subscribe({
      next: (histogram) => this.histogram.set(histogram),
      error: () => this.histogram.set(null),
    });
  }

  /** Remonte le message d'erreur du backend (ProblemDetail.detail), ex. erreur de syntaxe de requête. */
  private toErrorMessage(err: HttpErrorResponse): string {
    const detail = err.error?.detail;
    return typeof detail === 'string' ? detail : 'Erreur lors du chargement des logs.';
  }

  /** Formate un epoch millis en "MM-dd HH:mm:ss" (UTC), cohérent avec l'affichage des dates. */
  private formatInstant(millis: number): string {
    return new Date(millis).toISOString().slice(5, 19).replace('T', ' ');
  }
}
