package com.company.iacchatbot.controller;

import com.company.iacchatbot.model.DeploymentLog;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.LogLevel;
import com.company.iacchatbot.repository.DeploymentLogRepository;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.company.iacchatbot.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller d'administration : workflow d'approbation des demandes (UC-12).
 * Réservé aux utilisateurs ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final InfrastructureRequestRepository requestRepository;
    private final DeploymentLogRepository logRepository;
    private final NotificationService notificationService;

    public AdminController(InfrastructureRequestRepository requestRepository,
                           DeploymentLogRepository logRepository,
                           NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.logRepository = logRepository;
        this.notificationService = notificationService;
    }

    /**
     * Liste des demandes en attente d'approbation
     * GET /api/admin/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InfrastructureRequest>> getPendingRequests() {
        return ResponseEntity.ok(requestRepository.findByStatus("PENDING_APPROVAL"));
    }

    /**
     * Approuve une demande : statut -> CODE_GENERATED + log INFO
     * POST /api/admin/approve/{id}
     */
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id) {
        InfrastructureRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande d'infrastructure non trouvée avec l'ID: " + id));

        request.setStatus("CODE_GENERATED");
        request = requestRepository.save(request);
        logRepository.save(new DeploymentLog(request, "APPROVAL", LogLevel.INFO,
                "Demande approuvée par un administrateur"));

        log.info("Demande {} approuvée", id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", request.getId());
        body.put("status", request.getStatus());
        body.put("message", "Demande approuvée : déploiement autorisé");
        return ResponseEntity.ok(body);
    }

    /**
     * Rejette une demande : statut -> CANCELLED + errorMessage + log WARN + notification Discord
     * POST /api/admin/reject/{id}  body optionnel : {"reason": "..."}
     */
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                      @RequestBody(required = false) Map<String, String> payload) {
        InfrastructureRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande d'infrastructure non trouvée avec l'ID: " + id));

        String reason = (payload != null && payload.get("reason") != null)
                ? payload.get("reason")
                : "Rejetée par un administrateur";

        request.setStatus("CANCELLED");
        request.setErrorMessage(reason);
        request.setCompletedAt(LocalDateTime.now());
        request = requestRepository.save(request);
        logRepository.save(new DeploymentLog(request, "APPROVAL", LogLevel.WARN,
                "Demande rejetée : " + reason));
        notificationService.sendDiscordNotification(
                "🚫 Demande #" + id + " rejetée par un administrateur : " + reason);

        log.info("Demande {} rejetée: {}", id, reason);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", request.getId());
        body.put("status", request.getStatus());
        body.put("message", "Demande rejetée");
        body.put("reason", reason);
        return ResponseEntity.ok(body);
    }
}
