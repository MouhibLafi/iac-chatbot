package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service de generation de code Infrastructure as Code via Thymeleaf.
 *
 * Combinaisons supportees (document officiel societe):
 * - VM + VSPHERE      -> Terraform vSphere (templates/terraform/vmware-vm.tf)
 * - CONTAINER + OPENSHIFT -> YAML Kubernetes (deployment + service + route)
 * - VM + OPENSHIFT    -> YAML KubeVirt (templates/openshift/kubevirt-vm.yaml)
 */
@Service
public class IaCGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(IaCGeneratorService.class);

    private final TemplateEngine iacTemplateEngine;

    public IaCGeneratorService(@Qualifier("iacTemplateEngine") TemplateEngine iacTemplateEngine) {
        this.iacTemplateEngine = iacTemplateEngine;
    }

    /**
     * Generer le code IaC a partir des parametres extraits.
     */
    public String generateCode(ExtractedParameters params) {
        log.info("Generation IaC pour: {}", params);

        ResourceType type = params.getResourceType();
        PlatformType platform = params.getPlatform();

        if (type == ResourceType.VM && platform == PlatformType.VSPHERE) {
            return render("terraform/vmware-vm.tf", buildContext(params));
        }
        if (type == ResourceType.CONTAINER && platform == PlatformType.OPENSHIFT) {
            return generateOpenShiftContainer(params);
        }
        if (type == ResourceType.VM && platform == PlatformType.OPENSHIFT) {
            return render("openshift/kubevirt-vm.yaml", buildContext(params));
        }
        // CONTAINER + VSPHERE : combinaison non logique
        throw new IllegalArgumentException(
                "Combinaison non supportee: les conteneurs se deploient sur OpenShift, " +
                "les machines virtuelles classiques sur VMware vSphere. " +
                "Precisez 'OpenShift' pour un conteneur ou 'VMware' pour une VM.");
    }

    /**
     * Generer les manifestes OpenShift pour un conteneur:
     * Deployment + Service + Route (separes par ---)
     */
    private String generateOpenShiftContainer(ExtractedParameters params) {
        Context context = buildContext(params);
        String deployment = render("openshift/deployment.yaml", context);
        String service = render("openshift/service.yaml", context);
        String route = render("openshift/route.yaml", context);
        return deployment + "\n---\n\n" + service + "\n---\n\n" + route;
    }

    /**
     * Rendre un template Thymeleaf (mode TEXT).
     */
    private String render(String templateName, Context context) {
        return iacTemplateEngine.process(templateName, context);
    }

    /**
     * Construire le contexte Thymeleaf avec les valeurs par defaut appliquees.
     */
    private Context buildContext(ExtractedParameters params) {
        Context context = new Context();
        context.setVariable("resourceType", params.getResourceType());
        context.setVariable("platform", params.getPlatform());
        context.setVariable("osImage", valueOrDefault(params.getOsImage(), "ubuntu-22.04"));
        context.setVariable("cpu", valueOrDefault(params.getCpu(), 2));
        context.setVariable("ramGb", valueOrDefault(params.getRamGb(), 4));
        context.setVariable("storageGb", valueOrDefault(params.getStorageGb(), 50));
        context.setVariable("replicas", valueOrDefault(params.getReplicas(), 1));
        context.setVariable("containerImage", valueOrDefault(params.getContainerImage(), "nginx"));
        context.setVariable("network", valueOrDefault(params.getNetwork(), "default"));
        context.setVariable("appName", sanitizeName(valueOrDefault(params.getContainerImage(), "app")));
        context.setVariable("vmName", "vm-" + sanitizeName(valueOrDefault(params.getOsImage(), "ubuntu-22-04")));
        return context;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Nom compatible Kubernetes (RFC 1123): minuscules, chiffres et tirets.
     */
    private String sanitizeName(String name) {
        String sanitized = name.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
        sanitized = sanitized.replaceAll("^-+", "").replaceAll("-+$", "");
        return sanitized.isEmpty() ? "app" : sanitized;
    }
}
