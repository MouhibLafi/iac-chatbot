import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { WebSocketService } from './websocket.service';
import {
  CurrentUser,
  LoginRequest,
  LoginResponse,
  MessageResponse,
  RegisterRequest,
} from '../../shared/models/auth.model';

/**
 * Service d'authentification JWT.
 * Gère la connexion, l'inscription, la déconnexion et l'état de session
 * (token JWT + utilisateur courant persistés dans le localStorage).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly webSocketService = inject(WebSocketService);

  // Clés de stockage local (comme dans le prototype JS)
  private readonly TOKEN_KEY = 'iac_token';
  private readonly USER_KEY = 'iac_user';

  /** Utilisateur courant (signal en lecture seule exposé publiquement) */
  private readonly userSignal = signal<CurrentUser | null>(this.readStoredUser());
  readonly currentUser = this.userSignal.asReadonly();

  /** Vrai si un utilisateur est connecté avec un token JWT non expiré */
  readonly isAuthenticated = computed(() => {
    const token = this.getToken();
    return this.userSignal() !== null && token !== null && !this.isTokenExpired(token);
  });

  /** Vrai si l'utilisateur courant possède le rôle administrateur */
  readonly isAdmin = computed(() => {
    const user = this.userSignal();
    if (!user) {
      return false;
    }
    // Le backend renvoie le nom de l'enum ("ADMIN"), on tolère aussi "ROLE_ADMIN"
    return user.role === 'ADMIN' || user.role === 'ROLE_ADMIN';
  });

  /** Connexion : POST /api/auth/login puis stockage du token et de l'utilisateur */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, credentials).pipe(
      tap((response) => {
        localStorage.setItem(this.TOKEN_KEY, response.token);
        const user: CurrentUser = {
          id: response.id,
          username: response.username,
          email: response.email,
          role: response.role,
        };
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
        this.userSignal.set(user);
        // Ouverture de la connexion WebSocket pour les notifications temps réel
        this.webSocketService.connect();
      }),
    );
  }

  /** Inscription : POST /api/auth/register */
  register(data: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/register`, data);
  }

  /** Déconnexion : purge du stockage local et redirection vers /login */
  logout(): void {
    // Fermeture de la connexion WebSocket avant la purge de la session
    this.webSocketService.disconnect();
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.userSignal.set(null);
    this.router.navigate(['/login']);
  }

  /** Retourne le token JWT stocké (ou null si absent) */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /** Lit l'utilisateur stocké dans le localStorage au démarrage */
  private readStoredUser(): CurrentUser | null {
    const raw = localStorage.getItem(this.USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as CurrentUser;
    } catch {
      // Données corrompues : on nettoie
      localStorage.removeItem(this.USER_KEY);
      return null;
    }
  }

  /** Vérifie l'expiration du token JWT en décodant son payload */
  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }
}
