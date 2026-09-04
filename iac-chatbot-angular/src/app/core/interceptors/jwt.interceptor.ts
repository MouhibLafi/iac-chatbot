import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP fonctionnel :
 * 1. Ajoute l'en-tête "Authorization: Bearer <token>" à toutes les requêtes
 *    si un token JWT est disponible.
 * 2. En cas de réponse 401/403 (session expirée), déconnecte l'utilisateur
 *    et le redirige vers la page de connexion.
 *    Les endpoints /api/auth/** sont exclus de cette déconnexion automatique
 *    (un 401 sur /login signifie simplement « identifiants invalides »).
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthEndpoint = req.url.includes('/auth/');
      if ((error.status === 401 || error.status === 403) && !isAuthEndpoint) {
        // Session expirée ou accès interdit → déconnexion + redirection login
        authService.logout();
      }
      return throwError(() => error);
    }),
  );
};
