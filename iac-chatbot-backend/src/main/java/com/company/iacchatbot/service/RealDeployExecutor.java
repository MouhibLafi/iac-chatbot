package com.company.iacchatbot.service;

import com.company.iacchatbot.model.InfrastructureRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Exécuteur RÉEL des déploiements (UC-06, étape 5-6 du fonctionnement global).
 *
 * Utilisé par DeployService pour tous les déploiements.
 *
 * Cibles :
 *  - VM        -> VirtualBox sur la machine hôte (VBoxManage)
 *  - OpenShift -> oc apply/delete du manifeste YAML généré (conteneurs et VMs KubeVirt)
 *
 * Chaque étape (VALIDATION, PLAN, APPLY, VERIFY) exécute de vraies commandes
 * et remonte leur sortie réelle via le StepCallback (logs + notifications).
 */
@Component
public class RealDeployExecutor {

    private static final Logger log = LoggerFactory.getLogger(RealDeployExecutor.class);

    /** Taille max d'une sortie de commande stockée en base */
    private static final int MAX_OUTPUT = 4000;

    /**
     * Callback appelé après chaque étape réelle avec la sortie de la commande.
     */
    public interface StepCallback {
        void onStep(String step, String message);
    }

    private final String vboxManagePath;
    private final String ocBin;
    private final String ocServer;
    private final String ocToken;
    private final Path workdirBase;
    private final long timeoutSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealDeployExecutor(
            @Value("${deploy.vboxmanage-path:VBoxManage}") String vboxManagePath,
            @Value("${deploy.oc-bin:oc}") String ocBin,
            @Value("${deploy.oc-server:}") String ocServer,
            @Value("${deploy.oc-token:}") String ocToken,
            @Value("${deploy.oc-token-file:../openshift/oc-token.txt}") String ocTokenFile,
            @Value("${deploy.workdir:./deploy-work}") String workdir,
            @Value("${deploy.command-timeout-seconds:300}") long timeoutSeconds) {
        this.vboxManagePath = vboxManagePath;
        this.ocBin = ocBin;
        this.ocServer = ocServer;
        this.ocToken = resolveToken(ocToken, ocTokenFile);
        this.workdirBase = Path.of(workdir);
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Token OpenShift : variable d'environnement OC_TOKEN en priorité,
     * sinon lecture automatique du fichier (défaut ../openshift/oc-token.txt).
     * Permet de lancer le backend avec simplement "mvn spring-boot:run".
     */
    private static String resolveToken(String ocToken, String ocTokenFile) {
        if (ocToken != null && !ocToken.isBlank()) {
            return ocToken;
        }
        try {
            Path file = Path.of(ocTokenFile);
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            log.warn("Impossible de lire le fichier de token OpenShift {} : {}", ocTokenFile, e.getMessage());
        }
        return ocToken == null ? "" : ocToken;
    }

    /**
     * Déploie réellement la ressource demandée (VM VirtualBox ou manifeste OpenShift).
     *
     * @throws RuntimeException si une commande échoue (status FAILED géré par DeployService)
     */
    public void deploy(InfrastructureRequest request, StepCallback callback) {
        if ("OPENSHIFT".equalsIgnoreCase(request.getTargetPlatform())) {
            deployOpenShift(request, callback);
        } else {
            deployVirtualBox(request, callback);
        }
    }

    /**
     * Supprime réellement la ressource (annulation / destruction).
     */
    public void destroy(InfrastructureRequest request, StepCallback callback) {
        if ("OPENSHIFT".equalsIgnoreCase(request.getTargetPlatform())) {
            Path manifest = writeManifest(request);
            ocLoginIfNeeded();
            callback.onStep("CANCEL", run(null, ocBin, "delete", "-f", manifest.toString(),
                    "--ignore-not-found=true"));
        } else {
            String vmName = request.getResourceName();
            if (vmName == null || vmName.isBlank()) {
                callback.onStep("CANCEL",
                        "Aucune VM VirtualBox associée à cette demande, rien à supprimer");
                return;
            }
            // Extinction si la VM tourne (échec ignoré : elle est peut-être déjà éteinte)
            runQuietly(vboxManagePath, "controlvm", vmName, "poweroff");
            callback.onStep("CANCEL", run(null, vboxManagePath, "unregistervm", vmName, "--delete"));
        }
    }

    // ======================================================================
    // VirtualBox (VMs)
    // ======================================================================

    private void deployVirtualBox(InfrastructureRequest request, StepCallback callback) {
        JsonNode params = parseParams(request);
        int cpu = params.path("cpu").asInt(2);
        int ramMb = params.path("ramGb").asInt(4) * 1024;
        int storageMb = params.path("storageGb").asInt(50) * 1024;
        String osType = osTypeFor(params.path("osImage").asText(""));

        // 1. VALIDATION : VirtualBox opérationnel + nom de VM unique (aléatoire)
        String version = run(null, vboxManagePath, "--version");
        String existing = run(null, vboxManagePath, "list", "vms");
        String vmName = resolveVmName(request, existing);
        if (existing.contains("\"" + vmName + "\"")) {
            throw new IllegalStateException("La VM " + vmName + " existe déjà dans VirtualBox");
        }
        callback.onStep("VALIDATION",
                "VirtualBox " + version.trim() + " opérationnel, nom de VM : " + vmName);

        // 2. PLAN : résumé des ressources qui vont être créées
        callback.onStep("PLAN", String.format(
                "Création de la VM %s : %d vCPU, %d Mo RAM, %d Mo disque, ostype=%s",
                vmName, cpu, ramMb, storageMb, osType));

        // 3. APPLY : création réelle de la VM
        Path dir = workdir(request);
        Path vdi = dir.resolve(vmName + ".vdi");
        List<String> output = new ArrayList<>();
        output.add(run(null, vboxManagePath, "createvm", "--name", vmName,
                "--ostype", osType, "--register"));
        output.add(run(null, vboxManagePath, "modifyvm", vmName,
                "--cpus", String.valueOf(cpu),
                "--memory", String.valueOf(ramMb),
                "--vram", "16", "--nic1", "nat"));
        output.add(run(null, vboxManagePath, "createmedium", "disk",
                "--filename", vdi.toString(), "--size", String.valueOf(storageMb), "--format", "VDI"));
        output.add(run(null, vboxManagePath, "storagectl", vmName,
                "--name", "SATA", "--add", "sata", "--controller", "IntelAhci"));
        output.add(run(null, vboxManagePath, "storageattach", vmName,
                "--storagectl", "SATA", "--port", "0", "--device", "0",
                "--type", "hdd", "--medium", vdi.toString()));
        callback.onStep("APPLY", String.join("\n", output));

        // 4. VERIFY : la VM existe bien dans VirtualBox
        String info = run(null, vboxManagePath, "showvminfo", vmName, "--machinereadable");
        String state = info.lines()
                .filter(l -> l.startsWith("VMState="))
                .findFirst().orElse("VMState=inconnu");
        callback.onStep("VERIFY", "VM présente dans VirtualBox : " + state.trim());
    }

    // ======================================================================
    // OpenShift (conteneurs + VMs KubeVirt)
    // ======================================================================

    private void deployOpenShift(InfrastructureRequest request, StepCallback callback) {
        Path manifest = writeManifest(request);
        ocLoginIfNeeded();

        // 1. VALIDATION : dry-run côté serveur OpenShift
        callback.onStep("VALIDATION",
                run(null, ocBin, "apply", "--dry-run=server", "-f", manifest.toString()));

        // 2. PLAN : liste des ressources qui seront appliquées
        callback.onStep("PLAN",
                run(null, ocBin, "apply", "--dry-run=server", "-o", "name", "-f", manifest.toString()));

        // 3. APPLY : création réelle des ressources
        callback.onStep("APPLY", run(null, ocBin, "apply", "-f", manifest.toString()));

        // 4. VERIFY : les ressources existent dans le cluster
        callback.onStep("VERIFY", run(null, ocBin, "get", "-f", manifest.toString()));
    }

    private void ocLoginIfNeeded() {
        if (ocServer != null && !ocServer.isBlank()) {
            run(null, ocBin, "login", "--server=" + ocServer, "--token=" + ocToken,
                    "--insecure-skip-tls-verify=true");
        }
    }

    // ======================================================================
    // Utilitaires
    // ======================================================================

    /** Nom de VM : réutilise le nom stocké dans la demande, sinon génère iac-vm-<3 caractères aléatoires> */
    static String resolveVmName(InfrastructureRequest request, String existingVms) {
        if (request.getResourceName() != null && !request.getResourceName().isBlank()) {
            return request.getResourceName();
        }
        String name;
        do {
            name = "iac-vm-" + randomSuffix();
        } while (existingVms != null && existingVms.contains("\"" + name + "\""));
        request.setResourceName(name);
        return name;
    }

    /** Suffixe aléatoire de 3 caractères (lettres minuscules + chiffres) */
    static String randomSuffix() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** Mappe l'image OS demandée vers un ostype VirtualBox */
    static String osTypeFor(String osImage) {
        String os = osImage == null ? "" : osImage.toLowerCase();
        if (os.contains("ubuntu")) return "Ubuntu_64";
        if (os.contains("debian")) return "Debian_64";
        if (os.contains("windows") && os.contains("2022")) return "Windows2022_64";
        if (os.contains("windows")) return "Windows2019_64";
        if (os.contains("centos") || os.contains("rhel") || os.contains("redhat")) return "RedHat_64";
        if (os.contains("fedora")) return "Fedora_64";
        return "Other_64";
    }

    private JsonNode parseParams(InfrastructureRequest request) {
        try {
            String json = request.getExtractedParams();
            return (json == null || json.isBlank()) ? objectMapper.createObjectNode()
                    : objectMapper.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Paramètres extraits illisibles pour la demande "
                    + request.getId(), e);
        }
    }

