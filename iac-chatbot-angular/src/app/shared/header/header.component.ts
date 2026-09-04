import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * En-tête partagé : titre, nom d'utilisateur, badge de rôle,
 * navigation (Chat, Admin si administrateur) et bouton de déconnexion.
 */
@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  protected readonly authService = inject(AuthService);

  /** Déconnexion avec confirmation */
  logout(): void {
    if (confirm('Êtes-vous sûr de vouloir vous déconnecter ?')) {
      this.authService.logout();
    }
  }

  /** Libellé lisible du rôle (sans préfixe ROLE_ éventuel) */
  roleLabel(role: string): string {
    return role.replace('ROLE_', '');
  }
}
