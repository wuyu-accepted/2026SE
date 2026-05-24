package com.ruc.platform.ai.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatResponse {
    private Long conversationId;
    private String answer;
    private Boolean fallback;
    private List<AiCitationVO> citations = new ArrayList<>();
    private List<AiActionVO> actions = new ArrayList<>();
}
