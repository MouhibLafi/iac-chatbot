package com.company.iacchatbot.dto;

/**
 * DTO pour la mise à jour des quotas d'un utilisateur (UC-13, ADMIN)
 */
public class QuotaRequest {

    private Integer quotaCpu;
    private Integer quotaRam;
    private Integer quotaStorage;

    public QuotaRequest() {
    }

    public QuotaRequest(Integer quotaCpu, Integer quotaRam, Integer quotaStorage) {
        this.quotaCpu = quotaCpu;
        this.quotaRam = quotaRam;
        this.quotaStorage = quotaStorage;
    }

    public Integer getQuotaCpu() {
        return quotaCpu;
    }

    public void setQuotaCpu(Integer quotaCpu) {
        this.quotaCpu = quotaCpu;
    }

    public Integer getQuotaRam() {
        return quotaRam;
    }

    public void setQuotaRam(Integer quotaRam) {
        this.quotaRam = quotaRam;
    }

    public Integer getQuotaStorage() {
        return quotaStorage;
    }

    public void setQuotaStorage(Integer quotaStorage) {
        this.quotaStorage = quotaStorage;
    }
}
