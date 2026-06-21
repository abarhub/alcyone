import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { LogEntry, LogPage, LogSource } from './log.model';
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
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

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
  }

  /** Remonte le message d'erreur du backend (ProblemDetail.detail), ex. erreur de syntaxe de requête. */
  private toErrorMessage(err: HttpErrorResponse): string {
    const detail = err.error?.detail;
    return typeof detail === 'string' ? detail : 'Erreur lors du chargement des logs.';
  }
}
