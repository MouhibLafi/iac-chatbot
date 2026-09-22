# ARCHITECTURE DU PROJET GENIAC — VERSION FINALE
## Infrastructure as Code & Automation pilotée par Chatbot Intelligent

> **Version FINALE 4.0** — Projet **Geniac**, alignée sur l'implémentation réelle livrée.
> Points clés : **Ollama + Llama 3** (IA 100 % locale), **VMware vSphere + OpenShift** (aucun cloud public),
> **déploiement 100 % réel** (le mode simulation a été supprimé), base MySQL locale persistante (XAMPP).

---

## Stack technique

| Couche | Technologie | Rôle |
|---|---|---|
| Frontend | Angular 17+ — interface « Geniac » style ChatGPT/Claude (avatars, streaming, menu déroulant) | Interface utilisateur (chatbot + historique + admin) |
| Backend | Spring Boot 3.2+ | API REST, logique métier |
| IA / LLM | **Ollama + Llama 3 (local, gratuit)** | Extraction paramètres langage naturel |
| Base de données | MySQL 8.0 Community local (XAMPP / phpMyAdmin, base `iac_chatbot`) | Stockage historique et utilisateurs |
| Templates | Thymeleaf (mode TEXT) | Génération dynamique code IaC |
| IaC VMware | Terraform (code généré) / **VirtualBox** (exécution réelle locale via VBoxManage) | Provisioning VMs |
| IaC OpenShift | oc / **MicroShift 4.18** (cluster OpenShift réel en VM VirtualBox) | Déploiement conteneurs et KubeVirt |
| Exécution déploiements | `RealDeployExecutor` : VBoxManage + oc | Pipeline réel VALIDATION → PLAN → APPLY → VERIFY + journalisation |
| Sécurité | Spring Security + JWT | Authentification et autorisation |
| Temps réel | WebSocket STOMP | Progression des déploiements en direct |

---

## 1. Diagramme de Cas d'Utilisation

### Acteurs

| Acteur | Description | Privilèges |
|---|---|---|
| Utilisateur | Développeur, ops ou chef de projet qui demande des ressources via le chatbot | S'authentifier, demander déploiement, visualiser code, consulter historique, annuler |
| Administrateur | Responsable infrastructure qui supervise la plateforme | Tout + valider demandes, gérer quotas, statistiques globales, templates |
| Moteur de déploiement | `RealDeployExecutor` (backend) qui exécute automatiquement le pipeline | Exécuter VBoxManage (VirtualBox), oc apply (OpenShift/MicroShift), journaliser résultats |

### Diagramme (PlantUML)

```plantuml
@startuml
left to right direction
actor "Utilisateur" as user
actor "Administrateur" as admin
actor "Moteur de déploiement\n(RealDeployExecutor)" as cicd

rectangle "Plateforme Geniac" {
  usecase "UC-01 S'authentifier (JWT)" as UC01
  usecase "UC-02 S'inscrire" as UC02
  usecase "UC-04 Demander un\ndéploiement" as UC04
  usecase "UC-05 Visualiser le\ncode généré" as UC05
  usecase "UC-06 Confirmer le\ndéploiement réel" as UC06
  usecase "UC-08 Consulter\nl'historique" as UC08
  usecase "UC-09 Consulter le\ndashboard" as UC09
  usecase "UC-10 Annuler un\ndéploiement" as UC10
  usecase "UC-11 Gérer les\nutilisateurs" as UC11
  usecase "UC-12 Valider les\ndemandes" as UC12
  usecase "UC-13 Configurer\nles quotas" as UC13
  usecase "Exécuter pipeline réel\n(VBoxManage/oc apply)" as PIPELINE
}

user --> UC01
user --> UC02
user --> UC04
user --> UC05
user --> UC06
user --> UC08
user --> UC09
user --> UC10
admin --> UC01
admin --> UC11
admin --> UC12
admin --> UC13
UC06 ..> PIPELINE : déclenche
UC10 ..> PIPELINE : supprime les ressources
cicd --> PIPELINE
@enduml
```

---

## 2. Diagramme de Classes

### Relations entre les classes

