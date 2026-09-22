import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * Page d'authentification : connexion et inscription avec onglets.
 * Reproduit le comportement de login.html / auth.js du prototype.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /** Onglet actif : connexion ou inscription */
  protected readonly activeTab = signal<'login' | 'register'>('login');

  /** Message d'alerte (erreur ou succès) */
  protected readonly alertMessage = signal('');
  protected readonly alertType = signal<'error' | 'success'>('error');

  /** Chargement en cours (boutons désactivés + spinner) */
  protected readonly loading = signal(false);

  /** Visibilité des mots de passe */
  protected readonly showLoginPassword = signal(false);
  protected readonly showRegisterPassword = signal(false);

  // Champs du formulaire de connexion
  protected loginUsername = '';
  protected loginPassword = '';

  // Champs du formulaire d'inscription
  protected registerUsername = '';
  protected registerEmail = '';
  protected registerPassword = '';

  constructor() {
    // Si déjà authentifié, on va directement au chat
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/chat']);
    }
  }

  /** Bascule entre les onglets Connexion / Inscription */
  switchTab(tab: 'login' | 'register'): void {
    this.activeTab.set(tab);
    this.hideAlert();
  }

  /** Soumission du formulaire de connexion */
  onLogin(): void {
    const username = this.loginUsername.trim();
    const password = this.loginPassword;

    if (!username || !password) {
      this.showAlert('Veuillez remplir tous les champs', 'error');
      return;
    }

    this.loading.set(true);
    this.authService.login({ username, password }).subscribe({
      next: () => {
        this.showAlert('Connexion réussie ! Redirection...', 'success');
        setTimeout(() => this.router.navigate(['/chat']), 800);
      },
      error: (err) => {
        this.loading.set(false);
        this.showAlert(
          err.error?.message || 'Erreur de connexion. Vérifiez vos identifiants.',
          'error',
        );
      },
    });
  }

  /** Soumission du formulaire d'inscription */
  onRegister(): void {
    const username = this.registerUsername.trim();
    const email = this.registerEmail.trim();
    const password = this.registerPassword;

    // Validations identiques au prototype (auth.js)
    if (!username || !email || !password) {
      this.showAlert('Veuillez remplir tous les champs', 'error');
      return;
    }
    if (username.length < 3) {
      this.showAlert("Le nom d'utilisateur doit contenir au moins 3 caractères", 'error');
      return;
    }
    if (password.length < 6) {
      this.showAlert('Le mot de passe doit contenir au moins 6 caractères', 'error');
      return;
    }
    if (!this.isValidEmail(email)) {
      this.showAlert('Veuillez entrer un email valide', 'error');
      return;
    }

    this.loading.set(true);
    this.authService.register({ username, email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.showAlert('Inscription réussie ! Vous pouvez maintenant vous connecter.', 'success');
        // Réinitialisation puis bascule vers l'onglet connexion avec le nom pré-rempli
        this.registerPassword = '';
        this.registerEmail = '';
        setTimeout(() => {
          this.loginUsername = username;
          this.registerUsername = '';
          this.switchTab('login');
        }, 2000);
      },
      error: (err) => {
        this.loading.set(false);
        this.showAlert(
          err.error?.message || "Erreur lors de l'inscription. Veuillez réessayer.",
          'error',
        );
      },
    });
  }

  private showAlert(message: string, type: 'error' | 'success'): void {
    this.alertMessage.set(message);
    this.alertType.set(type);
  }

  private hideAlert(): void {
    this.alertMessage.set('');
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}
