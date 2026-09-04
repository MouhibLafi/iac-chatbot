package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.ExtractedParameters;
import com.company.iacchatbot.exception.QuotaExceededException;
import com.company.iacchatbot.model.InfrastructureRequest;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.InfrastructureRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de vérification des quotas de ressources par utilisateur (UC-13).
 * Les ressources consommées = somme des demandes de l'utilisateur au statut SUCCESS.
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final InfrastructureRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    public QuotaService(InfrastructureRequestRepository requestRepository, ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Vérifie que la nouvelle demande ne fait pas dépasser les quotas de l'utilisateur.
     *
     * @param user   utilisateur courant
     * @param params paramètres extraits de la nouvelle demande
     * @throws QuotaExceededException si un quota est dépassé
     */
    @Transactional(readOnly = true)
    public void checkQuota(User user, ExtractedParameters params) {
        List<InfrastructureRequest> successRequests =
                requestRepository.findByUserIdAndStatus(user.getId(), "SUCCESS");

        int usedCpu = 0;
        int usedRam = 0;
        int usedStorage = 0;

        for (InfrastructureRequest req : successRequests) {
            ExtractedParameters p = parseParams(req.getExtractedParams());
            if (p != null) {
                usedCpu += orZero(p.getCpu());
                usedRam += orZero(p.getRamGb());
                usedStorage += orZero(p.getStorageGb());
            }
        }

        int totalCpu = usedCpu + orZero(params.getCpu());
        int totalRam = usedRam + orZero(params.getRamGb());
        int totalStorage = usedStorage + orZero(params.getStorageGb());

        if (totalCpu > user.getQuotaCpu()) {
            throw new QuotaExceededException(String.format(
                    "Quota CPU dépassé : %d utilisés + %d demandés = %d vCPU (quota: %d). Contactez un administrateur.",
                    usedCpu, orZero(params.getCpu()), totalCpu, user.getQuotaCpu()));
        }
        if (totalRam > user.getQuotaRam()) {
            throw new QuotaExceededException(String.format(
                    "Quota RAM dépassé : %d utilisés + %d demandés = %d Go (quota: %d Go). Contactez un administrateur.",
                    usedRam, orZero(params.getRamGb()), totalRam, user.getQuotaRam()));
        }
        if (totalStorage > user.getQuotaStorage()) {
            throw new QuotaExceededException(String.format(
                    "Quota stockage dépassé : %d utilisés + %d demandés = %d Go (quota: %d Go). Contactez un administrateur.",
                    usedStorage, orZero(params.getStorageGb()), totalStorage, user.getQuotaStorage()));
        }

        log.debug("Quotas OK pour {} : CPU {}/{}, RAM {}/{}, stockage {}/{}",
                user.getUsername(), totalCpu, user.getQuotaCpu(),
                totalRam, user.getQuotaRam(), totalStorage, user.getQuotaStorage());
    }

    /**
     * Parse les paramètres extraits stockés en JSON dans la demande
     */
    private ExtractedParameters parseParams(String extractedParams) {
        if (extractedParams == null || extractedParams.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(extractedParams, ExtractedParameters.class);
        } catch (Exception e) {
            log.warn("Impossible de parser extractedParams pour le calcul des quotas: {}", e.getMessage());
            return null;
        }
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
