import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { LoginComponent } from './features/auth/login.component';
import { ChatComponent } from './features/chat/chat.component';
import { HistoryComponent } from './features/history/history.component';
import { AdminComponent } from './features/admin/admin.component';

/**
 * Routes de l'application :
 * - /login : page publique (connexion / inscription)
 * - /chat  : interface chatbot (protégée, route par défaut)
 * - /history : historique des demandes (protégée)
 * - /admin : dashboard d'administration (protégée, ADMIN uniquement)
 * - **     : redirection vers /chat
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'chat', component: ChatComponent, canActivate: [authGuard] },
  { path: 'history', component: HistoryComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'chat' },
  { path: '**', redirectTo: 'chat' },
];
