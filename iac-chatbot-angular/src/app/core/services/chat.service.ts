import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import {
  ChatRequest,
  ChatResponse,
  InfrastructureRequest,
  TargetPlatform,
} from '../../shared/models/chat.model';

/**
 * Service de communication avec le chatbot IaC.
 * Le token JWT est ajouté automatiquement par l'intercepteur.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/chatbot`;

  /** Envoie un message en langage naturel : POST /api/chatbot/process */
  sendMessage(message: string, targetPlatform: TargetPlatform): Observable<ChatResponse> {
    const body: ChatRequest = { message, targetPlatform };
    return this.http.post<ChatResponse>(`${this.baseUrl}/process`, body);
  }

  /** Historique des requêtes : GET /api/chatbot/requests (JWT requis) */
  getHistory(): Observable<InfrastructureRequest[]> {
    return this.http.get<InfrastructureRequest[]>(`${this.baseUrl}/requests`);
  }

  /** Health check du backend : GET /api/chatbot/health (endpoint public, renvoie du texte) */
  health(): Observable<string> {
    return this.http.get(`${this.baseUrl}/health`, { responseType: 'text' });
  }
}