| Relation | Type | Cardinalité | Description |
|---|---|---|---|
| User → InfrastructureRequest | Composition | 1 → * | Un utilisateur peut faire plusieurs demandes |
| InfrastructureRequest → DeploymentLog | Composition | 1 → * | Une demande génère plusieurs logs |

### Diagramme (PlantUML)

```plantuml
@startuml
class User {
  -id : Long
  -username : String
  -email : String
  -password : String (BCrypt)
  -role : Role
  -quotaCpu : Integer = 32
  -quotaRam : Integer = 128
  -quotaStorage : Integer = 1000
  -enabled : Boolean
}

class InfrastructureRequest {
  -id : Long
  -userMessage : String
  -resourceType : ResourceType
  -targetPlatform : String
  -extractedParams : String (JSON)
  -generatedCode : String
  -resourceName : String (nom réel ex: iac-vm-x7k)
  -status : String
  -errorMessage : String
  -createdAt : LocalDateTime
  -processedAt : LocalDateTime
  -completedAt : LocalDateTime
}

class DeploymentLog {
  -id : Long
  -step : String
  -logLevel : LogLevel
  -message : String
  -timestamp : LocalDateTime
}

enum ResourceType {
  VM
  CONTAINER
}

enum PlatformType {
  VSPHERE
  OPENSHIFT
}

enum Role {
  USER
  ADMIN
}

User "1" *-- "*" InfrastructureRequest
InfrastructureRequest "1" *-- "*" DeploymentLog
InfrastructureRequest --> ResourceType
InfrastructureRequest --> PlatformType
User --> Role
@enduml
```

> **Note :** le champ `resourceName` stocke le nom réel de la ressource créée (ex : `iac-vm-x7k`,
> suffixe de 3 caractères aléatoires). Il garantit l'unicité des noms et permet à l'annulation
> de supprimer exactement la bonne ressource, même après redémarrage de la base.

---

## 3. Diagramme de Séquence — Déploiement d'une VM (VirtualBox local / VMware vSphere entreprise)

### Description du flux principal

| Étape | Action | Détails |
|---|---|---|
| 1-4 | Extraction des paramètres | L'utilisateur formule une demande en langage naturel. Le système utilise **Ollama (Llama 3)** en local pour extraire les paramètres (type, plateforme, CPU, RAM, stockage, OS) |
| 5-7 | Génération du code IaC | Spring Boot utilise **Thymeleaf** (mode TEXT) pour générer le code Terraform vSphere. Le code est stocké en base |
| 8-12 | Validation et déclenchement | L'utilisateur visualise le code généré et confirme. Le système met à jour le statut et déclenche le pipeline de déploiement réel (`RealDeployExecutor`) |
| 13-16 | Exécution du déploiement | Le moteur exécute `VBoxManage` (création VM réelle dans VirtualBox) ou `oc apply` (OpenShift/MicroShift). Résultat journalisé en base |
| 17-20 | Notification et confirmation | Progression temps réel (WebSocket), notification optionnelle (Discord Webhook) et confirmation à l'utilisateur |

### Diagramme (PlantUML)

```plantuml
@startuml
actor Utilisateur
participant "Frontend Geniac\n(Angular)" as FE
participant "ChatbotController" as CTRL
participant "LlmService\n(Ollama/Llama3)" as LLM
participant "IaCGeneratorService\n(Thymeleaf)" as GEN
database "MySQL" as DB
participant "DeployService +\nRealDeployExecutor" as DEPLOY
participant "VirtualBox / OpenShift\n(MicroShift)" as INFRA

Utilisateur -> FE : "Je veux une VM Ubuntu\n2 CPU, 4 Go RAM"
FE -> CTRL : POST /api/chatbot/process\n(JWT + message)
CTRL -> LLM : extractParameters(message)
LLM -> LLM : Prompt extraction JSON\n(100% local, offline)
LLM --> CTRL : ExtractedParameters\n{VM, VSPHERE, cpu:2, ramGb:4}
CTRL -> DB : save(InfrastructureRequest)
CTRL -> GEN : generateCode(params)
GEN -> GEN : render(terraform/vmware-vm.tf)
GEN --> CTRL : Code Terraform vSphere
CTRL -> DB : update(generatedCode, COMPLETED)
CTRL --> FE : ChatResponse (code + params)
FE --> Utilisateur : Affiche le code Terraform\n(effet streaming)

Utilisateur -> FE : Confirme le déploiement
FE -> CTRL : POST /api/deploy (JWT)
CTRL -> DEPLOY : deploy(requestId)
DEPLOY -> DEPLOY : resolveVmName()\niac-vm-<3 caractères aléatoires>
DEPLOY -> DEPLOY : VALIDATION / PLAN / APPLY / VERIFY\n(VBoxManage ou oc apply)
DEPLOY -> INFRA : Provisionnement VM / conteneur
INFRA --> DEPLOY : Ressource opérationnelle
DEPLOY -> DB : Journalisation résultat + resourceName
DEPLOY --> Utilisateur : Progression WebSocket temps réel\n+ Notification (Discord Webhook)
@enduml
```

