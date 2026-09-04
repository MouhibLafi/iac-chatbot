package com.company.iacchatbot.service;

import com.company.iacchatbot.exception.DeploymentFailedException;
import com.company.iacchatbot.model.DeploymentLog;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.LogLevel;
import com.company.iacchatbot.repository.DeploymentLogRepository;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de déploiement (UC-06, UC-10).
 * Deux modes (propriété deploy.mode) :
 *  - simulation (défaut) : étapes simulées, aucun outil externe exécuté
 *  - real : exécution réelle via RealDeployExecutor (VirtualBox / OpenShift oc)
 * Les étapes sont tracées dans des DeploymentLog + notifications temps réel.
 */
@Service
public class DeployService {

    private static final Logger log = LoggerFactory.getLogger(DeployService.class);

    private final InfrastructureRequestRepository requestRepository;
    private final DeploymentLogRepository logRepository;
    private final ProgressNotificationService progressService;
    private final NotificationService notificationService;
    private final RealDeployExecutor realExecutor;
    private final String deployMode;

    public DeployService(InfrastructureRequestRepository requestRepository,
                         DeploymentLogRepository logRepository,
                         ProgressNotificationService progressService,
                         NotificationService notificationService,
                         RealDeployExecutor realExecutor,
                         @Value("${deploy.mode:simulation}") String deployMode) {
        this.requestRepository = requestRepository;
        this.logRepository = logRepository;
        this.progressService = progressService;
        this.notificationService = notificationService;
        this.realExecutor = realExecutor;
        this.deployMode = deployMode;
    }

    /** Mode réel actif ? (deploy.mode=real et exécuteur disponible) */
    private boolean isRealMode() {
        return "real".equalsIgnoreCase(deployMode) && realExecutor != null;
    }

    /** Progression associée à chaque étape réelle */
    private int progressForStep(String step) {
        return switch (step) {
            case "VALIDATION" -> 30;
            case "PLAN" -> 50;
            case "APPLY" -> 80;
            case "VERIFY" -> 95;
            default -> 50;
        };
    }

    /**
     * Simule le déploiement d'une demande (RUNNING -> SUCCESS).
     *
     * @throws IllegalStateException    si le déploiement est déjà terminé avec succès (409)
     * @throws IllegalArgumentException si la demande attend encore une approbation (400)
     * @throws EntityNotFoundException  si la demande n'existe pas (404)
     */
    @Transactional
    public InfrastructureRequest deploy(Long requestId) {
        InfrastructureRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande d'infrastructure non trouvée avec l'ID: " + requestId));

        if ("SUCCESS".equals(request.getStatus())) {
            throw new IllegalStateException("Cette demande est déjà déployée avec succès.");
        }
        if ("PENDING_APPROVAL".equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Cette demande est en attente d'approbation par un administrateur.");
        }
        if ("RUNNING".equals(request.getStatus())) {
            throw new IllegalStateException("Un déploiement est déjà en cours pour cette demande.");
        }

        boolean openshift = "OPENSHIFT".equalsIgnoreCase(request.getTargetPlatform());
        String validateCmd = openshift
                ? "kubectl apply --dry-run=client : OK (simulation)"
                : "terraform validate : OK (simulation)";
        String planCmd = openshift
                ? "oc diff : 3 ressource(s) à créer (simulation)"
                : "terraform plan : 1 ressource(s) à créer (simulation)";
        String applyCmd = openshift
                ? "kubectl apply -f manifest.yaml : ressources créées (simulation)"
                : "terraform apply -auto-approve : ressources créées (simulation)";

        try {
            // 1. Passage en RUNNING
            request.setStatus("RUNNING");
            request = requestRepository.save(request);
            addLog(request, "DEPLOY", LogLevel.INFO,
                    isRealMode() ? "Déploiement réel démarré" : "Déploiement simulé démarré");
            progressService.sendProgress(requestId, "DEPLOY", 10, "Déploiement démarré...");

            if (isRealMode()) {
                // Exécution réelle : VirtualBox (VM) ou oc (OpenShift)
                InfrastructureRequest finalRequest = request;
                realExecutor.deploy(request, (step, message) -> {
                    addLog(finalRequest, step, LogLevel.INFO, message);
                    progressService.sendProgress(requestId, step, progressForStep(step), message);
                });
            } else {
                // 2. Etape VALIDATION (simulation)
                addLog(request, "VALIDATION", LogLevel.INFO, validateCmd);
                progressService.sendProgress(requestId, "VALIDATION", 30, "Validation de la configuration...");

                // 3. Etape PLAN (simulation)
                addLog(request, "PLAN", LogLevel.INFO, planCmd);
                progressService.sendProgress(requestId, "PLAN", 50, "Planification des ressources...");

                // 4. Etape APPLY (simulation)
                addLog(request, "APPLY", LogLevel.INFO, applyCmd);
                progressService.sendProgress(requestId, "APPLY", 80, "Création des ressources...");

                // 5. Etape VERIFY (simulation)
                addLog(request, "VERIFY", LogLevel.INFO,
                        "Vérification des ressources : toutes opérationnelles (simulation)");
                progressService.sendProgress(requestId, "VERIFY", 95, "Vérification des ressources...");
            }

            // 6. Succès final
            request.setStatus("SUCCESS");
            request.setCompletedAt(LocalDateTime.now());
            request = requestRepository.save(request);
            addLog(request, "DEPLOY", LogLevel.INFO, "Déploiement terminé avec succès");
            progressService.sendProgress(requestId, "SUCCESS", 100, "Déploiement terminé avec succès !");
            notificationService.sendDiscordNotification(
                    "✅ Déploiement réussi : demande #" + requestId
                            + " (" + request.getResourceType() + " sur " + request.getTargetPlatform() + ")");

            log.info("Déploiement réussi pour la demande {} (mode {})", requestId,
                    isRealMode() ? "real" : "simulation");
            return request;

        } catch (RuntimeException e) {
            // Traçabilité de l'échec
            request.setStatus("FAILED");
            request.setErrorMessage(e.getMessage());
            request.setCompletedAt(LocalDateTime.now());
            requestRepository.save(request);
            addLog(request, "ERROR", LogLevel.ERROR, "Echec du déploiement: " + e.getMessage());
            progressService.sendProgress(requestId, "ERROR", 100, "Echec du déploiement");
            notificationService.sendDiscordNotification(
                    "❌ Echec du déploiement : demande #" + requestId + " - " + e.getMessage());
            throw new DeploymentFailedException("Echec du déploiement de la demande " + requestId, e);
        }
    }

