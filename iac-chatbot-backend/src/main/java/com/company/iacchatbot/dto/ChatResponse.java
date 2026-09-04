package com.company.iacchatbot.dto;

/**
 * DTO pour la réponse du chatbot
 */
public class ChatResponse {

    private Long requestId;
    private String status;
    private ExtractedParameters extractedParams;
    private String generatedCode;
    private String message;

    // Constructors
    public ChatResponse() {}

    public ChatResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Getters and Setters
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ExtractedParameters getExtractedParams() {
        return extractedParams;
    }

    public void setExtractedParams(ExtractedParameters extractedParams) {
        this.extractedParams = extractedParams;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "requestId=" + requestId +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
