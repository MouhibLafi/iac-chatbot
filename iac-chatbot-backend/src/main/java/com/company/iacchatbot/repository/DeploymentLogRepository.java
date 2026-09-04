package com.company.iacchatbot.repository;

import com.company.iacchatbot.model.DeploymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les logs de déploiement (traçabilité)
 */
@Repository
public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {

    /**
     * Trouver tous les logs d'une demande de déploiement (ordre chronologique)
     */
    List<DeploymentLog> findByDeploymentRequestIdOrderByTimestampAsc(Long deploymentRequestId);
}
