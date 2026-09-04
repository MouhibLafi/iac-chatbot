package com.company.iacchatbot.controller;

import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.Role;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.company.iacchatbot.repository.UserRepository;
import com.company.iacchatbot.service.DeployService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller de déploiement (UC-06, UC-10).
 * Mode simulation ou réel selon la propriété deploy.mode.
 * L'utilisateur doit être propriétaire de la demande ou ADMIN.
 */
@RestController
@RequestMapping("/api/deploy")
@CrossOrigin(origins = "*")
public class DeployController {

    private static final Logger log = LoggerFactory.getLogger(DeployController.class);

    private final DeployService deployService;
    private final InfrastructureRequestRepository requestRepository;
    private final UserRepository userRepository;

    public DeployController(DeployService deployService,
                            InfrastructureRequestRepository requestRepository,
                            UserRepository userRepository) {
        this.deployService = deployService;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lance un déploiement (simulation ou réel selon deploy.mode)
     * POST /api/deploy/{requestId}
     */
    @PostMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> deploy(@PathVariable Long requestId) {
        checkOwnership(requestId);
        InfrastructureRequest request = deployService.deploy(requestId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", request.getId());
        body.put("status", request.getStatus());
        body.put("message", "Déploiement terminé avec succès");
        body.put("completedAt", request.getCompletedAt());
        return ResponseEntity.ok(body);
    }

    /**
     * Statut et logs d'un déploiement
     * GET /api/deploy/{requestId}/status
     */
    @GetMapping("/{requestId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long requestId) {
        checkOwnership(requestId);
        InfrastructureRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande d'infrastructure non trouvée avec l'ID: " + requestId));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", request.getId());
        body.put("status", request.getStatus());
        body.put("logs", deployService.getLogs(requestId));
        return ResponseEntity.ok(body);
    }

    /**
     * Annule un déploiement (suppression réelle ou simulée des ressources)
     * DELETE /api/deploy/{requestId}
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long requestId) {
        checkOwnership(requestId);
        InfrastructureRequest request = deployService.cancel(requestId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", request.getId());
        body.put("status", request.getStatus());
        body.put("message", "Déploiement annulé (ressources supprimées)");
        return ResponseEntity.ok(body);
    }

    /**
     * Vérifie que l'utilisateur courant est propriétaire de la demande ou ADMIN
     */
    private void checkOwnership(Long requestId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return; // les admins ont accès à toutes les demandes
        }
        InfrastructureRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande d'infrastructure non trouvée avec l'ID: " + requestId));
        if (request.getUser() != null && !request.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas propriétaire de cette demande");
        }
    }

    /**
     * Récupère l'utilisateur courant depuis le SecurityContext
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
