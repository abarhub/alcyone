import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LogPage, LogSource } from './log.model';

/**
 * Accès à l'API de logs du backend.
 */
@Injectable({ providedIn: 'root' })
export class LogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/logs';

  /** Liste des sources configurées. */
  getSources(): Observable<LogSource[]> {
    return this.http.get<LogSource[]>(`${this.baseUrl}/sources`);
  }

  /** Page de logs d'une source, avec recherche texte optionnelle. */
  getLogs(source: string, search: string, page: number, size: number): Observable<LogPage> {
    let params = new HttpParams()
      .set('source', source)
      .set('page', page)
      .set('size', size);
    if (search.trim()) {
      params = params.set('q', search.trim());
    }
    return this.http.get<LogPage>(this.baseUrl, { params });
  }
}
