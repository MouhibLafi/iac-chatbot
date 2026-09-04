import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { DeployResponse, DeployStatusResponse } from '../../shared/models/deploy.model';

/**
 * Service de déploiement du code IaC généré (simulation ou réel).
 * Réservé au propriétaire de la demande ou à un administrateur.
 * La progression est diffusée en temps réel sur /topic/progress (WebSocket).
 */
@Injectable({ providedIn: 'root' })
export class DeployService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/deploy`;

  /** Lance le déploiement : POST /api/deploy/{requestId} */
  deploy(requestId: number): Observable<DeployResponse> {
    return this.http.post<DeployResponse>(`${this.baseUrl}/${requestId}`, {});
  }

  /** Statut et logs du déploiement : GET /api/deploy/{requestId}/status */
  getStatus(requestId: number): Observable<DeployStatusResponse> {
    return this.http.get<DeployStatusResponse>(`${this.baseUrl}/${requestId}/status`);
  }

  /** Annule le déploiement : DELETE /api/deploy/{requestId} */
  cancel(requestId: number): Observable<DeployResponse> {
    return this.http.delete<DeployResponse>(`${this.baseUrl}/${requestId}`);
  }
}
