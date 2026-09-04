import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { HeaderComponent } from '../../shared/header/header.component';
import { AdminService } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { ChatService } from '../../core/services/chat.service';
import { HistoryService } from '../../core/services/history.service';
import { QuotaRequest, UserDto } from '../../shared/models/user.model';
import { InfrastructureRequest } from '../../shared/models/chat.model';
import {
  formatDate,
  paramBadges,
  parseExtractedParams,
  statusBadgeClass,
} from '../../shared/utils/request.utils';

/** Onglets du dashboard d'administration */
type AdminTab = 'users' | 'pending' | 'all';

/**
 * Dashboard d'administration (réservé au rôle ADMIN) avec onglets :
 * - Utilisateurs : statistiques, tableau, suppression, édition des quotas
 * - Validations : demandes PENDING_APPROVAL à approuver / rejeter
 * - Toutes les demandes : historique global avec filtres
 */
@Component({
  selector: 'app-admin',
  imports: [FormsModule, HeaderComponent],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly chatService = inject(ChatService);
  private readonly historyService = inject(HistoryService);
  protected readonly authService = inject(AuthService);

  /** Onglet actif */
  protected readonly activeTab = signal<AdminTab>('users');

  /** Message d'alerte (succès / erreur) commun aux onglets */
  protected readonly alertMessage = signal('');
  protected readonly alertType = signal<'error' | 'success'>('error');

  // --- Onglet Utilisateurs ---
  protected readonly users = signal<UserDto[]>([]);
  protected readonly loadingUsers = signal(true);
  protected readonly totalUsers = signal<number | null>(null);
  protected readonly totalRequests = signal<number | null>(null);
  protected readonly backendOnline = signal<boolean | null>(null);

  /** Formulaires de quotas par utilisateur + éditeur ouvert */
  protected quotaForms: Record<number, QuotaRequest> = {};
  protected readonly quotaEditorOpen = signal<number | null>(null);

  // --- Onglet Validations ---
  protected readonly pendingRequests = signal<InfrastructureRequest[]>([]);
  protected readonly loadingPending = signal(false);
  protected readonly expandedPendingId = signal<number | null>(null);
  private pendingLoaded = false;

  // --- Onglet Toutes les demandes ---
  protected readonly allRequests = signal<InfrastructureRequest[]>([]);
  protected readonly loadingAll = signal(false);
  protected allStatusFilter = '';
  protected allPlatformFilter = '';
  private allLoaded = false;

  /** Options des filtres de l'onglet « Toutes les demandes » */
  protected readonly statusOptions = [
    { value: '', label: 'Tous les statuts' },
    { value: 'CODE_GENERATED', label: 'Code généré' },
    { value: 'completed', label: 'Terminé' },
    { value: 'SUCCESS', label: 'Déployé' },
    { value: 'RUNNING', label: 'En cours' },
    { value: 'FAILED', label: 'Échoué' },
    { value: 'CANCELLED', label: 'Annulé' },
    { value: 'PENDING_APPROVAL', label: 'En attente de validation' },
  ];
  protected readonly platformOptions = [
    { value: '', label: 'Toutes les plateformes' },
    { value: 'vsphere', label: 'VMware vSphere' },
    { value: 'openshift', label: 'OpenShift / KubeVirt' },
  ];

  // Utilitaires partagés exposés au template
  protected readonly statusBadgeClass = statusBadgeClass;
  protected readonly formatDate = formatDate;
  protected readonly parseExtractedParams = parseExtractedParams;
  protected readonly paramBadges = paramBadges;

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadUsers();
  }

  /** Bascule d'onglet avec chargement paresseux des données */
  switchTab(tab: AdminTab): void {
    this.activeTab.set(tab);
    if (tab === 'pending' && !this.pendingLoaded) {
      this.loadPending();
    }
    if (tab === 'all' && !this.allLoaded) {
      this.loadAllRequests();
    }
  }

  // ------------------------------------------------------------------
  // Statistiques
  // ------------------------------------------------------------------

  /** Charge les statistiques : nombre d'utilisateurs, de requêtes et état du backend */
  loadDashboardData(): void {
    this.adminService.getUsers().subscribe({
      next: (users) => this.totalUsers.set(users.length),
      error: () => this.totalUsers.set(null),
    });

    this.chatService.getHistory().subscribe({
      next: (requests) => this.totalRequests.set(requests.length),
      error: () => this.totalRequests.set(null),
    });

    this.chatService.health().subscribe({
      next: () => this.backendOnline.set(true),
      error: () => this.backendOnline.set(false),
    });
  }

  // ------------------------------------------------------------------
  // Onglet Utilisateurs
  // ------------------------------------------------------------------

  /** Charge la liste des utilisateurs */
  loadUsers(): void {
    this.loadingUsers.set(true);
    this.adminService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loadingUsers.set(false);
      },
      error: () => {
        this.loadingUsers.set(false);
        this.showAlert('Erreur lors du chargement des utilisateurs', 'error');
      },
    });
  }

  /** Supprime un utilisateur après confirmation */
  deleteUser(user: UserDto): void {
    if (!confirm(`Êtes-vous sûr de vouloir supprimer l'utilisateur "${user.username}" ?`)) {
      return;
    }
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.showAlert(`Utilisateur "${user.username}" supprimé avec succès`, 'success');
        // Rechargement du tableau et des statistiques
        this.loadUsers();
        this.loadDashboardData();
      },
      error: () => {
        this.showAlert(`Erreur lors de la suppression de l'utilisateur "${user.username}"`, 'error');
      },
    });
  }

  /** Ouvre / ferme l'éditeur de quotas d'un utilisateur (pré-rempli) */
  toggleQuotaEditor(user: UserDto): void {
    if (this.quotaEditorOpen() === user.id) {
      this.quotaEditorOpen.set(null);
      return;
    }
    this.quotaForms[user.id] = {
      quotaCpu: user.quotaCpu ?? 0,
      quotaRam: user.quotaRam ?? 0,
      quotaStorage: user.quotaStorage ?? 0,
    };
    this.quotaEditorOpen.set(user.id);
  }

  /** Enregistre les quotas modifiés : PUT /api/users/{id}/quota */
  saveQuota(user: UserDto): void {
    const quotas = this.quotaForms[user.id];
    if (!quotas) {
      return;
    }
    this.adminService.updateQuota(user.id, quotas).subscribe({
      next: (response) => {
        this.showAlert(response.message || `Quotas de "${user.username}" mis à jour`, 'success');
        this.quotaEditorOpen.set(null);
        this.loadUsers();
      },
      error: (err) => {
        this.showAlert(
          err.error?.message || `Erreur lors de la mise à jour des quotas de "${user.username}"`,
          'error',
        );
      },
    });
  }

  // ------------------------------------------------------------------
  // Onglet Validations (demandes PENDING_APPROVAL)
  // ------------------------------------------------------------------

  /** Charge les demandes en attente d'approbation */
  loadPending(): void {
    this.loadingPending.set(true);
    this.adminService.getPending().subscribe({
      next: (requests) => {
        this.pendingRequests.set(requests);
        this.loadingPending.set(false);
        this.pendingLoaded = true;
      },
      error: () => {
        this.loadingPending.set(false);
        this.showAlert('Erreur lors du chargement des demandes en attente', 'error');
      },
    });
  }

  /** Replie / déplie le code généré d'une demande en attente */
  togglePendingDetail(id: number): void {
    this.expandedPendingId.update((current) => (current === id ? null : id));
  }

  /** Approuve une demande : POST /api/admin/approve/{id} */
  approve(request: InfrastructureRequest): void {
    if (!confirm(`Approuver la demande #${request.id} ?`)) {
      return;
    }
    this.adminService.approve(request.id).subscribe({
      next: (response) => {
        this.showAlert(response.message || `Demande #${request.id} approuvée`, 'success');
        this.loadPending();
      },
      error: (err) => {
        this.showAlert(err.error?.message || `Erreur lors de l'approbation de la demande #${request.id}`, 'error');
      },
    });
  }

  /** Rejette une demande avec motif optionnel : POST /api/admin/reject/{id} */
  reject(request: InfrastructureRequest): void {
    const reason = prompt('Raison du rejet (optionnel) :');
    if (reason === null) {
      return; // Annulation du prompt
    }
    this.adminService.reject(request.id, reason.trim() || undefined).subscribe({
      next: (response) => {
        this.showAlert(response.message || `Demande #${request.id} rejetée`, 'success');
        this.loadPending();
      },
      error: (err) => {
        this.showAlert(err.error?.message || `Erreur lors du rejet de la demande #${request.id}`, 'error');
      },
    });
  }

  // ------------------------------------------------------------------
  // Onglet Toutes les demandes (GET /api/history/all)
  // ------------------------------------------------------------------

  /** Charge l'historique global avec les filtres courants */
  loadAllRequests(): void {
    this.loadingAll.set(true);
    this.historyService
      .getAllHistory(this.allStatusFilter || undefined, this.allPlatformFilter || undefined)
      .subscribe({
        next: (requests) => {
          this.allRequests.set(requests);
          this.loadingAll.set(false);
          this.allLoaded = true;
        },
        error: () => {
          this.loadingAll.set(false);
          this.showAlert('Erreur lors du chargement de toutes les demandes', 'error');
        },
      });
  }

  /** Tronque un texte pour l'affichage dans les tableaux */
  truncate(text: string, length = 60): string {
    return text.length > length ? `${text.substring(0, length)}...` : text;
  }

  // ------------------------------------------------------------------
  // Helpers d'affichage
  // ------------------------------------------------------------------

  /** Libellé lisible du rôle (sans préfixe ROLE_ éventuel) */
  roleLabel(role: string): string {
    return role.replace('ROLE_', '');
  }

  /** Vrai si le rôle est administrateur */
  isAdminRole(role: string): boolean {
    return role === 'ADMIN' || role === 'ROLE_ADMIN';
  }

  private showAlert(message: string, type: 'error' | 'success'): void {
    this.alertMessage.set(message);
    this.alertType.set(type);
    // Masquage automatique après 5 secondes
    setTimeout(() => this.alertMessage.set(''), 5000);
  }
}
