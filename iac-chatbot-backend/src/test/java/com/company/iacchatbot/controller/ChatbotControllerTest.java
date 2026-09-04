package com.company.iacchatbot.controller;

import com.company.iacchatbot.dto.ChatRequest;
import com.company.iacchatbot.dto.ChatResponse;
import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.company.iacchatbot.repository.UserRepository;
import com.company.iacchatbot.service.IaCGeneratorService;
import com.company.iacchatbot.service.LlmService;
import com.company.iacchatbot.service.ProgressNotificationService;
import com.company.iacchatbot.service.QuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du ChatbotController (sans contexte Spring)
 */
@ExtendWith(MockitoExtension.class)
class ChatbotControllerTest {

    @Mock
    private LlmService llmService;

    @Mock
    private IaCGeneratorService iaCGeneratorService;

    @Mock
    private InfrastructureRequestRepository repository;

    @Mock
    private ProgressNotificationService progressService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ChatbotController controller;

    @BeforeEach
    void setUp() {
        // La sauvegarde retourne l'entite avec un ID
        lenient().when(repository.save(any(InfrastructureRequest.class))).thenAnswer(invocation -> {
            InfrastructureRequest req = invocation.getArgument(0);
            req.setId(1L);
            return req;
        });
    }

    private ExtractedParameters params(ResourceType type, PlatformType platform) {
        ExtractedParameters params = new ExtractedParameters();
        params.setResourceType(type);
        params.setPlatform(platform);
        params.setCpu(2);
        params.setRamGb(4);
        params.setStorageGb(50);
        params.setOsImage("ubuntu-22.04");
        params.setNetwork("default");
        params.setReplicas(1);
        return params;
    }

    @Test
    void processMessage_succes_vmVsphere() {
        when(llmService.extractParameters(anyString()))
                .thenReturn(params(ResourceType.VM, PlatformType.VSPHERE));
        when(iaCGeneratorService.generateCode(any())).thenReturn("resource \"vsphere_virtual_machine\" \"vm\" {}");

        ResponseEntity<ChatResponse> response =
                controller.processMessage(new ChatRequest("VM Ubuntu 2 CPU", "auto"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().getStatus());
        assertNotNull(response.getBody().getGeneratedCode());
        assertTrue(response.getBody().getMessage().contains("vSphere"));
        verify(repository, times(2)).save(any());
        verify(progressService, atLeast(3)).sendProgress(any(), anyString(), anyInt(), anyString());
    }

    @Test
    void processMessage_succes_conteneurOpenShift() {
        ExtractedParameters p = params(ResourceType.CONTAINER, PlatformType.OPENSHIFT);
        p.setContainerImage("nginx");
        p.setReplicas(3);
        when(llmService.extractParameters(anyString())).thenReturn(p);
        when(iaCGeneratorService.generateCode(any())).thenReturn("kind: Deployment");

        ResponseEntity<ChatResponse> response =
                controller.processMessage(new ChatRequest("conteneur nginx", "auto"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("OpenShift"));
    }

    @Test
    void processMessage_plateformeForceeParUtilisateur_prioritaireSurLlm() {
        // Le LLM detecte VSPHERE mais l'utilisateur force OPENSHIFT
        when(llmService.extractParameters(anyString()))
                .thenReturn(params(ResourceType.VM, PlatformType.VSPHERE));
        when(iaCGeneratorService.generateCode(any())).thenReturn("kind: VirtualMachine");

        ResponseEntity<ChatResponse> response =
                controller.processMessage(new ChatRequest("VM Ubuntu", "openshift"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PlatformType.OPENSHIFT, response.getBody().getExtractedParams().getPlatform());
        verify(iaCGeneratorService).generateCode(argThat(p -> p.getPlatform() == PlatformType.OPENSHIFT));
    }

    @Test
    void processMessage_combinaisonInvalide_retourneClarification() {
        when(llmService.extractParameters(anyString()))
                .thenReturn(params(ResourceType.CONTAINER, PlatformType.VSPHERE));
        when(iaCGeneratorService.generateCode(any()))
                .thenThrow(new IllegalArgumentException("Precisez 'OpenShift' pour un conteneur"));

        ResponseEntity<ChatResponse> response =
                controller.processMessage(new ChatRequest("conteneur sur vsphere", "auto"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("clarification_needed", response.getBody().getStatus());
    }

    @Test
    void processMessage_erreurLlm_retourne500() {
        when(llmService.extractParameters(anyString()))
                .thenThrow(new RuntimeException("Ollama indisponible"));

        ResponseEntity<ChatResponse> response =
                controller.processMessage(new ChatRequest("test", "auto"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("error", response.getBody().getStatus());
    }
}
