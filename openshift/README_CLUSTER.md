# 📂 Dossier openshift/ — Cluster MicroShift local

> Les fichiers de ce dossier sont exclus de Git pour des raisons de sécurité
> (token d'accès, binaire oc.exe). Voici comment reconstituer ce dossier.

---

## Contenu attendu

```
openshift/
├── oc.exe              → Client OpenShift (binaire Windows)
├── oc-token.txt        → Token d'authentification du cluster
├── pull-secret.json    → Pull secret Red Hat (compte developer gratuit)
└── seed/               → Fichiers cloud-init pour la VM AlmaLinux
```

---

## Comment obtenir oc.exe

Télécharger depuis le site officiel Red Hat :
https://mirror.openshift.com/pub/openshift-v4/clients/oc/latest/windows/

---

## Comment obtenir le token

Le token est généré automatiquement lors de l'installation de MicroShift.
Il est stocké dans la VM via :

```bash
# Dans la VM microshift (SSH)
kubectl create serviceaccount iac-backend -n default
kubectl create clusterrolebinding iac-backend-admin \
  --clusterrole=cluster-admin \
  --serviceaccount=default:iac-backend
kubectl create token iac-backend -n default --duration=87600h
```

Coller le résultat dans `oc-token.txt`.

---

## Informations du cluster

| Paramètre | Valeur |
|-----------|--------|
| VM VirtualBox | `microshift` |
| OS | AlmaLinux 9 |
| MicroShift | 4.18 |
| API Server | https://127.0.0.1:16443 |
| Port NAT API | 16443 → 6443 |
| Port NAT HTTP | 9080 → 80 |
| Port NAT HTTPS | 9443 → 443 |
| Port NAT SSH | 2222 → 22 |

---

## Démarrer le cluster

```powershell
# Démarrer la VM
& "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" startvm microshift --type headless

# Vérifier que le cluster est Ready (attendre ~2 min)
$token = Get-Content oc-token.txt
.\oc.exe get nodes --server=https://127.0.0.1:16443 --token=$token --insecure-skip-tls-verify=true
```

---

## Arrêter le cluster

```powershell
& "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" controlvm microshift acpipowerbutton
```
