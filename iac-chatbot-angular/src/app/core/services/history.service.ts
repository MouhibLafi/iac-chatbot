import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { InfrastructureRequest } from '../../shared/models/chat.model';

/**
 * Service d'historique des demandes d'infrastructure.
 * - getMyHistory : demandes de l'utilisateur connecté
 * - getAllHistory : toutes les demandes (ADMIN uniquement)
 * Filtres optionnels par statut et plateforme.
 */
@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/history`;

  /** Historique de l'utilisateur connecté : GET /api/history?status=&platform= */
  getMyHistory(status?: string, platform?: string): Observable<InfrastructureRequest[]> {
    return this.http.get<InfrastructureRequest[]>(this.baseUrl, {
      params: this.buildParams(status, platform),
    });
  }

  /** Historique de toutes les demandes (ADMIN) : GET /api/history/all */
  getAllHistory(status?: string, platform?: string): Observable<InfrastructureRequest[]> {
    return this.http.get<InfrastructureRequest[]>(`${this.baseUrl}/all`, {
      params: this.buildParams(status, platform),
    });
  }

  /** Construit les paramètres de requête en ignorant les filtres vides */
  private buildParams(status?: string, platform?: string): HttpParams {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (platform) {
      params = params.set('platform', platform);
    }
    return params;
  }
}
