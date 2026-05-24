package com.ruc.platform.ai.client;

import com.ruc.platform.ai.entity.AiProviderConfig;

import java.util.List;

public interface AiChatClient {
    String chat(AiProviderConfig config, List<Message> messages);

    record Message(String role, String content) {
    }
}
