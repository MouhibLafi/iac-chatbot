import { ExtractedParameters } from '../models/chat.model';

/**
 * Utilitaires partagés pour l'affichage des demandes d'infrastructure
 * (historique, validations admin, etc.).
 */

/** Badge d'affichage d'un paramètre extrait */
export interface ParamBadge {
  label: string;
  value: string;
}

/**
 * Convertit le champ extractedParams (string JSON côté backend)
 * en objet ExtractedParameters typé. Retourne null si absent ou invalide.
 */
export function parseExtractedParams(json?: string | null): ExtractedParameters | null {
  if (!json) {
    return null;
  }
  try {
    return JSON.parse(json) as ExtractedParameters;
  } catch {
    return null;
  }
}

/** Construit la liste des badges des paramètres extraits présents */
export function paramBadges(params?: ExtractedParameters | null): ParamBadge[] {
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

/**
 * Classe CSS du badge de statut d'une demande :
 * - completed / CODE_GENERATED → bleu
 * - SUCCESS → vert, RUNNING → orange
 * - FAILED / CANCELLED → rouge, PENDING_APPROVAL → violet
 */
export function statusBadgeClass(status?: string): string {
  switch (status) {
    case 'SUCCESS':
    case 'success':
      return 'badge-success';
    case 'RUNNING':
      return 'badge-running';
    case 'FAILED':
    case 'CANCELLED':
    case 'failed':
    case 'error':
      return 'badge-error';
    case 'PENDING_APPROVAL':
      return 'badge-pending';
    case 'clarification_needed':
      return 'badge-warning';
    default:
      // completed, CODE_GENERATED, etc.
      return 'badge-info';
  }
}

/** Formate une date ISO en date/heure française lisible */
export function formatDate(iso?: string): string {
  if (!iso) {
    return '—';
  }
  try {
    return new Date(iso).toLocaleString('fr-FR');
  } catch {
    return iso;
  }
}
