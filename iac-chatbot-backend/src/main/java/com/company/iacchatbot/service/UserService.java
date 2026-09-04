package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.QuotaRequest;
import com.company.iacchatbot.dto.UserDto;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des utilisateurs
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Récupère tous les utilisateurs
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un utilisateur par son ID
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        return convertToDto(user);
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur
     */
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec le nom: " + username));
        return convertToDto(user);
    }

    /**
     * Supprime un utilisateur
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        userRepository.delete(user);
    }

    /**
     * Active ou désactive un utilisateur
     */
    @Transactional
    public UserDto toggleUserEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        user.setEnabled(!user.getEnabled());
        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    /**
     * Met à jour les quotas de ressources d'un utilisateur (UC-13)
     */
    @Transactional
    public UserDto updateQuota(Long id, QuotaRequest quotaRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        if (quotaRequest.getQuotaCpu() != null) {
            user.setQuotaCpu(quotaRequest.getQuotaCpu());
        }
        if (quotaRequest.getQuotaRam() != null) {
            user.setQuotaRam(quotaRequest.getQuotaRam());
        }
        if (quotaRequest.getQuotaStorage() != null) {
            user.setQuotaStorage(quotaRequest.getQuotaStorage());
        }
        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    /**
     * Convertit une entité User en UserDto
     */
    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
