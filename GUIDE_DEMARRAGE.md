# 🧞 GENIAC — Guide de lancement pas à pas

**À suivre à chaque fois que tu veux démarrer le projet (démo, test, présentation).**

---

## ✅ Prérequis (une seule fois)

- Java 21, Maven, Node.js installés
- XAMPP installé
- Ollama installé avec le modèle llama3
- VirtualBox installé avec la VM `microshift`

---

## 🚀 Démarrage — 5 étapes dans l'ordre

### Étape 0 — MySQL (XAMPP)

1. Ouvre **XAMPP Control Panel**
2. Clique **Start** sur la ligne **MySQL**
3. (Optionnel, pour voir les données) : clique aussi **Start** sur **Apache**
   → phpMyAdmin : http://localhost/phpmyadmin/ (base `iac_chatbot`)

### Étape 1 — Ollama (l'IA)

Ouvre un terminal PowerShell :

```powershell
ollama serve
```

Laisse cette fenêtre ouverte.

### Étape 2 — Cluster OpenShift (VM microshift)

Nouveau terminal PowerShell :

```powershell
& "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" startvm microshift --type headless
```

⏱️ Attends **~2 minutes**, puis vérifie :

```powershell
cd "C:\Users\mouhi\Desktop\iac test\openshift"
$token = Get-Content oc-token.txt
.\oc.exe get nodes --server=https://127.0.0.1:16443 --token=$token --insecure-skip-tls-verify=true
```

→ Tu dois voir `microshift   Ready`. Si erreur, attends 30 s et réessaie.

### Étape 3 — Backend (port 8081)

Nouveau terminal PowerShell :

```powershell
cd "C:\Users\mouhi\Desktop\iac test\geniac-backend"
$env:OC_TOKEN=Get-Content "C:\Users\mouhi\Desktop\iac test\openshift\oc-token.txt"
mvn spring-boot:run
```

Attends le message `Started IacChatbotApplication`.

### Étape 4 — Frontend (port 4200)

Nouveau terminal PowerShell :

```powershell
cd "C:\Users\mouhi\Desktop\iac test\geniac-angular"
npx ng serve
```

Attends `Compiled successfully`.

---

## 🌐 Utiliser l'application

- **Application** : http://localhost:4200
- **Login admin** : `admin` / `password123`
- **Login user** : `user` / `password123`
- **Documentation API (Swagger)** : http://localhost:8081/swagger-ui.html
- **Base de données (phpMyAdmin)** : http://localhost/phpmyadmin/

---

## ⚡ Option rapide : tout en 1 commande

```powershell
cd "C:\Users\mouhi\Desktop\iac test"
powershell -ExecutionPolicy Bypass -File .\start-demo.ps1
```

Le script démarre MySQL, Ollama, microshift, le backend et le frontend automatiquement.

---

## 🛑 Arrêt — après la démo

1. **Ctrl+C** dans chaque fenêtre PowerShell (backend, frontend, Ollama)
2. Éteindre la VM microshift :

```powershell
& "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" controlvm microshift acpipowerbutton
```

3. Dans XAMPP : **Stop** sur MySQL (et Apache)

---

## 🔧 Problèmes fréquents

| Problème | Solution |
|---|---|
| « Erreur de connexion » au login | MySQL n'est pas démarré → XAMPP, Start MySQL |
| « already locked » (VBoxManage startvm) | La VM tourne déjà → rien à faire, continue |
| « Echec du déploiement » | Vérifie que la VM microshift est Ready (étape 2) |
| La page ne charge pas | Vérifie que les 4 fenêtres tournent + F5 dans le navigateur |
| Port 4200 ou 8081 occupé | Ferme l'ancienne fenêtre du terminal concerné |

---

## 📦 Envoyer le projet à l'encadrant

Le fichier ZIP final est déjà prêt sur ton Bureau :

```
C:\Users\mouhi\Desktop\Geniac-v1.0-final.zip
```

**Si tu modifies le projet et veux un ZIP à jour**, lance dans PowerShell :

```powershell
$src = "C:\Users\mouhi\Desktop\iac test"
$zip = "C:\Users\mouhi\Desktop\Geniac-v1.0-final.zip"
$tmp = "C:\Users\mouhi\Desktop\geniac-package"
if (Test-Path $zip) { Remove-Item $zip }
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Path $tmp | Out-Null
robocopy $src $tmp /E /XD node_modules target .angular dist .git /NFL /NDL /NJH /NJS | Out-Null
Compress-Archive -Path "$tmp\*" -DestinationPath $zip
Remove-Item -Recurse -Force $tmp
Write-Host "ZIP créé : $zip"
```

---

## 📄 Documents inclus dans le projet

| Fichier | Contenu |
|---|---|
| `README.md` | Présentation + démarrage |
| `Cahier_des_Charges_Geniac.md` | Cahier des charges complet |
| `Architecture_Geniac.md` | Architecture + diagrammes UML (PlantUML) |
| `Rapport_de_Stage_Geniac.md` | Rapport de stage |
| `GUIDE_DEMARRAGE.md` | Ce fichier |

---

*Geniac — Infrastructure as Code pilotée par IA locale. Coût : 0 €.*
