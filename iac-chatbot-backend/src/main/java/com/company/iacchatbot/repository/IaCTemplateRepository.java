package com.company.iacchatbot.repository;

import com.company.iacchatbot.model.IaCTemplate;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les templates IaC versionnés
 */
@Repository
public interface IaCTemplateRepository extends JpaRepository<IaCTemplate, Long> {

    /**
     * Trouver les templates actifs par type de ressource et plateforme
     */
    List<IaCTemplate> findByResourceTypeAndPlatformAndActiveTrue(ResourceType resourceType, PlatformType platform);

    /**
     * Trouver un template actif par son nom
     */
    Optional<IaCTemplate> findByNameAndActiveTrue(String name);
}
