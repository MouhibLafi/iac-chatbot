package com.company.iacchatbot.controller;

import com.company.iacchatbot.dto.ChatRequest;
import com.company.iacchatbot.dto.ChatResponse;
import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.exception.QuotaExceededException;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.company.iacchatbot.repository.UserRepository;
import com.company.iacchatbot.service.IaCGeneratorService;
import com.company.iacchatbot.service.LlmService;
import com.company.iacchatbot.service.ProgressNotificationService;
import com.company.iacchatbot.service.QuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller principal du chatbot IaC
 * Plateformes: VMware vSphere (VMs) et OpenShift (conteneurs + VMs KubeVirt)
 */
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    private final LlmService llmService;
    private final IaCGeneratorService iaCGeneratorService;
    private final InfrastructureRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final ProgressNotificationService progressService;
    private final QuotaService quotaService;
    private final UserRepository userRepository;

    /**
     * Si true, les demandes générées passent en PENDING_APPROVAL (workflow d'approbation UC-12)
     */
    @Value("${app.approval.required:false}")
    private boolean approvalRequired;

    public ChatbotController(
            LlmService llmService,
            IaCGeneratorService iaCGeneratorService,
            InfrastructureRequestRepository repository,
            ObjectMapper objectMapper,
            ProgressNotificationService progressService,
            QuotaService quotaService,
            UserRepository userRepository) {
        this.llmService = llmService;
        this.iaCGeneratorService = iaCGeneratorService;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.progressService = progressService;
        this.quotaService = quotaService;
        this.userRepository = userRepository;
    }

    /**
     * Endpoint principal : traiter un message utilisateur
     */
    @PostMapping("/process")
    public ResponseEntity<ChatResponse> processMessage(@RequestBody ChatRequest request) {
        log.info("Reception requete: {}", request);

        // Id temporaire pour le suivi temps reel avant la premiere sauvegarde
        Long trackingId = System.currentTimeMillis();

        try {
            // 1. Extraire les parametres via Ollama (Llama 3)
            progressService.sendProgress(trackingId, "EXTRACTION", 10, "Analyse de votre demande par l'IA (Ollama/Llama 3)...");
            ExtractedParameters params = llmService.extractParameters(request.getMessage());

            // 2. La plateforme choisie dans l'interface a priorite sur celle detectee par le LLM
            PlatformType requestedPlatform = parsePlatform(request.getTargetPlatform());
            if (requestedPlatform != null) {
                params.setPlatform(requestedPlatform);
            }

            // 3. Utilisateur courant (peut etre null hors contexte authentifie)
            User currentUser = getCurrentUser();

            // 4. Verification des quotas AVANT generation (UC-13)
            if (currentUser != null) {
                quotaService.checkQuota(currentUser, params);
            }

            // 5. Creer et sauvegarder la demande (liee a l'utilisateur si authentifie)
            InfrastructureRequest infraRequest = new InfrastructureRequest(request.getMessage());
            infraRequest.setUser(currentUser);
            infraRequest.setTargetPlatform(params.getPlatform().name());
            infraRequest.setResourceType(params.getResourceType());
            infraRequest.setStatus("processing");
            infraRequest.setExtractedParams(objectMapper.writeValueAsString(params));
            infraRequest = repository.save(infraRequest);
            progressService.sendProgress(infraRequest.getId(), "EXTRACTION", 40,
                    "Parametres detectes: " + params.getResourceType() + " sur " + params.getPlatform());

            // 6. Generer le code IaC (Terraform vSphere / YAML OpenShift / YAML KubeVirt)
            progressService.sendProgress(infraRequest.getId(), "GENERATION", 60, "Generation du code IaC (Thymeleaf)...");
            String generatedCode = iaCGeneratorService.generateCode(params);
            infraRequest.setGeneratedCode(generatedCode);
            // Workflow d'approbation (UC-12) : PENDING_APPROVAL si active, sinon comportement historique
            infraRequest.setStatus(approvalRequired ? "PENDING_APPROVAL" : "completed");
            infraRequest.setProcessedAt(LocalDateTime.now());
            infraRequest = repository.save(infraRequest);
            progressService.sendProgress(infraRequest.getId(), "COMPLETED", 100,
                    approvalRequired ? "Code genere - en attente d'approbation admin" : "Code IaC genere avec succes !");

            // 7. Construire la reponse
            ChatResponse response = new ChatResponse();
            response.setRequestId(infraRequest.getId());
            response.setStatus(approvalRequired ? "pending_approval" : "success");
            response.setExtractedParams(params);
            response.setGeneratedCode(generatedCode);
            response.setMessage(buildSuccessMessage(params));

            log.info("Requete traitee avec succes: {}", infraRequest.getId());

            return ResponseEntity.ok(response);

        } catch (QuotaExceededException e) {
            // Quota depasse (UC-13) -> 403
            log.warn("Quota depasse: {}", e.getMessage());
            progressService.sendProgress(trackingId, "ERROR", 100, "Quota depasse");
            ChatResponse quotaResponse = new ChatResponse("quota_exceeded", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(quotaResponse);

        } catch (IllegalArgumentException e) {
            // Combinaison ressource/plateforme invalide -> demander clarification
            log.warn("Combinaison invalide: {}", e.getMessage());
            progressService.sendProgress(trackingId, "ERROR", 100, "Clarification necessaire");
            ChatResponse clarifyResponse = new ChatResponse("clarification_needed", e.getMessage());
            return ResponseEntity.badRequest().body(clarifyResponse);

        } catch (Exception e) {
            log.error("Erreur lors du traitement", e);
            progressService.sendProgress(trackingId, "ERROR", 100, "Erreur: " + e.getMessage());
            ChatResponse errorResponse = new ChatResponse("error", "Erreur: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Recupere l'utilisateur courant depuis le SecurityContext (null si non authentifie)
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    /**
     * Parser la plateforme demandee par l'utilisateur (null = auto-detection par le LLM)
     */
    private PlatformType parsePlatform(String targetPlatform) {
        if (targetPlatform == null || targetPlatform.isBlank() || targetPlatform.equalsIgnoreCase("auto")) {
            return null;
        }
        return switch (targetPlatform.toLowerCase()) {
            case "vsphere", "vmware" -> PlatformType.VSPHERE;
            case "openshift", "ocp", "kubernetes" -> PlatformType.OPENSHIFT;
            default -> null;
        };
    }

    /**
     * Message de succes adapte a la plateforme
     */
    private String buildSuccessMessage(ExtractedParameters params) {
        if (params.getPlatform() == PlatformType.VSPHERE) {
            return "Code Terraform genere avec succes pour VMware vSphere !";
        }
        if (params.getResourceType() == ResourceType.VM) {
            return "Manifeste KubeVirt genere avec succes pour OpenShift Virtualization !";
        }
        return "Manifestes OpenShift generes avec succes (Deployment + Service + Route) !";
    }

    /**
     * Recuperer toutes les demandes
     */
    @GetMapping("/requests")
    public ResponseEntity<List<InfrastructureRequest>> getAllRequests() {
        List<InfrastructureRequest> requests = repository.findAll();
        return ResponseEntity.ok(requests);
    }

    /**
     * Recuperer une demande par ID
     */
    @GetMapping("/requests/{id}")
    public ResponseEntity<InfrastructureRequest> getRequest(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recuperer les demandes par statut
     */
    @GetMapping("/requests/status/{status}")
    public ResponseEntity<List<InfrastructureRequest>> getRequestsByStatus(@PathVariable String status) {
        List<InfrastructureRequest> requests = repository.findByStatus(status);
        return ResponseEntity.ok(requests);
    }

    /**
     * Endpoint de test simple
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chatbot API is running!");
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
