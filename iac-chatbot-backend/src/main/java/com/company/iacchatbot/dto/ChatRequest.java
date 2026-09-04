package com.company.iacchatbot.dto;

/**
 * DTO pour la requête du chatbot
 */
public class ChatRequest {

    private String message;
    private String targetPlatform; // terraform, openshift

    // Constructors
    public ChatRequest() {}

    public ChatRequest(String message, String targetPlatform) {
        this.message = message;
        this.targetPlatform = targetPlatform;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", targetPlatform='" + targetPlatform + '\'' +
                '}';
    }
}
