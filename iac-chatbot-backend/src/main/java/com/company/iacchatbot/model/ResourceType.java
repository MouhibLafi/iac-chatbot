package com.company.iacchatbot.model;

/**
 * Type de ressource d'infrastructure (document officiel societe)
 */
public enum ResourceType {
    VM,         // Machine Virtuelle (vSphere ou KubeVirt sur OpenShift)
    CONTAINER   // Conteneur (OpenShift / Kubernetes)
}
