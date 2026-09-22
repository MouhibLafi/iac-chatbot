package com.company.iacchatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les messages génériques
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String message;
    private String status = "success";

    public MessageResponse(String message) {
        this.message = message;
    }
}
