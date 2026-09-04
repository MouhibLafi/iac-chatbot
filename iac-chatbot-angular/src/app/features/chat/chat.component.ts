import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { HeaderComponent } from '../../shared/header/header.component';
import { ChatService } from '../../core/services/chat.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { DeployService } from '../../core/services/deploy.service';
import { ProgressEvent } from '../../shared/models/progress.model';
import {
  ChatMessage,
  ChatResponse,
  ExtractedParameters,
  TargetPlatform,
} from '../../shared/models/chat.model';

/** Badge d'affichage d'un paramètre extrait */
interface ParamBadge {
  label: string;
  value: string;
}

/**
 * Interface de chat type ChatGPT :
 * - zone de messages scrollable avec bulles utilisateur / bot
 * - sélecteur de plateforme cible (Auto / VMware vSphere / OpenShift)
 * - envoi par Entrée (Maj+Entrée pour un saut de ligne)
 * - badges des paramètres extraits, bloc de code avec bouton Copier
 * - indicateur de chargement et gestion du statut "clarification_needed"
 */
@Component({
  selector: 'app-chat',
  imports: [FormsModule, HeaderComponent],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements OnInit, OnDestroy {
  private readonly chatService = inject(ChatService);
  protected readonly wsService = inject(WebSocketService);
  private readonly deployService = inject(DeployService);

  @ViewChild('messagesContainer') private messagesContainer?: ElementRef<HTMLDivElement>;

  /** Fil de messages affiché dans la zone de chat */
  protected readonly messages = signal<ChatMessage[]>([]);

  /** Message saisi par l'utilisateur */
  protected userMessage = '';

  /** Plateforme cible sélectionnée */
  protected targetPlatform: TargetPlatform = 'auto';

  /** Vrai pendant l'attente de la réponse du backend */
  protected readonly loading = signal(false);

  /** État de connexion du backend : null = vérification en cours */
  protected readonly backendOnline = signal<boolean | null>(null);

  /** Dernier événement de progression reçu via WebSocket (null = pas de progression) */
  protected readonly progress = signal<ProgressEvent | null>(null);

  private progressSubscription?: Subscription;
  private healthCheckInterval?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    // Connexion WebSocket STOMP pour suivre la progression en temps réel
    this.wsService.connect();
    this.progressSubscription = this.wsService.progress$.subscribe((event) => {
      // On n'affiche la progression que pendant l'attente d'une réponse
      if (this.loading()) {
        this.progress.set(event);
      }
      // Progression temps réel d'un déploiement en cours (même topic, requestId réel)
      this.updateDeployProgress(event);
    });

    this.checkBackendStatus();
    // Vérification périodique du backend toutes les 30 secondes
    this.healthCheckInterval = setInterval(() => this.checkBackendStatus(), 30000);
  }

  ngOnDestroy(): void {
    this.progressSubscription?.unsubscribe();
    // Fermeture de la connexion WebSocket à la sortie de la page
    this.wsService.disconnect();
    if (this.healthCheckInterval) {
      clearInterval(this.healthCheckInterval);
    }
  }

  /** Envoi du message au backend */
  sendMessage(): void {
    const message = this.userMessage.trim();
    if (!message || this.loading()) {
      return;
    }

    // Bulle utilisateur + purge du champ de saisie
    this.messages.update((msgs) => [...msgs, { kind: 'user', text: message }]);
    this.userMessage = '';
    // Réinitialisation de la progression pour ce nouveau traitement
    this.progress.set(null);
    this.loading.set(true);
    this.scrollToBottom();

    this.chatService.sendMessage(message, this.targetPlatform).subscribe({
      next: (response) => {
        this.messages.update((msgs) => [
          ...msgs,
          { kind: 'bot', text: response.message, response },
        ]);
        // Réponse HTTP reçue : on masque la barre de progression
        this.progress.set(null);
        this.loading.set(false);
        this.scrollToBottom();
      },
      error: () => {
        this.messages.update((msgs) => [
          ...msgs,
          {
            kind: 'error',
            text: 'Erreur lors de la communication avec le backend. Vérifiez que le serveur est démarré.',
          },
        ]);
        this.progress.set(null);
        this.loading.set(false);
        this.scrollToBottom();
      },
    });
  }

  /** Entrée envoie le message, Maj+Entrée insère un saut de ligne */
  onTextareaKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  /** Copie le code généré dans le presse-papiers */
  copyCode(message: ChatMessage): void {
    const code = message.response?.generatedCode;
    if (!code) {
      return;
    }
    navigator.clipboard
      .writeText(code)
      .then(() => {
        message.copied = true;
        setTimeout(() => (message.copied = false), 2000);
      })
      .catch(() => alert('Erreur lors de la copie du code'));
  }

  /** Vrai si la réponse demande une clarification (pas de code généré) */
  isClarification(response: ChatResponse): boolean {
    return response.status === 'clarification_needed';
  }

  /** Classe CSS du badge de statut */
  statusBadgeClass(status: string): string {
    if (status === 'clarification_needed') {
      return 'badge-warning';
    }
    if (status === 'failed' || status === 'error' || status === 'FAILED' || status === 'CANCELLED') {
      return 'badge-error';
    }
    if (status === 'RUNNING') {
      return 'badge-warning';
    }
    return 'badge-success';
  }

  // ------------------------------------------------------------------
  // Déploiement, simulé ou réel (POST /api/deploy/{requestId})
  // ------------------------------------------------------------------

  /** Vrai si le code généré peut être déployé (génération réussie, pas de clarification) */
  canDeploy(message: ChatMessage): boolean {
    const response = message.response;
    if (!response?.generatedCode || this.isClarification(response)) {
      return false;
    }
    const status = response.status;
    return status === 'success' || status === 'completed' || status === 'CODE_GENERATED';
  }

  /** Lance le déploiement du code généré (simulation ou réel) */
  deployCode(message: ChatMessage): void {
    const requestId = message.response?.requestId;
    if (!requestId || message.deploying) {
      return;
    }
    this.patchMessage(message, { deploying: true, deployProgress: null });
    this.deployService.deploy(requestId).subscribe({
      next: (result) => {
        this.patchMessage(message, { deploying: false, deployResult: result });
      },
      error: (err) => {
        this.patchMessage(message, { deploying: false });
        // Erreur 403 (quota dépassé) ou autre : message du backend dans une bulle d'erreur
        const text = err.error?.message || 'Erreur lors du déploiement.';
        this.messages.update((msgs) => [...msgs, { kind: 'error', text }]);
        this.scrollToBottom();
      },
    });
  }

  /** Affiche / masque les logs du déploiement (chargement paresseux via GET .../status) */
  toggleLogs(message: ChatMessage): void {
    if (message.showLogs) {
      this.patchMessage(message, { showLogs: false });
      return;
    }
    const requestId = message.response?.requestId;
    if (!requestId) {
      return;
    }
    if (message.deployLogs) {
      // Logs déjà chargés : on se contente de déplier le panneau
      this.patchMessage(message, { showLogs: true });
      return;
    }
    this.deployService.getStatus(requestId).subscribe({
      next: (status) => {
        this.patchMessage(message, { deployLogs: status.logs ?? [], showLogs: true });
      },
      error: () => {
        this.patchMessage(message, { deployLogs: [], showLogs: true });
      },
    });
  }

  /** Annule le déploiement après confirmation */
  cancelDeploy(message: ChatMessage): void {
    const requestId = message.response?.requestId;
    if (!requestId || message.deploying) {
      return;
    }
    if (!confirm('Êtes-vous sûr de vouloir annuler ce déploiement ?')) {
      return;
    }
    this.deployService.cancel(requestId).subscribe({
      next: (result) => {
        this.patchMessage(message, { deployResult: result, deploying: false, showLogs: false });
      },
      error: (err) => {
        const text = err.error?.message || "Erreur lors de l'annulation du déploiement.";
        this.messages.update((msgs) => [...msgs, { kind: 'error', text }]);
        this.scrollToBottom();
      },
    });
  }

  /** Met à jour la progression d'un déploiement en cours à partir d'un événement WebSocket */
  private updateDeployProgress(event: ProgressEvent): void {
    this.messages.update((msgs) =>
      msgs.map((m) =>
        m.deploying && m.response?.requestId === event.requestId
          ? { ...m, deployProgress: event }
          : m,
      ),
    );
  }

  /** Applique un patch immuable sur un message du fil */
  private patchMessage(target: ChatMessage, patch: Partial<ChatMessage>): void {
    this.messages.update((msgs) => msgs.map((m) => (m === target ? { ...m, ...patch } : m)));
  }

  /** Construit la liste des badges de paramètres extraits présents */
  paramBadges(params?: ExtractedParameters): ParamBadge[] {
    if (!params) {
      return [];
    }
    const badges: ParamBadge[] = [];
    if (params.resourceType) badges.push({ label: 'Type', value: params.resourceType });
    if (params.platform) badges.push({ label: 'Plateforme', value: params.platform });
    if (params.osImage) badges.push({ label: 'OS', value: params.osImage });
    if (params.cpu) badges.push({ label: 'CPU', value: String(params.cpu) });
    if (params.ramGb) badges.push({ label: 'RAM', value: `${params.ramGb} Go` });
    if (params.storageGb) badges.push({ label: 'Stockage', value: `${params.storageGb} Go` });
    if (params.replicas) badges.push({ label: 'Replicas', value: String(params.replicas) });
    if (params.containerImage) badges.push({ label: 'Image', value: params.containerImage });
    if (params.network) badges.push({ label: 'Réseau', value: params.network });
    return badges;
  }

  /** Vérifie que le backend répond (endpoint public /api/chatbot/health) */
  private checkBackendStatus(): void {
    this.chatService.health().subscribe({
      next: () => this.backendOnline.set(true),
      error: () => this.backendOnline.set(false),
    });
  }

  /** Fait défiler la zone de messages vers le bas après rendu */
  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.messagesContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
}
