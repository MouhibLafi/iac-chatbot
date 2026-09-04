import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { HeaderComponent } from '../../shared/header/header.component';
import { HistoryService } from '../../core/services/history.service';
import { InfrastructureRequest } from '../../shared/models/chat.model';
import {
  formatDate,
  paramBadges,
  parseExtractedParams,
  statusBadgeClass,
} from '../../shared/utils/request.utils';

/**
 * Page Historique : liste des demandes de l'utilisateur connecté
 * (GET /api/history), avec filtres par statut et plateforme,
 * et détail dépliable (message complet, paramètres, code généré).
 */
@Component({
  selector: 'app-history',
  imports: [FormsModule, HeaderComponent],
  templateUrl: './history.component.html',
  styleUrl: './history.component.scss',
})
export class HistoryComponent implements OnInit {
  private readonly historyService = inject(HistoryService);

  /** Demandes chargées depuis le backend */
  protected readonly requests = signal<InfrastructureRequest[]>([]);
  protected readonly loading = signal(true);

  /** Filtres (chaîne vide = pas de filtre) */
  protected statusFilter = '';
  protected platformFilter = '';

  /** Identifiant de la ligne dont le détail est déplié (null = tout replié) */
  protected readonly expandedId = signal<number | null>(null);

  /** Identifiant de la demande dont le code vient d'être copié */
  protected readonly copiedId = signal<number | null>(null);

  /** Options des listes déroulantes de filtres */
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
    this.load();
  }

  /** Charge l'historique avec les filtres courants */
  load(): void {
    this.loading.set(true);
    this.historyService
      .getMyHistory(this.statusFilter || undefined, this.platformFilter || undefined)
      .subscribe({
        next: (requests) => {
          this.requests.set(requests);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  /** Replie / déplie le détail d'une demande */
  toggleDetail(id: number): void {
    this.expandedId.update((current) => (current === id ? null : id));
  }

  /** Tronque un texte pour l'affichage dans le tableau */
  truncate(text: string, length = 60): string {
    return text.length > length ? `${text.substring(0, length)}...` : text;
  }

  /** Copie le code généré dans le presse-papiers */
  copyCode(request: InfrastructureRequest): void {
    if (!request.generatedCode) {
      return;
    }
    navigator.clipboard
      .writeText(request.generatedCode)
      .then(() => {
        this.copiedId.set(request.id);
        setTimeout(() => this.copiedId.set(null), 2000);
      })
      .catch(() => alert('Erreur lors de la copie du code'));
  }
}
