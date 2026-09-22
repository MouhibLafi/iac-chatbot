/**
 * Modèle des événements de progression diffusés via WebSocket STOMP
 * sur le topic /topic/progress pendant le traitement d'une demande IaC.
 *
 * Séquence typique : EXTRACTION 10% → EXTRACTION 40% → GENERATION 60%
 * → COMPLETED 100% (ou ERROR 100%).
 */

/** Étapes du traitement d'une demande */
export type ProgressStep = 'EXTRACTION' | 'GENERATION' | 'COMPLETED' | 'ERROR';

/** Événement de progression reçu sur /topic/progress */
export interface ProgressEvent {
  /** Identifiant de la demande concernée */
  requestId: number;
  /** Étape courante du traitement */
  step: ProgressStep;
  /** Pourcentage d'avancement (0-100) */
  percent: number;
  /** Message lisible décrivant l'étape (ex. "Analyse de votre demande par l'IA...") */
  message: string;
}
