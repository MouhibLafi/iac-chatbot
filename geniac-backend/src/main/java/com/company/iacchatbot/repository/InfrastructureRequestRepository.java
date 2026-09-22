package com.company.iacchatbot.repository;

import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les demandes d'infrastructure
 */
@Repository
public interface InfrastructureRequestRepository extends JpaRepository<InfrastructureRequest, Long> {

    /**
     * Trouver toutes les demandes par statut
     */
    List<InfrastructureRequest> findByStatus(String status);

    /**
     * Trouver toutes les demandes par type de ressource
     */
    List<InfrastructureRequest> findByResourceType(ResourceType resourceType);

    /**
     * Trouver toutes les demandes par plateforme cible
     */
    List<InfrastructureRequest> findByTargetPlatform(String targetPlatform);

    /**
     * Trouver toutes les demandes d'un utilisateur (plus récentes d'abord)
     */
    List<InfrastructureRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Trouver les demandes d'un utilisateur par statut (historique + quotas)
     */
    List<InfrastructureRequest> findByUserIdAndStatus(Long userId, String status);

    /**
     * Trouver toutes les demandes triées par date décroissante (admin)
     */
    List<InfrastructureRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Toutes les demandes avec l'utilisateur chargé (évite le lazy loading
     * lors de la sérialisation du champ username — vue admin)
     */
    @Query("SELECT r FROM InfrastructureRequest r LEFT JOIN FETCH r.user ORDER BY r.createdAt DESC")
    List<InfrastructureRequest> findAllWithUserOrderByCreatedAtDesc();

    /**
     * Demandes d'un utilisateur avec l'utilisateur chargé (champ username)
     */
    @Query("SELECT r FROM InfrastructureRequest r LEFT JOIN FETCH r.user WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<InfrastructureRequest> findByUserIdWithUserOrderByCreatedAtDesc(Long userId);
}
