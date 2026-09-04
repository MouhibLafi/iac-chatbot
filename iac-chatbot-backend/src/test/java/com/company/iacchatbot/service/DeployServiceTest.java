package com.company.iacchatbot.service;

import com.company.iacchatbot.model.DeploymentLog;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.ResourceType;
import com.company.iacchatbot.repository.DeploymentLogRepository;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du DeployService (déploiement simulé - UC-06, UC-10)
 */
@ExtendWith(MockitoExtension.class)
class DeployServiceTest {

    @Mock
    private InfrastructureRequestRepository requestRepository;

    @Mock
    private DeploymentLogRepository logRepository;

    @Mock
    private ProgressNotificationService progressService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DeployService deployService;

    private InfrastructureRequest demande(String status) {
        InfrastructureRequest req = new InfrastructureRequest("VM Ubuntu 2 CPU");
        req.setId(1L);
        req.setStatus(status);
        req.setResourceType(ResourceType.VM);
        req.setTargetPlatform("VSPHERE");
        return req;
    }

    @Test
    void deploy_deploiementSimuleReussi_statutSuccessEtLogsCrees() {
        InfrastructureRequest req = demande("CODE_GENERATED");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(InfrastructureRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InfrastructureRequest result = deployService.deploy(1L);

        // Statut final SUCCESS + date de fin renseignée
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getCompletedAt());

        // Logs de traçabilité créés (DEPLOY, VALIDATION, PLAN, APPLY, VERIFY, DEPLOY)
        verify(logRepository, atLeast(5)).save(any(DeploymentLog.class));

        // Notifications temps réel à chaque étape
        verify(progressService, atLeast(5)).sendProgress(eq(1L), anyString(), anyInt(), anyString());

        // Notification Discord sur succès
        verify(notificationService).sendDiscordNotification(contains("réussi"));
    }

    @Test
    void deploy_demandeEnAttenteDApprobation_refuse() {
        InfrastructureRequest req = demande("PENDING_APPROVAL");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deployService.deploy(1L));

        assertTrue(ex.getMessage().contains("approbation"));
        // Aucun déploiement lancé
        verify(requestRepository, never()).save(any());
        verify(logRepository, never()).save(any());
    }

    @Test
    void deploy_dejaDeploye_refuse() {
        InfrastructureRequest req = demande("SUCCESS");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThrows(IllegalStateException.class, () -> deployService.deploy(1L));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void cancel_annulation_statutCancelledEtLogCree() {
        InfrastructureRequest req = demande("SUCCESS");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(InfrastructureRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InfrastructureRequest result = deployService.cancel(1L);

        assertEquals("CANCELLED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(logRepository).save(argThat(log ->
                "CANCEL".equals(log.getStep()) && log.getMessage().contains("terraform destroy")));
        verify(progressService).sendProgress(eq(1L), eq("CANCELLED"), eq(100), anyString());
    }
}
