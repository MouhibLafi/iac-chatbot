package com.company.iacchatbot.model;

/**
 * Statuts possibles d'un déploiement.
 * Note : le champ status de InfrastructureRequest reste une String
 * pour compatibilité avec le frontend et les tests existants,
 * mais les valeurs de cette enum servent de référence pour le workflow.
 */
public enum DeploymentStatus {
    PENDING,
    CODE_GENERATED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}
