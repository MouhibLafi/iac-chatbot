package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service pour l'extraction de parametres via Ollama (Llama 3)
 * 100% local et offline - zero cout API
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LlmService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Extraire les parametres d'infrastructure a partir d'un message utilisateur
     */
    public ExtractedParameters extractParameters(String userMessage) {
        log.info("Extraction des parametres depuis: {}", userMessage);

        String prompt = buildExtractionPrompt(userMessage);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Reponse Ollama: {}", response);

            return parseResponse(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'extraction des parametres", e);
            throw new RuntimeException("Erreur d'extraction des parametres: " + e.getMessage());
        }
    }

    /**
     * Construire le prompt pour Ollama (FR/EN)
     * Plateformes: VMware vSphere (VMs) et OpenShift (conteneurs + VMs KubeVirt)
     */
    private String buildExtractionPrompt(String userMessage) {
        return """
                Tu es un assistant expert en infrastructure cloud (VMware vSphere et OpenShift).
                Analyse la demande utilisateur et extrais les parametres techniques.

                Message utilisateur: "%s"

                Reponds UNIQUEMENT avec un objet JSON valide contenant ces champs (mets null si non mentionne):
                {
                  "resourceType": "VM" ou "CONTAINER",
                  "platform": "VSPHERE" ou "OPENSHIFT",
                  "osImage": "ubuntu-22.04, centos-9, windows-server-2022, debian-12, etc.",
                  "cpu": nombre de CPUs (1-32),
                  "ramGb": RAM en GB (1-128),
                  "storageGb": stockage en GB (10-1000),
                  "replicas": nombre de replicas (pour containers, 1-10),
                  "containerImage": "nginx, mysql, postgres, etc.",
                  "network": "nom du reseau"
                }

                Regles de detection de la plateforme:
                - Si l'utilisateur mentionne "VMware", "vSphere" ou "vCenter" -> VSPHERE
                - Si l'utilisateur mentionne "OpenShift", "Kubernetes", "OCP" ou "KubeVirt" -> OPENSHIFT
                - Si non mentionne: VM -> VSPHERE, CONTAINER -> OPENSHIFT

                Exemples:
                - "Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM"
                  -> {"resourceType":"VM","platform":"VSPHERE","cpu":2,"ramGb":4,"osImage":"ubuntu-22.04"}
                - "Deploie un conteneur nginx avec 3 replicas sur OpenShift"
                  -> {"resourceType":"CONTAINER","platform":"OPENSHIFT","replicas":3,"containerImage":"nginx"}
                - "Cree une VM Windows Server avec 100 Go de disque sur OpenShift"
                  -> {"resourceType":"VM","platform":"OPENSHIFT","storageGb":100,"osImage":"windows-server-2022"}
                - "I want a CentOS VM with 8GB RAM and 200GB storage on VMware"
                  -> {"resourceType":"VM","platform":"VSPHERE","ramGb":8,"storageGb":200,"osImage":"centos-9"}

                Reponds UNIQUEMENT avec le JSON, sans texte avant ou apres.
                """.formatted(userMessage);
    }

    /**
     * Parser la reponse JSON d'Ollama (avec fallback sur valeurs par defaut)
     */
    private ExtractedParameters parseResponse(String response) {
        try {
            // Nettoyer la reponse (enlever les ```json si presents)
            String cleanedResponse = response.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

            // Extraire uniquement le bloc JSON si du texte l'entoure
            int start = cleanedResponse.indexOf('{');
            int end = cleanedResponse.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleanedResponse = cleanedResponse.substring(start, end + 1);
            }

            log.debug("JSON nettoye: {}", cleanedResponse);

            ExtractedParameters params = objectMapper.readValue(cleanedResponse, ExtractedParameters.class);
            applyDefaults(params);
            return params;

        } catch (Exception e) {
            log.error("Erreur de parsing JSON: {}", response, e);

            // Fallback: VM vSphere par defaut
            ExtractedParameters params = new ExtractedParameters();
            params.setResourceType(ResourceType.VM);
            applyDefaults(params);
            return params;
        }
    }

    /**
     * Appliquer les valeurs par defaut du document officiel
     */
    private void applyDefaults(ExtractedParameters params) {
        if (params.getResourceType() == null) {
            params.setResourceType(ResourceType.VM);
        }
        // Plateforme par defaut selon le type de ressource
        if (params.getPlatform() == null) {
            params.setPlatform(params.getResourceType() == ResourceType.CONTAINER
                    ? PlatformType.OPENSHIFT
                    : PlatformType.VSPHERE);
        }
        if (params.getCpu() == null) params.setCpu(2);
        if (params.getRamGb() == null) params.setRamGb(4);
        if (params.getStorageGb() == null) params.setStorageGb(50);
        if (params.getOsImage() == null) params.setOsImage("ubuntu-22.04");
        if (params.getNetwork() == null) params.setNetwork("default");
        if (params.getReplicas() == null) params.setReplicas(1);
    }
}
