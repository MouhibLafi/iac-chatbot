package com.company.iacchatbot.controller;

import com.company.iacchatbot.dto.MessageResponse;
import com.company.iacchatbot.dto.QuotaRequest;
import com.company.iacchatbot.dto.UserDto;
import com.company.iacchatbot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des utilisateurs (ADMIN uniquement)
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Récupère tous les utilisateurs (ADMIN uniquement)
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Récupère un utilisateur par son ID (ADMIN uniquement)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDto user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    /**
     * Supprime un utilisateur (ADMIN uniquement)
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(new MessageResponse("Utilisateur supprimé avec succès!"));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    /**
     * Active/Désactive un utilisateur (ADMIN uniquement)
     * PUT /api/users/{id}/toggle
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserEnabled(@PathVariable Long id) {
        try {
            UserDto user = userService.toggleUserEnabled(id);
            String message = user.getEnabled() ? "Utilisateur activé" : "Utilisateur désactivé";
            return ResponseEntity.ok(new MessageResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    /**
     * Met à jour les quotas de ressources d'un utilisateur (ADMIN uniquement, UC-13)
     * PUT /api/users/{id}/quota
     * Body : {"quotaCpu": 32, "quotaRam": 128, "quotaStorage": 1000}
     */
    @PutMapping("/{id}/quota")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest quotaRequest) {
        try {
            userService.updateQuota(id, quotaRequest);
            return ResponseEntity.ok(new MessageResponse("Quotas mis à jour avec succès!"));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }
}
