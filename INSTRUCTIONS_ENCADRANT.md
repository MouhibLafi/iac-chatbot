# 📦 PROJET GENIAC — INSTRUCTIONS POUR L'ENCADRANT

**Infrastructure as Code & Automation pilotée par Chatbot Intelligent**

---

## 🎯 RÉSUMÉ DU PROJET

**Geniac** est une plateforme web permettant de générer et déployer réellement de l'infrastructure (VMs et conteneurs) à partir de demandes en langage naturel (français/anglais), grâce à une IA **100 % locale et gratuite**.

### Plateformes supportées
- **VMware vSphere** : VMs classiques (code Terraform généré)
- **OpenShift Container Platform** : Conteneurs + VMs KubeVirt (YAML généré)

### Exécution locale (sur PC étudiant)
- **VirtualBox** : Création réelle de VMs via VBoxManage
- **MicroShift** : Vrai cluster OpenShift (conteneurs réels)

### Coût total : **0 €** (100% outils open-source et gratuits)

---

## 📁 STRUCTURE DU PROJET

```
iac test/
├── 📄 README.md                           → Présentation + démarrage rapide
├── 📄 GUIDE_DEMARRAGE.md                  → Guide pas à pas détaillé
├── 📄 Cahier_des_Charges_Geniac.md        → Cahier des charges complet
├── 📄 Architecture_Geniac.md              → Architecture + diagrammes UML
├── 📄 Rapport_de_Stage_Geniac.md          → Rapport de stage complet
├── 📄 Infrastructure as Code & Automation 1.pdf  → Document officiel
├── 📄 start-demo.ps1                      → Script démarrage automatique
├── 📄 .gitignore                          → Fichiers Git ignorés
│
├── 📂 iac-chatbot-backend/                → Backend Spring Boot (Java 21)
│   ├── src/main/java/                     → Code source Java
│   │   └── com/company/iacchatbot/
│   │       ├── controller/                → API REST
│   │       ├── service/                   → Logique métier
│   │       ├── model/                     → Entités JPA
│   │       ├── repository/                → Repositories
│   │       ├── security/                  → JWT + Spring Security
│   │       └── config/                    → Configuration
│   ├── src/main/resources/
│   │   ├── templates/                     → Templates IaC (Thymeleaf)
│   │   │   ├── terraform/                 → Templates Terraform vSphere
│   │   │   └── openshift/                 → Templates YAML OpenShift
│   │   └── application.properties         → Configuration application
│   ├── src/test/java/                     → 61 tests unitaires
│   └── pom.xml                            → Dépendances Maven
│
├── 📂 iac-chatbot-angular/                → Frontend Angular 17+
│   ├── src/app/
│   │   ├── features/                      → Fonctionnalités
│   │   │   ├── auth/                      → Login/Register
│   │   │   ├── chat/                      → Interface chatbot
│   │   │   ├── history/                   → Historique
│   │   │   └── admin/                     → Dashboard admin
│   │   ├── core/                          → Guards, Interceptors, Services
│   │   └── shared/                        → Composants réutilisables
│   ├── package.json                       → Dépendances npm
│   └── angular.json                       → Configuration Angular
│
└── 📂 openshift/                          → Cluster OpenShift local
    ├── oc.exe                             → Client OpenShift
    ├── oc-token.txt                       → Token d'authentification
    ├── pull-secret.json                   → Pull secret Red Hat
    └── README_CLUSTER.md                  → Documentation cluster
```

---

## 🚀 DÉMARRAGE RAPIDE

### Prérequis installés sur le PC de l'étudiant
- ✅ Java 21 + Maven 3.9+
- ✅ Node.js 18+
- ✅ MySQL 8.0 (XAMPP) — base `iac_chatbot`
- ✅ Ollama + modèle llama3
- ✅ VirtualBox 7.2+ (création VMs)
- ✅ VM `microshift` (cluster OpenShift)

### Démarrage manuel (5 services)

```powershell
# 0. MySQL : Démarrer dans XAMPP Control Panel

# 1. Ollama (IA locale)
ollama serve

# 2. Cluster OpenShift (VM MicroShift)
VBoxManage startvm microshift --type headless

# 3. Backend Spring Boot (port 8081)
cd iac-chatbot-backend
$env:OC_TOKEN=Get-Content "../openshift/oc-token.txt"
mvn spring-boot:run

# 4. Frontend Angular (port 4200)
cd iac-chatbot-angular
npm install   # première fois uniquement
npx ng serve
```

