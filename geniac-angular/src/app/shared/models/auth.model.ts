/**
 * Modèles liés à l'authentification JWT.
 * Correspondent aux DTO du backend Spring Boot (LoginRequest, RegisterRequest, LoginResponse).
 */

/** Corps de la requête POST /api/auth/login */
export interface LoginRequest {
  username: string;
  password: string;
}

/** Corps de la requête POST /api/auth/register */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

/** Réponse de POST /api/auth/login (voir LoginResponse.java) */
export interface LoginResponse {
  token: string;
  type: string; // "Bearer"
  id: number;
  username: string;
  email: string;
  role: string; // ex. "ADMIN" ou "USER"
}

/** Utilisateur courant stocké dans le localStorage */
export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  role: string;
}

/** Réponse générique { message } du backend (MessageResponse.java) */
export interface MessageResponse {
  message: string;
}
