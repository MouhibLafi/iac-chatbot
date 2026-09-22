import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { QuotaRequest, UserDto } from '../../shared/models/user.model';
import { MessageResponse } from '../../shared/models/auth.model';
import { InfrastructureRequest } from '../../shared/models/chat.model';
import { DeployResponse } from '../../shared/models/deploy.model';

/**
 * Service d'administration — réservé aux utilisateurs avec le rôle ADMIN.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  /** Liste des utilisateurs : GET /api/users (ADMIN uniquement) */
  getUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${API_BASE_URL}/users`);
  }

  /** Suppression d'un utilisateur : DELETE /api/users/{id} (ADMIN uniquement) */
  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/users/${id}`);
  }

  /** Modification des quotas d'un utilisateur : PUT /api/users/{id}/quota */
  updateQuota(userId: number, quotas: QuotaRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${API_BASE_URL}/users/${userId}/quota`, quotas);
  }

  /** Demandes en attente d'approbation : GET /api/admin/pending */
  getPending(): Observable<InfrastructureRequest[]> {
    return this.http.get<InfrastructureRequest[]>(`${API_BASE_URL}/admin/pending`);
  }

  /** Approuve une demande : POST /api/admin/approve/{id} */
  approve(id: number): Observable<DeployResponse> {
    return this.http.post<DeployResponse>(`${API_BASE_URL}/admin/approve/${id}`, {});
  }

  /** Rejette une demande avec motif optionnel : POST /api/admin/reject/{id} */
  reject(id: number, reason?: string): Observable<DeployResponse> {
    const body = reason ? { reason } : {};
    return this.http.post<DeployResponse>(`${API_BASE_URL}/admin/reject/${id}`, body);
  }
}