### Démarrage automatique (script PowerShell)

```powershell
cd "C:\Users\mouhi\Desktop\iac test"
powershell -ExecutionPolicy Bypass -File .\start-demo.ps1
```

Le script démarre automatiquement tous les services.

---

## 🌐 ACCÈS À L'APPLICATION

| Service | URL | Identifiants |
|---------|-----|--------------|
| **Application Web** | http://localhost:4200 | admin / password123 |
| **API Backend** | http://localhost:8081 | — |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | Documentation API |
| **phpMyAdmin** | http://localhost/phpmyadmin/ | Base `iac_chatbot` |

### Comptes utilisateurs

| Username | Password | Rôle | Quotas |
|----------|----------|------|--------|
| admin | password123 | ADMIN | 128 CPU, 512 GB RAM, 5000 GB Storage |
| user | password123 | USER | 32 CPU, 128 GB RAM, 1000 GB Storage |
| testdemo | password123 | USER | 32 CPU, 128 GB RAM, 1000 GB Storage |

---

## 🎬 DÉMONSTRATION (Scénario 5 minutes)

### Étape 1 : Connexion
1. Ouvrir http://localhost:4200
2. Se connecter avec `admin` / `password123`

### Étape 2 : Demande de VM
1. Dans le chatbot, écrire : **"Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM"**
2. Observer l'extraction automatique des paramètres par Ollama (IA locale)
3. Visualiser le code Terraform généré
4. Cliquer sur **"Déployer"**
5. Observer la progression en temps réel (WebSocket)

