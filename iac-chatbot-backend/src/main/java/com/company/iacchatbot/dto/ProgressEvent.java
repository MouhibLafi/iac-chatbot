package com.company.iacchatbot.dto;

/**
 * Evenement de progression envoye via WebSocket STOMP (/topic/progress)
 * pendant le traitement d'une demande de generation IaC.
 */
public class ProgressEvent {

    private Long requestId;
    private String step;     // EXTRACTION, GENERATION, COMPLETED, ERROR
    private int percent;     // 0-100
    private String message;

    public ProgressEvent() {}

    public ProgressEvent(Long requestId, String step, int percent, String message) {
        this.requestId = requestId;
        this.step = step;
        this.percent = percent;
        this.message = message;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
