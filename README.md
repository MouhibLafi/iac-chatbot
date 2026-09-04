# 🚀 IaC Chatbot — Infrastructure as Code pilotée par Chatbot Intelligent

Plateforme web permettant de **générer et déployer réellement** de l'infrastructure
(VMs et conteneurs) à partir de demandes en langage naturel (français/anglais),
grâce à une IA **100 % locale**.

**Plateformes cibles (document officiel) :** VMware vSphere (VMs) et OpenShift (conteneurs + VMs KubeVirt)
**Exécution réelle sur PC local :** VirtualBox (VMs) et MicroShift — vrai OpenShift — (conteneurs)
**Coût : 0 €** — uniquement des outils open-source et gratuits.

## Stack

| Couche | Technologie |
|---|---|
| Frontend | Angular 17+ (`iac-chatbot-angular/`) |
| Backend | Spring Boot 3.2, Java 21, Spring Security + JWT |
| IA | Ollama + Llama 3 (local, offline) |
| Génération IaC | Thymeleaf (mode TEXT) : Terraform vSphere, YAML OpenShift, YAML KubeVirt |
| Déploiement réel | `RealDeployExecutor` : VBoxManage (VMs VirtualBox) + oc (OpenShift/MicroShift) |
| Temps réel | WebSocket STOMP (progression de la génération en direct) |
| Base de données | H2 (dev) / MySQL Community (prod, via Docker Compose) |

## Fonctionnement

1. L'utilisateur formule une demande via le chatbot
2. Le chatbot analyse et structure la requête (llama3 local)
3. Le type de ressource et la plateforme sont identifiés
4. Le code Infrastructure as Code est généré
5. Validation et exécution du déploiement (étapes VALIDATION → PLAN → APPLY → VERIFY)
6. La ressource est créée réellement : VM dans VirtualBox / conteneur sur OpenShift

## Démarrage complet (mode réel — recommandé)

### Prérequis
- Java 21, Maven 3.9+, Node.js 18+
- Ollama avec le modèle llama3 (`ollama pull llama3`)
- VirtualBox (création réelle des VMs)
- La VM `microshift` (cluster OpenShift, déjà installée — voir `openshift/`)

```powershell
# 1. IA locale
ollama serve

# 2. Cluster OpenShift (MicroShift) — attendre ~2 min le démarrage
& "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" startvm microshift --type headless

# 3. Backend en mode REEL (port 8081)
cd iac-chatbot-backend
$env:DEPLOY_MODE="real"
$env:VBOXMANAGE_PATH="C:\Program Files\Oracle\VirtualBox\VBoxManage.exe"
$env:OC_BIN="C:\Users\mouhi\Desktop\iac test\openshift\oc.exe"
$env:OC_SERVER="https://127.0.0.1:16443"
$env:OC_TOKEN=Get-Content "C:\Users\mouhi\Desktop\iac test\openshift\oc-token.txt"
mvn spring-boot:run

# 4. Frontend (autre terminal)
cd iac-chatbot-angular
npm install   # première fois uniquement
npx ng serve  # http://localhost:4200
```

Le backend doit tourner **sur la machine hôte** (pas dans Docker) car VBoxManage et
oc sont des binaires Windows. Sans les variables `DEPLOY_MODE/OC_*`, le backend
démarre en mode **simulation** (aucune ressource réelle créée).

### Connexion

| Utilisateur | Mot de passe | Rôle |
|---|---|---|
| `admin` | `password123` | ADMIN |
| `user` | `password123` | USER |

## Exemples de requêtes

- « Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM » → VM réelle créée dans VirtualBox (+ code Terraform généré)
- « Déploie un conteneur nginx avec 3 replicas sur OpenShift » → Deployment + Service + Route réels sur MicroShift
- « Crée une VM Windows Server avec 100 Go de disque sur OpenShift » → YAML KubeVirt

## Documentation

| Document | Description |
|---|---|
| [Cahier_des_Charges_IaC_Chatbot_FINAL.pdf](Cahier_des_Charges_IaC_Chatbot_FINAL.pdf) | Cahier des charges final (version officielle) |
| [Architecture_IaC_Chatbot_UML_FINALE.pdf](Architecture_IaC_Chatbot_UML_FINALE.pdf) | Architecture et diagrammes UML |
| [Infrastructure as Code & Automation 1.pdf](<Infrastructure as Code & Automation 1.pdf>) | Document officiel de la société |

## API principale

| Endpoint | Description |
|---|---|
| `POST /api/auth/register` / `login` | Authentification JWT |
| `POST /api/chatbot/process` | Traiter un message → code IaC |
| `GET /api/chatbot/requests` | Historique des demandes |
| `POST /api/deploy/{requestId}` | Déployer (réel ou simulé selon `deploy.mode`) |
| `GET /api/deploy/{requestId}/status` | Statut + logs du déploiement |
| `DELETE /api/deploy/{requestId}` | Annuler (supprime la VM / les ressources OpenShift) |
| `GET /api/users` (ADMIN) | Gestion des utilisateurs |
| `GET /swagger-ui.html` | Documentation API interactive |

## Tests

```powershell
cd iac-chatbot-backend
mvn test              # 59 tests unitaires (génération IaC, LLM, déploiement, controller)
```

## Déploiement Docker (production, mode simulation)

```powershell
# Copier et adapter les variables
copy .env.example .env

# Lancer toute la stack (MySQL + Backend + Frontend)
docker compose up -d
```

Application : http://localhost (frontend) — API : http://localhost:8082

## Cluster OpenShift local (MicroShift)

Un vrai cluster OpenShift (MicroShift 4.18 sur AlmaLinux 9) tourne dans la VM
VirtualBox `microshift`. Redirections NAT : `16443→6443` (API), `9080→80`,
`9443→443` (Routes), `2222→22` (SSH).

- Fichiers du cluster : `openshift/` (oc.exe, pull secret Red Hat, token, clés SSH, seed cloud-init)
- Redémarrage : `VBoxManage startvm microshift --type headless`
- Vérification : `openshift\oc.exe get nodes --server=https://127.0.0.1:16443 --token=<oc-token.txt> --insecure-skip-tls-verify=true`
- Note : KubeVirt (VMs dans OpenShift) n'est pas exécutable sur cette machine (RAM) ;
  le YAML KubeVirt est généré et prêt pour un vrai cluster.