---

## 4. Structure du Projet (implémentation réelle)

```
iac-chatbot-backend/
├── src/main/java/com/company/iacchatbot/
│   ├── IacChatbotApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security + JWT
│   │   ├── OllamaConfig.java            # Configuration Ollama (Spring AI)
│   │   ├── IaCTemplateConfig.java       # Moteur Thymeleaf TEXT pour IaC
│   │   └── DataInitializer.java         # Utilisateurs par défaut
│   ├── controller/
│   │   ├── AuthController.java          # /api/auth/**
│   │   ├── ChatbotController.java       # /api/chatbot/**
│   │   ├── DeployController.java        # /api/deploy/** (déploiement réel)
│   │   ├── HistoryController.java       # /api/history/**
│   │   ├── AdminController.java         # /api/admin/** (approbations)
│   │   └── UserController.java          # /api/users/** (ADMIN)
│   ├── dto/
│   │   ├── ChatRequest.java             # {message, targetPlatform}
│   │   ├── ChatResponse.java            # {status, params, generatedCode}
│   │   ├── ExtractedParameters.java     # {resourceType, platform, cpu, ramGb...}
│   │   ├── ProgressEvent.java           # Événement WebSocket /topic/progress
│   │   ├── QuotaRequest.java            # {quotaCpu, quotaRam, quotaStorage}
│   │   ├── LoginRequest.java / LoginResponse.java
│   │   ├── RegisterRequest.java / UserDto.java / MessageResponse.java
│   ├── model/
│   │   ├── User.java                    # JPA + rôles + quotas
│   │   ├── InfrastructureRequest.java   # JPA demande + code généré + resourceName + user
│   │   ├── DeploymentLog.java           # JPA journal des étapes
│   │   ├── ResourceType.java            # VM, CONTAINER
│   │   ├── PlatformType.java            # VSPHERE, OPENSHIFT
│   │   ├── LogLevel.java                # INFO, WARN, ERROR
│   │   └── Role.java                    # USER, ADMIN
│   ├── repository/                      # Repositories JPA
│   ├── service/
│   │   ├── AuthService.java             # login, register, JWT
│   │   ├── LlmService.java              # Extraction via Ollama (Llama 3)
│   │   ├── IaCGeneratorService.java     # Génération Thymeleaf (vSphere/OpenShift/KubeVirt)
│   │   ├── DeployService.java           # Orchestration du déploiement réel + logs
│   │   ├── RealDeployExecutor.java      # Exécution réelle : VBoxManage (VMs) / oc (OpenShift)
│   │   ├── QuotaService.java            # Contrôle quotas (UC-13)
│   │   ├── NotificationService.java     # Discord Webhook
│   │   ├── ProgressNotificationService.java  # WebSocket temps réel
│   │   ├── UserService.java             # Gestion utilisateurs (suppression avec détachement)
│   │   └── UserDetailsServiceImpl.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # Format d'erreur standard
│   │   ├── QuotaExceededException.java
│   │   └── DeploymentFailedException.java
│   └── security/
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       └── JwtAuthenticationEntryPoint.java
├── src/main/resources/
│   ├── templates/
│   │   ├── terraform/
│   │   │   └── vmware-vm.tf             # Template Thymeleaf Terraform vSphere
│   │   └── openshift/
│   │       ├── deployment.yaml          # Deployment Kubernetes
│   │       ├── service.yaml             # Service
│   │       ├── route.yaml               # Route OpenShift
│   │       └── kubevirt-vm.yaml         # VirtualMachine KubeVirt + DataVolume
│   ├── application.properties           # MySQL local + déploiement réel
│   └── application-prod.properties      # Profil serveur (MySQL)
├── src/test/java/...                    # 61 tests unitaires (100 % passent)
└── pom.xml
```