    /**
     * Annule un déploiement (terraform destroy / oc delete simulé)
     */
    @Transactional
    public InfrastructureRequest cancel(Long requestId) {
        InfrastructureRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande d'infrastructure non trouvée avec l'ID: " + requestId));

        boolean openshift = "OPENSHIFT".equalsIgnoreCase(request.getTargetPlatform());
        String destroyCmd = openshift
                ? "oc delete -f manifest.yaml : ressources supprimées (simulation)"
                : "terraform destroy -auto-approve : ressources détruites (simulation)";

        if (isRealMode()) {
            // Destruction réelle (best effort : la demande passe CANCELLED même en cas d'erreur)
            InfrastructureRequest finalRequest = request;
            try {
                realExecutor.destroy(request, (step, message) ->
                        addLog(finalRequest, "CANCEL", LogLevel.WARN, "Suppression réelle : " + message));
            } catch (RuntimeException e) {
                addLog(request, "CANCEL", LogLevel.ERROR,
                        "Echec de la suppression réelle : " + e.getMessage());
            }
        }

        request.setStatus("CANCELLED");
        request.setCompletedAt(LocalDateTime.now());
        request = requestRepository.save(request);
        addLog(request, "CANCEL", LogLevel.WARN, "Annulation demandée : " + destroyCmd);
        progressService.sendProgress(requestId, "CANCELLED", 100, "Déploiement annulé");

        log.info("Déploiement annulé pour la demande {}", requestId);
        return request;
    }

    /**
     * Récupère les logs d'une demande (ordre chronologique)
     */
    @Transactional(readOnly = true)
    public List<DeploymentLog> getLogs(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new EntityNotFoundException(
                    "Demande d'infrastructure non trouvée avec l'ID: " + requestId);
        }
        return logRepository.findByDeploymentRequestIdOrderByTimestampAsc(requestId);
    }

    /**
     * Ajoute un log de déploiement tracé en base
     */
    private void addLog(InfrastructureRequest request, String step, LogLevel level, String message) {
        logRepository.save(new DeploymentLog(request, step, level, message));
    }
}
