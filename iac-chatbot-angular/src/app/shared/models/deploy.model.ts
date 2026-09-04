/**
 * Modèles liés au déploiement du code IaC généré (simulation ou réel).
 * Endpoints backend : POST/GET/DELETE /api/deploy/{requestId}[...]
 * et actions admin (approve/reject) qui partagent le même format de réponse.
 */

/** Réponse de POST/DELETE /api/deploy/{requestId} et des actions admin */
export interface DeployResponse {
  requestId: number;
  status: string; // SUCCESS, CANCELLED, FAILED, ...
  message: string;
  completedAt?: string;
  reason?: string; // Motif de rejet (actions admin)
}

/** Ligne de log d'un déploiement (entité DeploymentLog) */
export interface DeploymentLog {
  id: number;
  step: string;         // Étape du déploiement
  logLevel: string;     // INFO, WARN, ERROR
  message: string;
  timestamp: string;
}

/** Réponse de GET /api/deploy/{requestId}/status */
export interface DeployStatusResponse {
  requestId: number;
  status: string;
  logs: DeploymentLog[];
}