    private Path workdir(InfrastructureRequest request) {
        try {
            Path dir = workdirBase.resolve(String.valueOf(request.getId()));
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le répertoire de travail", e);
        }
    }

    private Path writeManifest(InfrastructureRequest request) {
        try {
            Path manifest = workdir(request).resolve("manifest.yaml");
            Files.writeString(manifest, request.getGeneratedCode(), StandardCharsets.UTF_8);
            return manifest;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire le manifeste YAML", e);
        }
    }

    /**
     * Exécute une commande réelle et retourne sa sortie (stdout+stderr).
     *
     * @throws RuntimeException si la commande échoue, expire ou est introuvable
     */
    private String run(Path dir, String... command) {
        log.debug("Exécution : {}", String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        if (dir != null) {
            pb.directory(dir.toFile());
        }
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        "Timeout (" + timeoutSeconds + "s) sur : " + command[0]);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "Echec de la commande " + command[0] + " (code " + process.exitValue()
                                + ") : " + truncate(output));
            }
            return truncate(output);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Commande introuvable ou non exécutable : " + command[0]
                            + " (" + e.getMessage() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Commande interrompue : " + command[0], e);
        }
    }

    /** Exécute une commande en ignorant tout échec (best effort) */
    private void runQuietly(String... command) {
        try {
            run(null, command);
        } catch (RuntimeException e) {
            log.debug("Commande best-effort ignorée : {} ({})", command[0], e.getMessage());
        }
    }

    private String truncate(String output) {
        if (output == null) {
            return "";
        }
        return output.length() <= MAX_OUTPUT ? output : output.substring(0, MAX_OUTPUT) + "…";
    }
}
