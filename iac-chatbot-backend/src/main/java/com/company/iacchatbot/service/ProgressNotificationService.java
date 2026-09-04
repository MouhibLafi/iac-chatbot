package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service de notification temps reel via WebSocket STOMP.
 * Diffuse la progression de la generation IaC sur /topic/progress.
 */
@Service
public class ProgressNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ProgressNotificationService.class);
    private static final String PROGRESS_TOPIC = "/topic/progress";

    private final SimpMessagingTemplate messagingTemplate;

    public ProgressNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envoyer un evenement de progression a tous les clients connectes.
     */
    public void sendProgress(Long requestId, String step, int percent, String message) {
        ProgressEvent event = new ProgressEvent(requestId, step, percent, message);
        log.debug("Progression [{}] {}% - {}", step, percent, message);
        messagingTemplate.convertAndSend(PROGRESS_TOPIC, event);
    }
}
