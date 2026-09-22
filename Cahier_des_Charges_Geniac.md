# CAHIER DES CHARGES — GENIAC
## Infrastructure as Code & Automation pilotée par Chatbot Intelligent

| Champ | Valeur |
|---|---|
| Projet | **Geniac** — Plateforme IaC multi-environnements avec chatbot conversationnel |
| Stagiaire | [Nom du stagiaire] |
| Entreprise | [Nom de l'entreprise] |
| Encadrant | [Nom de l'encadrant] |
| Durée du stage | 1 mois (20 jours ouvrés) |
| Version | **FINALE 4.0 — Version livrée (projet Geniac)** |
| Date | Septembre 2026 |
| Coût total | **0 €** (100 % outils gratuits et open-source) |

> **Note de version :** cette version décrit le projet **tel que livré**, sous son nom final **Geniac**,
> alignée sur le document officiel fourni par la société (*Infrastructure as Code & Automation*).
> Les plateformes cibles sont **VMware vSphere** et **OpenShift Container Platform**.
> Aucun cloud public (AWS, Azure, GCP) n'est utilisé. L'IA est **100 % locale** (Ollama + Llama 3).
> Les déploiements sont **réels** : le mode simulation a été retiré de la version finale.

---

## 1. Introduction

### 1.1 Contexte du projet

Dans le cadre de la transformation digitale, l'automatisation de l'infrastructure est un enjeu stratégique majeur.
L'Infrastructure as Code (IaC) permet de gérer et provisionner des ressources informatiques via des fichiers de
configuration, assurant reproductibilité, traçabilité et réduction des erreurs humaines. Cependant, cette approche
nécessite des compétences techniques avancées (Terraform, Kubernetes) qui ne sont pas toujours maîtrisées par
l'ensemble des équipes.

Ce projet vise à **démocratiser l'accès à l'IaC** en proposant une interface conversationnelle intelligente capable
de comprendre les besoins exprimés en langage naturel et de générer automatiquement le code infrastructure
approprié, en utilisant exclusivement des outils et ressources gratuits.

### 1.2 Objectifs du projet

- Automatiser le provisioning d'infrastructure (VMs et conteneurs) via un chatbot intelligent
- Supporter deux environnements : **VMware vSphere** (VMs classiques) et **OpenShift Container Platform**
  (conteneurs + VMs via OpenShift Virtualization / KubeVirt)
- Unifier la gestion des ressources VMware et OpenShift sous une seule interface
- Simplifier les opérations IT : les utilisateurs non-techniques déploient en langage naturel (FR/EN)
- Introduire l'IA dans l'automatisation cloud via un LLM **gratuit et local** (Ollama + Llama 3)
- Garantir la standardisation, la traçabilité et la fiabilité des déploiements
- Réaliser le projet à **coût zéro**

### 1.3 Périmètre du projet

Le projet couvre la conception et le développement d'une plateforme web complète :

- Un chatbot conversationnel (français/anglais) utilisant **Ollama (Llama 3)** — modèle local gratuit
- Un moteur d'extraction de paramètres basé sur ce LLM local (100 % offline, zéro coût API)
- Un générateur de code IaC :
  - **Terraform pour VMware vSphere** (VMs)
  - **YAML Kubernetes pour OpenShift** (conteneurs : Deployment, Service, Route)
  - **YAML KubeVirt pour OpenShift Virtualization** (VMs sur Kubernetes)
- Génération des templates via **Thymeleaf** (moteur de templates intégré à Spring Boot)
- Un système d'authentification sécurisée (**JWT**) avec gestion des rôles (USER / ADMIN)
- Un dashboard de suivi avec historique et statistiques
- Un système de **déploiement réel** intégré au backend (**RealDeployExecutor** : validation,
  exécution réelle VBoxManage/oc, vérification, journalisation)

### 1.4 Hors périmètre

- Support des clouds publics (**AWS, Azure, GCP**) — *explicitement exclus*
- Authentification SSO (LDAP, Active Directory, OAuth2 externe)
- Haute disponibilité multi-datacenter
- Facturation et gestion des coûts par utilisateur
- Monitoring avancé (Prometheus, Grafana)
- Services cloud payants (OpenAI API, SendGrid, etc.)

---

## 2. Analyse de l'existant

### 2.1 État actuel

| Environnement | Description | Outils actuels | Problématique |
|---|---|---|---|
| VMware vSphere | Datacenter virtualisé pour les VMs classiques | vCenter, templates manuels | Provisioning manuel, pas de standardisation |
| OpenShift | Plateforme Kubernetes pour conteneurs et VMs (KubeVirt) | oc CLI, manifestes YAML | Nécessite expertise Kubernetes |
| Gestion | Demandes traitées par les équipes infrastructure | Tickets, emails, scripts ad-hoc | Temps de réponse long, erreurs humaines |

### 2.2 Problématiques identifiées

- **Complexité technique** : maîtrise de Terraform, Kubernetes et des spécificités de chaque plateforme requise
- **Temps de déploiement** : 30 à 60 minutes en moyenne pour un déploiement manuel
- **Erreurs humaines** : configuration manuelle source d'incohérences
- **Manque de traçabilité** : difficile de savoir qui a déployé quoi et quand
- **Fragmentation** : deux outils distincts (VMware et OpenShift) sans interface unifiée
- **Dépendance aux équipes infra** pour chaque demande
- **Coût des outils** : solutions payantes non accessibles pour un stage

### 2.3 Opportunités

- **IA conversationnelle locale** : Ollama + Llama 3, compréhension fine du langage naturel sans coût API
- **Standardisation** : environnements reproductibles et identiques grâce à l'IaC
- **Autonomisation** : déploiement en libre-service sans expertise technique
- **Gain de temps** : réduction estimée de 90 % (de 30-60 min à 2-3 min)
- **Coût zéro** : tous les outils sont open-source ou gratuits

---

## 3. Besoins Fonctionnels

### 3.1 Identification des acteurs

| Acteur | Description | Compétences | Fréquence |
|---|---|---|---|
| Utilisateur | Développeur, ops ou chef de projet demandant des ressources | Aucune expertise IaC requise | Quotidienne |
| Administrateur | Responsable infrastructure supervisant la plateforme | Expertise VMware/OpenShift | Hebdomadaire |
| Moteur de déploiement | RealDeployExecutor exécutant le pipeline automatiquement | Automatique | À chaque déploiement |

### 3.2 Cas d'utilisation détaillés

**Module Authentification**

| ID | Cas d'utilisation | Description | Priorité | Acteur |
|---|---|---|---|---|
| UC-01 | S'authentifier | Connexion login/mot de passe, génération JWT, expiration 24h | Haute | Utilisateur, Admin |
| UC-02 | S'inscrire | Création de compte | Haute | Utilisateur |
| UC-03 | Se déconnecter | Fin de session côté client | Moyenne | Utilisateur, Admin |

**Module Chatbot (IA locale Ollama)**

| ID | Cas d'utilisation | Description | Priorité | Acteur |
|---|---|---|---|---|
| UC-04 | Demander un déploiement | Demande en langage naturel (FR/EN), extraction automatique des paramètres par Ollama (Llama 3) — 100 % offline | Haute | Utilisateur, Admin |
| UC-05 | Visualiser le code généré | Affichage du code Terraform/YAML avant confirmation, bouton copier | Haute | Utilisateur, Admin |
| UC-06 | Confirmer le déploiement | Validation utilisateur et déclenchement du déploiement réel | Haute | Utilisateur, Admin |
| UC-07 | Demander des précisions | Si la demande est ambiguë ou la combinaison invalide (ex : conteneur sur vSphere), le chatbot demande des clarifications | Moyenne | Système → Utilisateur |

**Module Suivi**

| ID | Cas d'utilisation | Description | Priorité | Acteur | Statut |
|---|---|---|---|---|---|
| UC-08 | Consulter l'historique | Demandes avec filtres (statut, plateforme), détail + code | Moyenne | Utilisateur, Admin | ✅ Implémenté |
| UC-09 | Consulter le dashboard | Statistiques : nombre de demandes, statut backend | Moyenne | Utilisateur, Admin | ✅ Implémenté |
| UC-10 | Annuler un déploiement | Suppression réelle des ressources (VM VirtualBox supprimée, manifestes OpenShift supprimés via oc delete) | Basse | Utilisateur, Admin | ✅ Implémenté |

**Module Administration**

| ID | Cas d'utilisation | Description | Priorité | Acteur | Statut |
|---|---|---|---|---|---|
| UC-11 | Gérer les utilisateurs | Liste, rôles, activation/désactivation, suppression (demandes détachées, historique conservé) | Moyenne | Admin | ✅ Implémenté |
| UC-12 | Valider les demandes | Approuver/Rejeter (activable via `app.approval.required`) | Moyenne | Admin | ✅ Implémenté |
| UC-13 | Configurer les quotas | Limites CPU/RAM/Stockage par utilisateur (bloquant à 403) | Moyenne | Admin | ✅ Implémenté |
| UC-14 | Statistiques globales | Vue d'ensemble de toutes les demandes | Basse | Admin | ✅ Implémenté |
| UC-16 | Configurer les notifications | Discord Webhook (déploiements, rejets) | Basse | Admin | ✅ Implémenté |

---

## 4. Besoins Non Fonctionnels

### 4.1 Performance

| Exigence | Critère | Valeur cible |
|---|---|---|
| Temps de réponse API | Latence endpoints REST | < 200 ms (p95) |
| Temps d'extraction LLM | Appel Ollama local | < 15 s (Llama 3 8B) |
| Temps de génération IaC | Rendu Thymeleaf | < 500 ms |
| Temps de déploiement total | De la demande à la ressource opérationnelle | < 5 minutes |
| Utilisateurs simultanés | Charge concurrente | 50+ utilisateurs |
| Disponibilité | Uptime du service | > 99 % (hors maintenance) |

### 4.2 Sécurité

- **Authentification** : JWT avec expiration configurable (24h par défaut)
- **Autorisation** : contrôle d'accès basé sur les rôles (USER / ADMIN) avec Spring Security
- **Validation des entrées** : sanitization des requêtes, protection XSS et injection SQL
- **Chiffrement** : mots de passe hashés avec BCrypt, communications HTTPS/TLS
- **Quotas** : limitation des ressources par utilisateur (CPU max 32, RAM max 128 Go, stockage max 1 To)
- **Audit** : journalisation des actions (qui, quoi, quand, résultat)
- **Secrets** : variables d'environnement pour tous les credentials — **aucun secret en dur**
- **Isolation** : Ollama fonctionne en local, aucune donnée utilisateur n'est envoyée à l'extérieur

### 4.3 Ergonomie et UX

- Interface responsive (mobile, tablette, desktop), design moderne type « glassmorphism »
- Chatbot style ChatGPT/Claude : avatars (logo Geniac pour l'assistant, initiale pour l'utilisateur),
  réponse de l'IA affichée progressivement (effet streaming), exemples de demandes cliquables,
  menu utilisateur déroulant dans l'en-tête
- Sélecteur de plateforme : **Auto** (détection par l'IA), **VMware vSphere**, **OpenShift**
- Feedback immédiat : progression temps réel (WebSocket), confirmations, messages d'erreur explicites
- Internationalisation : support Français et Anglais

### 4.4 Maintenabilité

- Code propre : principes SOLID, patterns Repository et Service
- Documentation : JavaDoc, README, Swagger/OpenAPI
- Tests : 61 tests unitaires automatisés (100 % passent), build Maven reproductible
- Versioning : Git

### 4.5 Scalabilité

Architecture permettant l'évolution horizontale : backend stateless (JWT), base de données externalisable
(MySQL), composants open-source. Le passage de VirtualBox/MicroShift à un vCenter/cluster d'entreprise
se fait par configuration, sans modification de code.

---

## 5. Architecture Technique

### 5.1 Stack technique (100 % GRATUITE)

| Couche | Technologie | Version | Justification | Coût |
|---|---|---|---|---|
| Frontend | Angular 17+ | 17+ | Framework complet, TypeScript, composants réactifs | 0 € |
| Backend | Spring Boot | 3.2+ | Standard enterprise, intégration native avec l'IA | 0 € |
| IA / LLM | **Ollama + Llama 3** | — | 100 % local et gratuit, offline, pas de quota | 0 € |
| Base de données | MySQL Community | 8.0+ | Open-source, fiable, consultation via phpMyAdmin (XAMPP) | 0 € |
| Templates | Thymeleaf | 3.1+ | Intégration native Spring Boot, mode TEXT pour IaC | 0 € |
| Exécution VMs (local) | VirtualBox (VBoxManage) | 7.2+ | Création réelle de VMs en local | 0 € |
| Exécution OpenShift (local) | MicroShift | 4.18 | Vrai cluster OpenShift en VM VirtualBox | 0 € |
| Déploiement | RealDeployExecutor (backend) | — | Pipeline réel : validation, apply, verify, journalisation | 0 € |
| Sécurité | Spring Security + JWT | 6.2+ | Authentification stateless | 0 € |
| Temps réel | WebSocket STOMP | — | Progression des déploiements en direct | 0 € |
| Notifications | Discord Webhook | — | 100 % gratuit, simple à intégrer (optionnel) | 0 € |
| Documentation API | Swagger UI (springdoc) | 2.5+ | Génération automatique, interactive | 0 € |

**COÛT TOTAL DE LA STACK : 0 €**

### 5.2 Architecture en couches

| Couche | Composants | Responsabilité |
|---|---|---|
| Présentation | Angular 17+, RxJS, WebSocket Client | Interface utilisateur, chatbot interactif, dashboard |
| API Gateway | Spring Security, JWT Filter, CORS | Authentification, autorisation, sécurisation endpoints |
| Application | Spring Boot, Controllers, Services | Logique métier, orchestration des flux |
| Intelligence | Ollama (Llama 3), Thymeleaf | NLP local, extraction paramètres, génération code IaC |
| Données | Spring Data JPA, MySQL (local/prod) | Persistance, requêtes |
| Exécution | RealDeployExecutor (ProcessBuilder), VBoxManage, oc | Pipeline de déploiement réel |
| Infrastructure | VirtualBox (VMs), MicroShift (OpenShift local) | VMs réelles, déploiement conteneurs réel |

---

## 6. Spécifications Détaillées

### 6.1 Module Chatbot (IA locale Ollama)

Le chatbot est le point d'entrée unique de la plateforme. Il utilise Ollama avec le modèle **Llama 3** pour
comprendre les requêtes en langage naturel (français et anglais) et extraire les paramètres techniques.
Avantage clé : **100 % offline, zéro coût API, données confidentielles locales**.

**Exemples de requêtes valides (validés en test E2E) :**

| Requête | Paramètres extraits | Code généré |
|---|---|---|
| « Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM » | VM, VSPHERE, cpu=2, ramGb=4, osImage=ubuntu-22.04 | Terraform vSphere |
| « Déploie un conteneur nginx avec 3 replicas sur OpenShift » | CONTAINER, OPENSHIFT, replicas=3, containerImage=nginx | YAML K8s (Deployment+Service+Route) |
| « Crée une VM Windows Server avec 100 Go de disque sur OpenShift » | VM, OPENSHIFT, storageGb=100, osImage=windows-server-2022 | YAML KubeVirt |
| « I want a CentOS VM with 8GB RAM and 200GB storage on VMware » | VM, VSPHERE, ramGb=8, storageGb=200, osImage=centos-9 | Terraform vSphere |

**Paramètres extraits :**

| Paramètre | Type | Contraintes | Valeur par défaut |
|---|---|---|---|
| resourceType | Enum | VM ou CONTAINER | VM |
| platform | Enum | VSPHERE ou OPENSHIFT | VSPHERE (VM), OPENSHIFT (CONTAINER) |
| cpu | Integer | 1 – 32 | 2 |
| ramGb | Integer | 1 – 128 (Go) | 4 |
| storageGb | Integer | 10 – 1000 (Go) | 50 |
| osImage | String | ubuntu-22.04, centos-9, windows-server-2022 | ubuntu-22.04 |
| network | String | Nom du réseau existant | default |
| replicas | Integer | 1 – 10 (conteneurs uniquement) | 1 |
| containerImage | String | nginx, mysql, etc. | nginx |

**Règle de détection de plateforme :**
- « VMware », « vSphere », « vCenter » → VSPHERE
- « OpenShift », « Kubernetes », « OCP », « KubeVirt » → OPENSHIFT
- Non mentionné : VM → VSPHERE ; CONTAINER → OPENSHIFT
- Le sélecteur de plateforme dans l'interface a priorité sur la détection automatique

### 6.2 Module Génération IaC (Thymeleaf)

Le générateur utilise **Thymeleaf en mode TEXT** pour produire le code IaC à partir des paramètres extraits.
Trois combinaisons sont supportées :

| Ressource | Plateforme | Templates | Sortie |
|---|---|---|---|
| VM | VSPHERE | `templates/terraform/vmware-vm.tf` | Terraform vSphere (provider, datacenter, datastore, network, clone) |
| CONTAINER | OPENSHIFT | `templates/openshift/deployment.yaml` + `service.yaml` + `route.yaml` | Manifestes Kubernetes/OpenShift |
| VM | OPENSHIFT | `templates/openshift/kubevirt-vm.yaml` | VirtualMachine KubeVirt + DataVolume (PVC) |

La combinaison **CONTAINER + VSPHERE** est rejetée avec un message de clarification (UC-07) :
les conteneurs se déploient sur OpenShift, les VMs classiques sur VMware vSphere.

### 6.3 Module Déploiement RÉEL (pipeline intégré au backend)

Le déploiement est orchestré par le backend (`DeployService` + `RealDeployExecutor`) — **100 % réel** :

1. **Validation** : vérification réelle (`VBoxManage --version`, `oc apply --dry-run=server`)
2. **Plan** : prévisualisation des ressources à créer
3. **Approbation** (optionnel) : validation manuelle par un administrateur
4. **Apply** : exécution réelle (`VBoxManage createvm/modifyvm/createmedium/storagectl/storageattach`, `oc apply`)
5. **Vérification** : contrôle post-déploiement (`showvminfo`, `oc get`)
6. **Journalisation** : logs persistés en base (DeploymentLog) + notifications temps réel (WebSocket)

**Nommage des VMs** : chaque VM reçoit un nom unique `iac-vm-xxx` (3 caractères aléatoires), stocké dans
le champ `resourceName` de la demande. Ce nom est réutilisé lors de l'annulation pour supprimer exactement
la bonne VM — aucune collision possible, même après redémarrage de la base.

**Annulation (UC-10)** : suppression réelle — `VBoxManage unregistervm --delete` pour les VMs,
`oc delete -f manifest.yaml` pour OpenShift.

### 6.4 Interfaces utilisateur

| Écran | Description | Composants |
|---|---|---|
| Page de connexion | Authentification JWT, logo Geniac animé | Formulaire login/mot de passe, onglet inscription |
| Chatbot | Interface conversationnelle type ChatGPT/Claude | Avatars, effet streaming, exemples cliquables, sélecteur de plateforme, code copiable, déploiement temps réel |
| Historique | Toutes les demandes | Filtres statut/plateforme, détail dépliable, code généré |
| Panel Admin | Gestion de la plateforme | Statistiques, utilisateurs (rôles, quotas, suppression), approbations |

---

## 7. Contraintes et Risques

### 7.1 Contraintes techniques

| Type | Contrainte | Impact | Mitigation |
|---|---|---|---|
| Accès | Accès VPN + credentials vCenter et OpenShift (si déploiement entreprise) | Bloquant si non fourni | Exécution locale réelle : VirtualBox + MicroShift |
| Hardware LLM | Ollama nécessite ~4-8 Go RAM et un CPU moderne | Performance réduite sur PC ancien | Modèle plus petit (Gemma 2B) en backup |
| Réseau | Firewall entre environnements dev et prod | Tests limités | Environnement de test dédié |
| Permissions | Droits limités sur VMware/OpenShift pour un stagiaire | Déploiement entreprise impossible | Exécution locale réelle + review admin |
| Temps | 1 mois (20 jours) pour MVP complet | Scope à ajuster | Priorisation fonctionnalités |

### 7.2 Risques identifiés

| Risque | Probabilité | Impact | Plan d'action |
|---|---|---|---|
| LLM local ne comprend pas la demande | Moyenne | Élevé | Prompt engineering, fallback valeurs par défaut, questions guidées |
| Échec d'un déploiement réel | Moyenne | Élevé | Étape VALIDATION préalable (dry-run), statut FAILED + log d'erreur explicite, annulation propre |
| Performance API lente | Faible | Moyen | Appels async, pagination |
| Sécurité (injection, fuite données) | Faible | Critique | Validation entrées, audit logs, Ollama local = données jamais externalisées |
| Ollama indisponible | Faible | Élevé | Backup : modèle plus petit ou free tier (Groq, Gemini) |

---

## 8. Planning et Livrables

### 8.1 Calendrier du stage (20 jours)

| Semaine | Jours | Objectifs | Livrables |
|---|---|---|---|
| Semaine 1 | 1-5 | Onboarding, installation environnement (Ollama, VirtualBox, MicroShift), architecture validée | Environnement prêt, repo Git, architecture validée |
| Semaine 2 | 6-10 | Backend (API, Ollama, générateur IaC vSphere/OpenShift/KubeVirt), authentification JWT, démo intermédiaire | Backend fonctionnel, chatbot opérationnel, démo S2 |
| Semaine 3 | 11-15 | Frontend Angular, dashboard, tests complets, documentation technique | Interface web, dashboard, tests, docs |
| Semaine 4 | 16-20 | Finalisation, rapport de stage, préparation démo finale, soutenance | Code final, rapport, démo, soutenance |

### 8.2 Livrables attendus

| Catégorie | Livrable | Description | Échéance |
|---|---|---|---|
| Code | Backend Spring Boot | API REST : auth, chatbot, déploiement réel, historique, admin | S2 |
| Code | Templates IaC | Terraform (vSphere) + YAML (OpenShift/KubeVirt) via Thymeleaf | S2 |
| Code | Frontend Angular | Interface Geniac (chatbot, historique, panel admin) | S3 |
| Code | Pipeline de déploiement | DeployService + RealDeployExecutor : validation, apply, verify, journalisation | S3 |
| Doc | README.md | Installation, démarrage rapide, configuration | S4 |
| Doc | Documentation API | Swagger/OpenAPI (springdoc) | S2 |
| Doc | Cahier des charges | Ce document (version finale Geniac) | S1 |
| Doc | Rapport de stage | Intro, analyse, conception, réalisation, bilan | S4 |
| Demo | Présentation + démo live | Scénario 5 min : texte → params → code → déploiement réel → dashboard | S4 |

### 8.3 Critères d'acceptation

- ✅ Fonctionnel : le chatbot (Ollama) comprend ≥ 85 % des requêtes en langage naturel (FR/EN) — *validé en test E2E*
- ✅ Fonctionnel : le code généré cible **VMware vSphere** (Terraform) et **OpenShift** (YAML/KubeVirt)
- ✅ Fonctionnel : **déploiement réel** — VMs créées dans VirtualBox, conteneurs déployés sur OpenShift (MicroShift), avec journalisation complète (DeploymentLog)
- ✅ Fonctionnel : **annulation réelle** — suppression effective des ressources créées
- ✅ Fonctionnel : historique persistant avec filtres, workflow d'approbation admin, quotas utilisateur
- ✅ Performance : génération IaC < 500 ms (rendu Thymeleaf)
- ✅ Sécurité : authentification JWT fonctionnelle, aucune fuite de credentials, aucun secret en dur
- ✅ Qualité : 61 tests automatisés (100 % passent), format d'erreur standardisé
- ✅ Documentation : README complet, API documentée (Swagger)
- ✅ Coût : aucun service payant, tous composants open-source ou gratuits

---

## 9. Glossaire

| Terme | Définition |
|---|---|
| Geniac | Nom de la plateforme : le « génie » qui transforme une phrase en infrastructure réelle |
| IaC | Infrastructure as Code : gestion de l'infrastructure via des fichiers de configuration versionnés |
| LLM | Large Language Model : modèle de langage (ex : Llama 3 via Ollama) |
| Ollama | Outil gratuit et open-source pour exécuter des LLM localement (100 % offline) |
| Llama 3 | Modèle LLM gratuit et open-source de Meta, comparable à GPT-3.5 |
| VM | Machine virtuelle : émulation d'un ordinateur complet sur un serveur physique |
| Conteneur | Unité logicielle légère encapsulant une application et ses dépendances |
| Kubernetes | Plateforme d'orchestration de conteneurs open-source |
| OpenShift | Distribution Kubernetes de Red Hat avec outils DevOps et virtualisation (KubeVirt) |
| KubeVirt | Extension Kubernetes gérant des machines virtuelles comme des pods |
| MicroShift | OpenShift local allégé de Red Hat (vrai cluster en VM) |
| VirtualBox | Virtualisation gratuite et open-source d'Oracle (VMs réelles en local) |
| Terraform | Outil IaC open-source de HashiCorp (utilisé ici avec le provider vSphere) |
| RealDeployExecutor | Moteur de déploiement réel du backend (exécute VBoxManage / oc) |
| JWT | JSON Web Token : tokens d'accès signés et vérifiables |
| WebSocket / STOMP | Communication bidirectionnelle temps réel / protocole de messagerie associé |
| Thymeleaf | Moteur de templates Java (utilisé ici en mode TEXT pour générer Terraform/YAML) |
| MySQL Community | Base de données gratuite (locale XAMPP / prod) |
| Discord Webhook | Notifications gratuites (optionnelles) |

---

## 10. Récapitulatif

Ce cahier des charges définit la version **finale livrée** du projet **Geniac**, alignée sur le document officiel
de la société. Points clés :

- ✅ **Deux plateformes uniquement** : VMware vSphere (VMs) et OpenShift (conteneurs + VMs KubeVirt)
- ✅ **IA 100 % locale** : Ollama + Llama 3 — zéro coût, zéro fuite de données
- ✅ **Déploiement 100 % réel** : VMs VirtualBox et conteneurs OpenShift créés/supprimés pour de vrai
- ✅ **Génération IaC via Thymeleaf** : templates versionnés, standardisés, réutilisables
- ✅ **Coût total : 0 €** — économie estimée ~18 000 $/an par rapport à une stack payante
- ✅ **Traçabilité** : historique des demandes et logs persistés en base de données

| Composant | Version payante | Version gratuite (ce projet) | Économie |
|---|---|---|---|
| IA / LLM | OpenAI API (~$20-50/mois) | Ollama + Llama 3 (local, offline) | $20-50/mois |
| BDD prod | MySQL Enterprise (~$2000/an) | MySQL Community | $2000/an |
| CI/CD | Jenkins Enterprise (~$10000/an) | Pipeline intégré au backend (RealDeployExecutor) | $10000/an |
| VMware (local) | vSphere License (~$1000+/an) | VirtualBox | $1000+/an |
| OpenShift (local) | Red Hat License (~$5000/an) | MicroShift (gratuit, compte Red Hat Developer) | $5000+/an |

**ÉCONOMIE TOTALE ESTIMÉE : ~18 000 $/an — COÛT TOTAL DU PROJET : 0 €**

---

*Document préparé par : [Nom du stagiaire]*
*Entreprise : [Nom de l'entreprise] — Encadrant : [Nom de l'encadrant]*
*Version : FINALE 4.0 — Septembre 2026 — Projet Geniac, alignée sur le document officiel de la société*
