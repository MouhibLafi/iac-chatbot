package com.company.iacchatbot.controller;

import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.company.iacchatbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller d'historique des demandes (UC-08).
 * - /api/history : demandes de l'utilisateur courant
 * - /api/history/all : toutes les demandes (ADMIN)
 * Filtres optionnels : status, platform (targetPlatform)
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    private final InfrastructureRequestRepository requestRepository;
    private final UserRepository userRepository;

    public HistoryController(InfrastructureRequestRepository requestRepository,
                             UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    /**
     * Historique des demandes de l'utilisateur courant (plus récentes d'abord)
     * GET /api/history?status=SUCCESS&platform=VSPHERE
     */
    @GetMapping
    public ResponseEntity<List<InfrastructureRequest>> getMyHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        List<InfrastructureRequest> requests =
                requestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        return ResponseEntity.ok(applyFilters(requests, status, platform));
    }

    /**
     * Historique de toutes les demandes (ADMIN uniquement)
     * GET /api/history/all?status=SUCCESS&platform=OPENSHIFT
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InfrastructureRequest>> getAllHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform) {
        List<InfrastructureRequest> requests = requestRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(applyFilters(requests, status, platform));
    }

    /**
     * Applique les filtres optionnels status / platform
     */
    private List<InfrastructureRequest> applyFilters(List<InfrastructureRequest> requests,
                                                     String status, String platform) {
        return requests.stream()
                .filter(r -> status == null || status.isBlank()
                        || status.equalsIgnoreCase(r.getStatus()))
                .filter(r -> platform == null || platform.isBlank()
                        || platform.equalsIgnoreCase(r.getTargetPlatform()))
                .collect(Collectors.toList());
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