### Résultat attendu
- ✅ VM créée réellement dans VirtualBox (visible dans l'interface VirtualBox)
- ✅ Code Terraform vSphere généré et disponible
- ✅ Logs de déploiement complets dans l'historique
- ✅ Données persistées dans MySQL (visible dans phpMyAdmin)

### Étape 3 : Demande de conteneur
1. Écrire : **"Déploie nginx avec 3 replicas sur OpenShift"**
2. Observer le code YAML Kubernetes généré
3. Déployer
4. Vérifier dans l'historique : 3 pods Running sur le cluster MicroShift

### Étape 4 : Administration
1. Aller dans le panel Admin
2. Consulter les statistiques globales
3. Voir la liste des utilisateurs et leurs quotas
4. Consulter l'historique complet des déploiements

---

## ✅ FONCTIONNALITÉS IMPLÉMENTÉES

### Module Chatbot (IA locale Ollama)
- ✅ Compréhension langage naturel (FR/EN) via Llama 3
- ✅ Extraction automatique des paramètres
- ✅ Support VMware vSphere ET OpenShift
- ✅ Génération code IaC (Terraform + YAML)

### Module Déploiement RÉEL
- ✅ Pipeline complet : VALIDATION → PLAN → APPLY → VERIFY
- ✅ VMs créées dans VirtualBox (VBoxManage)
- ✅ Conteneurs déployés sur OpenShift (MicroShift)
- ✅ Nommage unique des ressources
- ✅ Annulation réelle (suppression des ressources)

### Module Authentification & Sécurité
- ✅ JWT avec expiration 24h
- ✅ Rôles USER et ADMIN
- ✅ Mots de passe hashés (BCrypt)
- ✅ Protection des endpoints

### Module Suivi & Administration
- ✅ Historique complet des demandes
- ✅ Logs détaillés de chaque déploiement
- ✅ Dashboard statistiques
- ✅ Gestion des utilisateurs (ADMIN)
- ✅ Gestion des quotas (ADMIN)
- ✅ Workflow d'approbation (ADMIN)
- ✅ WebSocket temps réel

---

## 📊 TESTS

### Tests unitaires
- **61 tests** automatisés (JUnit 5 + Mockito)
- **100% passent**
- Couverture : Controllers, Services, Génération IaC, Déploiement

```powershell
cd iac-chatbot-backend
mvn test
```

### Tests de bout en bout (réalisés)
- ✅ VM réelle créée dans VirtualBox
- ✅ Conteneurs réels déployés sur OpenShift
- ✅ Annulation réelle (ressources supprimées)
- ✅ Persistance complète (redémarrage sans perte)

---

## 🎓 DOCUMENTATION COMPLÈTE

| Document | Description | Pages |
|----------|-------------|-------|
| **README.md** | Présentation + démarrage | 3 |
| **GUIDE_DEMARRAGE.md** | Guide pas à pas détaillé | 5 |
| **Cahier_des_Charges_Geniac.md** | Cahier des charges complet | 25 |
| **Architecture_Geniac.md** | Architecture + diagrammes UML | 15 |
| **Rapport_de_Stage_Geniac.md** | Rapport de stage complet | 35 |

---

## 💰 ÉCONOMIE RÉALISÉE

| Composant | Version payante | Version gratuite | Économie |
|-----------|-----------------|------------------|----------|
| IA / LLM | OpenAI API (~$20-50/mois) | Ollama + Llama 3 (local) | $20-50/mois |
| BDD prod | MySQL Enterprise (~$2000/an) | MySQL Community | $2000/an |
| CI/CD | Jenkins Enterprise (~$10000/an) | Pipeline intégré | $10000/an |
| VMware (local) | vSphere License (~$1000+/an) | VirtualBox | $1000+/an |
| OpenShift (local) | Red Hat License (~$5000/an) | MicroShift | $5000+/an |

**ÉCONOMIE TOTALE : ~18 000 $/an**  
**COÛT TOTAL DU PROJET : 0 €**

---

## 🔧 TECHNOLOGIES UTILISÉES

### Stack technique (100% GRATUITE)

| Couche | Technologie | Version |
|--------|-------------|---------|
| Frontend | Angular | 17+ |
| Backend | Spring Boot | 3.2+ |
| IA / LLM | Ollama + Llama 3 | — |
| Base de données | MySQL Community | 8.0+ |
| Templates | Thymeleaf | 3.1+ |
| IaC VMware | Terraform | 1.5+ |
| IaC OpenShift | YAML Kubernetes | — |
| Exécution VMs | VirtualBox (VBoxManage) | 7.2+ |
| Exécution OpenShift | MicroShift | 4.18 |
| Sécurité | Spring Security + JWT | 6.2+ |
| Temps réel | WebSocket STOMP | — |
| Documentation | Swagger UI | 2.5+ |

---

## 📝 NOTES IMPORTANTES

### Persistance des données
- ✅ Toutes les données sont dans la base MySQL `iac_chatbot`
- ✅ Historique conservé après redémarrage
- ✅ Consultation possible via phpMyAdmin

### IA 100% locale
- ✅ Ollama fonctionne en local (offline)
- ✅ Aucune donnée envoyée à l'extérieur
- ✅ Zéro coût API
- ✅ Confidentialité totale

### Déploiements réels
- ✅ VMs créées dans VirtualBox (visibles dans l'interface)
- ✅ Conteneurs déployés sur MicroShift (pods réels)
- ✅ Annulation supprime réellement les ressources
- ✅ Journalisation complète de toutes les étapes

---

## 📧 CONTACT

**Étudiant :** [Nom de l'étudiant]  
**Email :** [Email de l'étudiant]  
**Entreprise :** [Nom de l'entreprise]  
**Période :** Septembre 2026 (1 mois)

---

## ✅ VALIDATION DU CAHIER DES CHARGES

Toutes les exigences du cahier des charges sont respectées :

- ✅ Chatbot conversationnel (FR/EN)
- ✅ IA 100% locale (Ollama + Llama 3)
- ✅ Génération code IaC (Terraform + YAML)
- ✅ Support VMware vSphere + OpenShift
- ✅ Déploiement 100% réel (VMs + conteneurs)
- ✅ Authentification JWT sécurisée
- ✅ Gestion des rôles et quotas
- ✅ Historique et traçabilité
- ✅ Dashboard administrateur
- ✅ WebSocket temps réel
- ✅ Tests automatisés (61 tests)
- ✅ Documentation complète
- ✅ Coût total : 0 €

**Le projet est complet et prêt pour la démonstration !** 🎉

---

*Dernière mise à jour : $(Get-Date -Format "dd/MM/yyyy HH:mm")*
