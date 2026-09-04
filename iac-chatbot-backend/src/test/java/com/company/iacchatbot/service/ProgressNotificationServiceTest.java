package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ProgressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du ProgressNotificationService
 */
@ExtendWith(MockitoExtension.class)
class ProgressNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ProgressNotificationService progressNotificationService;

    @BeforeEach
    void setUp() {
        progressNotificationService = new ProgressNotificationService(messagingTemplate);
    }

    @Test
    void testSendProgressInitial() {
        // Arrange
        Long requestId = 1L;
        String step = "INITIALIZATION";
        int percent = 0;
        String message = "Initializing deployment";

        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        
        ProgressEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals(requestId, event.getRequestId());
        assertEquals(step, event.getStep());
        assertEquals(percent, event.getPercent());
        assertEquals(message, event.getMessage());
    }

    @Test
    void testSendProgressMidway() {
        // Arrange
        Long requestId = 2L;
        String step = "GENERATION";
        int percent = 50;
        String message = "Generating infrastructure code";

        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        
        ProgressEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals(requestId, event.getRequestId());
        assertEquals(step, event.getStep());
        assertEquals(percent, event.getPercent());
        assertEquals(message, event.getMessage());
    }

    @Test
    void testSendProgressCompletion() {
        // Arrange
        Long requestId = 3L;
        String step = "COMPLETION";
        int percent = 100;
        String message = "Deployment completed successfully";

        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        
        ProgressEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals(percent, 100);
        assertEquals(message, event.getMessage());
    }

    @Test
    void testSendProgressMultipleEvents() {
        // Arrange
        Long requestId = 4L;

        // Act
        progressNotificationService.sendProgress(requestId, "STEP1", 25, "First step");
        progressNotificationService.sendProgress(requestId, "STEP2", 50, "Second step");
        progressNotificationService.sendProgress(requestId, "STEP3", 75, "Third step");
        progressNotificationService.sendProgress(requestId, "STEP4", 100, "Fourth step");

        // Assert
        verify(messagingTemplate, times(4)).convertAndSend(eq("/topic/progress"), any(ProgressEvent.class));
    }

    @Test
    void testSendProgressWithZeroPercent() {
        // Arrange
        Long requestId = 5L;
        String step = "START";
        int percent = 0;
        String message = "Starting process";

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        ProgressEvent event = captor.getValue();
        assertEquals(0, event.getPercent());
    }

    @Test
    void testSendProgressWithFullPercent() {
        // Arrange
        Long requestId = 6L;
        String step = "DONE";
        int percent = 100;
        String message = "Process completed";

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        ProgressEvent event = captor.getValue();
        assertEquals(100, event.getPercent());
    }

    @Test
    void testSendProgressEmptyMessage() {
        // Arrange
        Long requestId = 7L;
        String step = "PROCESSING";
        int percent = 50;
        String message = "";

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        ProgressEvent event = captor.getValue();
        assertEquals("", event.getMessage());
    }

    @Test
    void testSendProgressComplexMessage() {
        // Arrange
        Long requestId = 8L;
        String step = "VALIDATION";
        int percent = 75;
        String message = "Validating configuration (CPU: 32, RAM: 128GB, Storage: 1TB)";

        // Act
        progressNotificationService.sendProgress(requestId, step, percent, message);

        // Assert
        ArgumentCaptor<ProgressEvent> captor = ArgumentCaptor.forClass(ProgressEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/progress"), captor.capture());
        ProgressEvent event = captor.getValue();
        assertEquals(message, event.getMessage());
        assertTrue(event.getMessage().contains("CPU"));
    }
}
