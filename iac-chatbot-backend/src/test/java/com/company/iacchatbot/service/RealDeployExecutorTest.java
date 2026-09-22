package com.company.iacchatbot.service;

import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du RealDeployExecutor (déploiement réel - UC-06)
 */
class RealDeployExecutorTest {

    @TempDir
    Path tempDir;

    private RealDeployExecutor executor(String vboxPath) {
        return new RealDeployExecutor(vboxPath, "oc", "", "", tempDir.toString(), "C:/oc-bin", 10L);
    }

    private InfrastructureRequest demandeVm() {
        InfrastructureRequest req = new InfrastructureRequest("VM Ubuntu 2 CPU");
        req.setId(1L);
        req.setResourceType(ResourceType.VM);
        req.setTargetPlatform("VSPHERE");
        req.setExtractedParams(
                "{\"resourceType\":\"VM\",\"platform\":\"VSPHERE\",\"osImage\":\"ubuntu-22.04\","
                        + "\"cpu\":2,\"ramGb\":4,\"storageGb\":50}");
        return req;
    }

    @Test
    void osTypeFor_imagesConnues_mappingVirtualBox() {
        assertEquals("Ubuntu_64", RealDeployExecutor.osTypeFor("ubuntu-22.04"));
        assertEquals("Windows2019_64", RealDeployExecutor.osTypeFor("windows-server"));
        assertEquals("Windows2022_64", RealDeployExecutor.osTypeFor("windows-2022"));
        assertEquals("Debian_64", RealDeployExecutor.osTypeFor("debian-12"));
        assertEquals("RedHat_64", RealDeployExecutor.osTypeFor("rhel-9"));
        assertEquals("Other_64", RealDeployExecutor.osTypeFor("image-inconnue"));
        assertEquals("Other_64", RealDeployExecutor.osTypeFor(null));
    }

    @Test
    void resolveVmName_suffixeAleatoireStockeEtReutilise() {
        InfrastructureRequest req = demandeVm();

        String name = RealDeployExecutor.resolveVmName(req, "");

        // Format iac-vm-<3 caractères alphanumériques> et stocké dans la demande
        assertTrue(name.matches("iac-vm-[a-z0-9]{3}"), "nom inattendu : " + name);
        assertEquals(name, req.getResourceName());
        // Un nom déjà stocké est réutilisé tel quel (indispensable pour la suppression)
        assertEquals(name, RealDeployExecutor.resolveVmName(req, ""));
    }

    @Test
    void resolveVmName_eviteLesCollisionsAvecVmsExistantes() {
        InfrastructureRequest req = demandeVm();
        req.setResourceName("iac-vm-abc");

        // Le nom stocké est réutilisé même si présent dans la liste (retry, annulation...)
        assertEquals("iac-vm-abc",
                RealDeployExecutor.resolveVmName(req, "\"iac-vm-abc\""));
    }

    @Test
    void randomSuffix_troisCaracteresAlphanumeriques() {
        for (int i = 0; i < 50; i++) {
            assertTrue(RealDeployExecutor.randomSuffix().matches("[a-z0-9]{3}"));
        }
    }

    @Test
    void deploy_vboxManageIntrouvable_echoueAvecMessageClair() {
        RealDeployExecutor executor = executor("vboxmanage-inexistant-xyz");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> executor.deploy(demandeVm(), (step, message) -> { }));

        assertTrue(ex.getMessage().contains("vboxmanage-inexistant-xyz"));
    }
}
