import 'zone.js';

// sockjs-client utilise la variable Node.js `global` dans certains chemins
// d'exécution. Le navigateur expose `globalThis` à la place.
(globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;
