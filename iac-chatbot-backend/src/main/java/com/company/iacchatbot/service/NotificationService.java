package com.company.iacchatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service de notification externe (Discord webhook).
 * Si l'URL du webhook n'est pas configurée, la notification est simplement loguée.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * URL du webhook Discord (vide par défaut = notifications désactivées)
     */
    @Value("${DISCORD_WEBHOOK_URL:}")
    private String discordWebhookUrl;

    /**
     * Envoie une notification Discord. Si aucune URL n'est configurée, log INFO seulement.
     */
    public void sendDiscordNotification(String message) {
        if (discordWebhookUrl == null || discordWebhookUrl.isBlank()) {
            log.info("Notification Discord (webhook non configuré): {}", message);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request =
                    new HttpEntity<>(Map.of("content", message), headers);
            restTemplate.postForEntity(discordWebhookUrl, request, String.class);
            log.info("Notification Discord envoyée: {}", message);
        } catch (Exception e) {
            log.warn("Echec de l'envoi de la notification Discord: {}", e.getMessage());
        }
    }
}
