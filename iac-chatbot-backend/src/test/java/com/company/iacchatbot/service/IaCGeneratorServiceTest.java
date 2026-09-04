package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du generateur IaC (vSphere / OpenShift / KubeVirt)
 */
class IaCGeneratorServiceTest {

    private IaCGeneratorService service;

    @BeforeEach
    void setUp() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix("");
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        service = new IaCGeneratorService(engine);
    }

    private ExtractedParameters params(ResourceType type, PlatformType platform) {
        ExtractedParameters params = new ExtractedParameters();
        params.setResourceType(type);
        params.setPlatform(platform);
        params.setCpu(4);
        params.setRamGb(8);
        params.setStorageGb(100);
        params.setOsImage("ubuntu-22.04");
        params.setContainerImage("nginx");
        params.setReplicas(3);
        params.setNetwork("default");
        return params;
    }

    @Test
    void vmSurVsphere_genereTerraformVsphere() {
        String code = service.generateCode(params(ResourceType.VM, PlatformType.VSPHERE));

        assertTrue(code.contains("vsphere_virtual_machine"), "Doit contenir la ressource vSphere");
        assertTrue(code.contains("hashicorp/vsphere"), "Doit utiliser le provider vSphere");
        assertTrue(code.contains("num_cpus = 4"), "Doit contenir le CPU");
        assertTrue(code.contains("memory   = 8 * 1024"), "Doit contenir la RAM");
        assertTrue(code.contains("size             = 100"), "Doit contenir le stockage");
        assertTrue(code.contains("ubuntu-22.04"), "Doit contenir l'image OS");
        assertFalse(code.contains("hashicorp/aws"), "Ne doit PAS contenir AWS");
    }

    @Test
    void conteneurSurOpenShift_genereManifestesKubernetes() {
        String code = service.generateCode(params(ResourceType.CONTAINER, PlatformType.OPENSHIFT));

        assertTrue(code.contains("kind: Deployment"), "Doit contenir un Deployment");
        assertTrue(code.contains("kind: Service"), "Doit contenir un Service");
        assertTrue(code.contains("kind: Route"), "Doit contenir une Route OpenShift");
        assertTrue(code.contains("replicas: 3"), "Doit contenir les replicas");
        assertTrue(code.contains("image: nginx:latest"), "Doit contenir l'image du conteneur");
        assertTrue(code.contains("route.openshift.io/v1"), "Doit utiliser l'API Route OpenShift");
    }

    @Test
    void vmSurOpenShift_genereKubeVirt() {
        String code = service.generateCode(params(ResourceType.VM, PlatformType.OPENSHIFT));

        assertTrue(code.contains("kubevirt.io/v1"), "Doit utiliser l'API KubeVirt");
        assertTrue(code.contains("kind: VirtualMachine"), "Doit contenir une VirtualMachine");
        assertTrue(code.contains("dataVolumeTemplates"), "Doit contenir le volume de donnees");
        assertTrue(code.contains("storage: 100Gi"), "Doit contenir le stockage");
        assertTrue(code.contains("cores: 4"), "Doit contenir le CPU");
    }

    @Test
    void conteneurSurVsphere_leveException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generateCode(params(ResourceType.CONTAINER, PlatformType.VSPHERE)));
        assertTrue(ex.getMessage().contains("OpenShift"));
    }
}
