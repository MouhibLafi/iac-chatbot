import { bootstrapApplication } from '@angular/platform-browser';

// sockjs-client attend la variable Node.js `global`. Elle doit être définie
// avant le chargement des services de l'application qui importent SockJS.
(globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;

Promise.all([import('./app/app'), import('./app/app.config')])
  .then(([{ App }, { appConfig }]) => bootstrapApplication(App, appConfig))
  .catch((err) => console.error(err));
