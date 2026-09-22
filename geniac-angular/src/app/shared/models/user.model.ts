/**
 * Modèle utilisateur pour le dashboard admin.
 * Correspond au DTO du backend (UserDto.java) — GET /api/users.
 */
export interface UserDto {
  id: number;
  username: string;
  email: string;
  role: string; // ex. "ADMIN" ou "USER"
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
  // Quotas de ressources (optionnels selon la version du backend)
  quotaCpu?: number;
  quotaRam?: number;
  quotaStorage?: number;
}

/** Corps de la requête PUT /api/users/{id}/quota (ADMIN) */
export interface QuotaRequest {
  quotaCpu: number;
  quotaRam: number;
  quotaStorage: number;
}
