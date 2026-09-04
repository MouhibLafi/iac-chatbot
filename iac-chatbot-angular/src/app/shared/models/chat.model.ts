/**
 * Modèles liés au chatbot IaC.
 * Correspondent aux DTO du backend (ChatRequest, ChatResponse, ExtractedParameters)
 * et à l'entité InfrastructureRequest (historique).
 */

import { DeploymentLog, DeployResponse } from './deploy.model';
import { ProgressEvent } from './progress.model';

/** Plateformes cibles acceptées par POST /api/chatbot/process */
export type TargetPlatform = 'auto' | 'vsphere' | 'openshift';

/** Corps de la requête POST /api/chatbot/process */
export interface ChatRequest {
  message: string;
  targetPlatform: TargetPlatform;
}

/** Paramètres extraits de la demande en langage naturel (ExtractedParameters.java) */
export interface ExtractedParameters {
  resourceType?: string;   // VM ou CONTAINER
  platform?: string;       // VSPHERE ou OPENSHIFT
  osImage?: string;        // ubuntu-22.04, centos-9, ...
  cpu?: number;            // Nombre de CPUs
  ramGb?: number;          // RAM en Go
  storageGb?: number;      // Stockage en Go
  replicas?: number;       // Pour les conteneurs
  containerImage?: string; // nginx, mysql, ...
  network?: string;        // Nom du réseau
}

/** Réponse de POST /api/chatbot/process (ChatResponse.java) */
export interface ChatResponse {
  requestId: number;
  status: string; // completed, clarification_needed, failed, ...
  message: string;
  generatedCode?: string;
  extractedParams?: ExtractedParameters;
}

/** Entrée de l'historique GET /api/chatbot/requests (InfrastructureRequest.java) */
export interface InfrastructureRequest {
  id: number;
  userMessage: string;
  resourceType?: string;
  extractedParams?: string; // JSON sérialisé côté backend
  generatedCode?: string;
  targetPlatform?: string;
  createdAt?: string;
  processedAt?: string;
  status: string;
}

/** Message affiché dans l'interface de chat (modèle UI interne) */
export interface ChatMessage {
  kind: 'user' | 'bot' | 'error';
  text: string;            // Texte du message (bulle user / erreur)
  response?: ChatResponse; // Réponse complète du backend (bulle bot)
  copied?: boolean;        // État du bouton "Copier" du bloc de code

  // --- État du déploiement, simulé ou réel (POST /api/deploy/{requestId}) ---
  deploying?: boolean;               // Déploiement en cours
  deployProgress?: ProgressEvent | null; // Progression temps réel (WebSocket)
  deployResult?: DeployResponse;     // Résultat du déploiement (SUCCESS, CANCELLED...)
  deployLogs?: DeploymentLog[];      // Logs chargés via GET .../status
  showLogs?: boolean;                // Panneau de logs déplié
}
