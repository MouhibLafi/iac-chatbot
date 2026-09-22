import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * En-tête partagé : titre, logo, et menu utilisateur déroulant
 * (profil, navigation Chat/Historique/Admin, déconnexion).
 */
@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  protected readonly authService = inject(AuthService);

  /** Menu utilisateur déroulant ouvert ou fermé */
  protected readonly menuOpen = signal(false);

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  /** Déconnexion avec confirmation */
  logout(): void {
    this.closeMenu();
    if (confirm('Êtes-vous sûr de vouloir vous déconnecter ?')) {
      this.authService.logout();
    }
  }

  /** Libellé lisible du rôle (sans préfixe ROLE_ éventuel) */
  roleLabel(role: string): string {
    return role.replace('ROLE_', '');
  }
}
