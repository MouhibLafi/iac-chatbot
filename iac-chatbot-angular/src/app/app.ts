import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Composant racine : simple conteneur du routeur.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
