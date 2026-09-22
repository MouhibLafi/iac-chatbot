import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

import { ProgressEvent } from '../../shared/models/progress.model';

/** Endpoint WebSocket (relatif — passe par le proxy Angular avec ws:true) */
const WS_ENDPOINT = '/ws';

/** Topic STOMP de diffusion des événements de progression */
const PROGRESS_TOPIC = '/topic/progress';

/**
 * Service WebSocket STOMP (via SockJS) pour les notifications temps réel.
 *
 * - connect() : ouvre la connexion (idempotent) et s'abonne au topic de progression
 * - disconnect() : ferme proprement la connexion
 * - connected : signal indiquant l'état de la connexion
 * - progress$ : flux des ProgressEvent reçus sur /topic/progress
 *
 * La reconnexion automatique est gérée par le client STOMP (reconnectDelay).
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client | null = null;

  /** Vrai quand la connexion STOMP est établie */
  readonly connected = signal(false);

  /** Flux des événements de progression (émet uniquement des JSON valides) */
  private readonly progressSubject = new Subject<ProgressEvent>();
  readonly progress$: Observable<ProgressEvent> = this.progressSubject.asObservable();

  /**
   * Ouvre la connexion WebSocket et s'abonne au topic de progression.
   * Sans effet si une connexion est déjà active ou en cours.
   */
  connect(): void {
    if (this.client?.active) {
      return; // Déjà connecté (ou connexion en cours)
    }

    this.client = new Client({
      // SockJS sur l'endpoint relatif /ws (proxifié vers http://localhost:8081)
      webSocketFactory: () => new SockJS(WS_ENDPOINT),
      // Reconnexion automatique toutes les 5 secondes en cas de coupure
      reconnectDelay: 5000,
      // Trace désactivée en production (décommenter pour déboguer)
      debug: () => {},
      onConnect: () => {
        this.connected.set(true);
        this.client?.subscribe(PROGRESS_TOPIC, (message: IMessage) => {
          try {
            const event = JSON.parse(message.body) as ProgressEvent;
            this.progressSubject.next(event);
          } catch {
            // Événement malformé : on l'ignore sans casser le flux
          }
        });
      },
      onDisconnect: () => this.connected.set(false),
      onWebSocketClose: () => this.connected.set(false),
      onStompError: () => this.connected.set(false),
    });

    this.client.activate();
  }

  /** Ferme la connexion WebSocket et réinitialise l'état */
  disconnect(): void {
    if (this.client) {
      // deactivate() coupe la connexion et désactive la reconnexion automatique
      void this.client.deactivate();
      this.client = null;
    }
    this.connected.set(false);
  }
}
