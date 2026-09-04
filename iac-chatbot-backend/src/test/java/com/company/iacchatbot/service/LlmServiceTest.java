package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service LLM (extraction de parametres via Ollama)
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    private LlmService llmService;
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        llmService = new LlmService(chatClientBuilder, new ObjectMapper());
    }

    private void mockOllamaResponse(String json) {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(json);
    }

    @Test
    void extractionReussie_vmVsphere() {
        mockOllamaResponse("{\"resourceType\":\"VM\",\"platform\":\"VSPHERE\",\"cpu\":2,\"ramGb\":4,\"osImage\":\"ubuntu-22.04\"}");

        ExtractedParameters params = llmService.extractParameters("Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM");

        assertEquals(ResourceType.VM, params.getResourceType());
        assertEquals(PlatformType.VSPHERE, params.getPlatform());
        assertEquals(2, params.getCpu());
        assertEquals(4, params.getRamGb());
        assertEquals("ubuntu-22.04", params.getOsImage());
        assertEquals(50, params.getStorageGb());   // defaut applique
        assertEquals("default", params.getNetwork()); // defaut applique
    }

    @Test
    void extractionReussie_conteneurOpenShift() {
        mockOllamaResponse("{\"resourceType\":\"CONTAINER\",\"platform\":\"OPENSHIFT\",\"replicas\":3,\"containerImage\":\"nginx\"}");

        ExtractedParameters params = llmService.extractParameters("Deploie un conteneur nginx avec 3 replicas sur OpenShift");

        assertEquals(ResourceType.CONTAINER, params.getResourceType());
        assertEquals(PlatformType.OPENSHIFT, params.getPlatform());
        assertEquals(3, params.getReplicas());
        assertEquals("nginx", params.getContainerImage());
    }

    @Test
    void plateformeParDefaut_vmSansPlateforme_vsphere() {
        mockOllamaResponse("{\"resourceType\":\"VM\",\"cpu\":4}");

        ExtractedParameters params = llmService.extractParameters("une VM avec 4 CPU");

        assertEquals(PlatformType.VSPHERE, params.getPlatform());
    }

    @Test
    void plateformeParDefaut_conteneurSansPlateforme_openshift() {
        mockOllamaResponse("{\"resourceType\":\"CONTAINER\",\"containerImage\":\"redis\"}");

        ExtractedParameters params = llmService.extractParameters("un conteneur redis");

        assertEquals(PlatformType.OPENSHIFT, params.getPlatform());
    }

    @Test
    void jsonAvecBackticksMarkdown_estNettoye() {
        mockOllamaResponse("```json\n{\"resourceType\":\"VM\",\"platform\":\"OPENSHIFT\",\"storageGb\":100}\n```");

        ExtractedParameters params = llmService.extractParameters("VM 100Go sur OpenShift");

        assertEquals(ResourceType.VM, params.getResourceType());
        assertEquals(PlatformType.OPENSHIFT, params.getPlatform());
        assertEquals(100, params.getStorageGb());
    }

    @Test
    void jsonInvalide_fallbackVmVsphere() {
        mockOllamaResponse("Je ne comprends pas la demande.");

        ExtractedParameters params = llmService.extractParameters("blabla");

        assertEquals(ResourceType.VM, params.getResourceType());
        assertEquals(PlatformType.VSPHERE, params.getPlatform());
        assertEquals(2, params.getCpu());
    }

    @Test
    void erreurOllama_leveException() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class,
                () -> llmService.extractParameters("test"));
    }
}
