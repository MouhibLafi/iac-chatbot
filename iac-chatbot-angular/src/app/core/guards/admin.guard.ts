import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Guard fonctionnel : autorise l'accès uniquement aux administrateurs.
 * - Non authentifié → redirection vers /login
 * - Authentifié mais pas ADMIN → redirection vers /chat
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  if (!authService.isAdmin()) {
    return router.createUrlTree(['/chat']);
  }
  return true;
};