---

## 5. Configuration et Dépendances

### Dépendances Maven principales (pom.xml)

- `spring-boot-starter-web` / `websocket` / `data-jpa` / `security` / `thymeleaf` / `validation`
- `org.springframework.ai:spring-ai-ollama-spring-boot-starter` — intégration Ollama (gratuit)
- `com.mysql:mysql-connector-j` — MySQL local (défaut) et prod
- `io.jsonwebtoken:jjwt-api/impl/jackson` (0.12.5) — JWT
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` (2.5.0) — Swagger
- `spring-boot-starter-test` + `spring-security-test` — tests

### Configuration (application.properties)

```properties
# Ollama (100% local et gratuit)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3
spring.ai.ollama.chat.options.temperature=0.2

# Base de données MySQL locale (XAMPP / phpMyAdmin)
spring.datasource.url=jdbc:mysql://localhost:3306/iac_chatbot?createDatabaseIfNotExist=true

# Déploiement RÉEL (VBoxManage / oc — chemins par défaut de la machine)
deploy.vboxmanage-path=${VBOXMANAGE_PATH:C:/Program Files/Oracle/VirtualBox/VBoxManage.exe}
deploy.oc-server=${OC_SERVER:https://127.0.0.1:16443}
deploy.oc-token=${OC_TOKEN:}

# JWT
jwt.secret=${JWT_SECRET:...}
jwt.expiration=86400000

# Swagger: http://localhost:8081/swagger-ui.html
```

---

## 6. Récapitulatif

Architecture moderne, enterprise-grade et **100 % gratuite** :

- **Frontend Angular « Geniac »** : interface réactive type ChatGPT/Claude — avatars, effet streaming,
  exemples cliquables, menu déroulant, progression temps réel
- **Backend Spring Boot** : API REST sécurisée JWT, support WebSocket temps réel
- **IA Ollama (Llama 3)** : extraction intelligente FR/EN, 100 % locale et offline
- **Templates Thymeleaf** : génération IaC standardisée — Terraform (vSphere), YAML (OpenShift), KubeVirt
- **Multi-plateforme** : VMware vSphere (VMs) et OpenShift (conteneurs + VMs KubeVirt) — **aucun cloud public**
- **Déploiement 100 % réel** : `RealDeployExecutor` avec étapes VALIDATION → PLAN → APPLY → VERIFY,
  exécution réelle (VBoxManage / oc), nommage unique des ressources (`resourceName`), annulation réelle
- **Persistance** : MySQL local (XAMPP) — historique conservé, consultation via phpMyAdmin
- **Traçabilité** : journalisation complète des demandes avec notifications

| Couche | Composants | Responsabilité |
|---|---|---|
| Présentation | Angular 17+, RxJS, WebSocket Client | Interface utilisateur, chatbot, dashboard |
| API Gateway | Spring Security, JWT Filter, CORS | Authentification, autorisation |
| Application | Spring Boot, Controllers, Services | Logique métier, orchestration |
| Intelligence | Ollama (Llama 3), Thymeleaf | NLP local, extraction, génération IaC |
| Données | Spring Data JPA, MySQL | Persistance, requêtes |
| Exécution | RealDeployExecutor (ProcessBuilder), VBoxManage, oc | Pipeline de déploiement réel |
| Infrastructure | VirtualBox (VMs), MicroShift/OpenShift (conteneurs), Terraform/oc (code généré) | Provisioning VMs, déploiement conteneurs |

---

*Document préparé pour validation par l'encadrant — Projet Geniac, version FINALE 4.0.*
