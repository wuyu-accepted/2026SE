package com.ruc.platform.ai.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private String message;
    private Long conversationId;
}
