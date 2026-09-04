package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.exception.QuotaExceededException;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du QuotaService (UC-13)
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private InfrastructureRequestRepository requestRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaService = new QuotaService(requestRepository, objectMapper);
    }

    private User userAvecQuotasDefaut() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user");
        user.setQuotaCpu(32);
        user.setQuotaRam(128);
        user.setQuotaStorage(1000);
        return user;
    }

    private ExtractedParameters params(int cpu, int ramGb, int storageGb) {
        ExtractedParameters params = new ExtractedParameters();
        params.setResourceType(ResourceType.VM);
        params.setPlatform(PlatformType.VSPHERE);
        params.setCpu(cpu);
        params.setRamGb(ramGb);
        params.setStorageGb(storageGb);
        return params;
    }

    private InfrastructureRequest demandeSuccess(int cpu, int ramGb, int storageGb) throws Exception {
        InfrastructureRequest req = new InfrastructureRequest("VM de test");
        req.setStatus("SUCCESS");
        req.setExtractedParams(objectMapper.writeValueAsString(params(cpu, ramGb, storageGb)));
        return req;
    }

    @Test
    void checkQuota_quotaRespecte_neLancePasDException() throws Exception {
        // Un déploiement SUCCESS existant (2 CPU) + nouvelle demande (4 CPU) = 6 < 32
        List<InfrastructureRequest> existantes = List.of(demandeSuccess(2, 4, 50));
        when(requestRepository.findByUserIdAndStatus(1L, "SUCCESS")).thenReturn(existantes);

        assertDoesNotThrow(() -> quotaService.checkQuota(userAvecQuotasDefaut(), params(4, 8, 100)));
    }

    @Test
    void checkQuota_aucuneDemandePrecedente_neLancePasDException() {
        when(requestRepository.findByUserIdAndStatus(1L, "SUCCESS")).thenReturn(List.of());

        assertDoesNotThrow(() -> quotaService.checkQuota(userAvecQuotasDefaut(), params(32, 128, 1000)));
    }

    @Test
    void checkQuota_quotaCpuDepasse_lanceQuotaExceededException() throws Exception {
        // 30 CPU déjà utilisés + 4 demandés = 34 > 32
        List<InfrastructureRequest> existantes = List.of(demandeSuccess(30, 10, 100));
        when(requestRepository.findByUserIdAndStatus(1L, "SUCCESS")).thenReturn(existantes);

        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> quotaService.checkQuota(userAvecQuotasDefaut(), params(4, 8, 100)));

        assertTrue(ex.getMessage().contains("Quota CPU dépassé"));
    }

    @Test
    void checkQuota_quotaRamDepasse_lanceQuotaExceededException() {
        when(requestRepository.findByUserIdAndStatus(1L, "SUCCESS")).thenReturn(List.of());

        // 200 Go de RAM demandés > quota de 128
        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> quotaService.checkQuota(userAvecQuotasDefaut(), params(2, 200, 100)));

        assertTrue(ex.getMessage().contains("Quota RAM dépassé"));
    }

    @Test
    void checkQuota_quotaStockageDepasse_lanceQuotaExceededException() throws Exception {
        // 950 Go déjà utilisés + 100 demandés = 1050 > 1000
        List<InfrastructureRequest> existantes = List.of(demandeSuccess(2, 4, 950));
        when(requestRepository.findByUserIdAndStatus(1L, "SUCCESS")).thenReturn(existantes);

        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> quotaService.checkQuota(userAvecQuotasDefaut(), params(2, 4, 100)));

        assertTrue(ex.getMessage().contains("Quota stockage dépassé"));
    }
}
