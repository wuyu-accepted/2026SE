package com.ruc.platform.ai.service;

import com.ruc.platform.ai.client.AiChatClient;
import com.ruc.platform.ai.dto.AiChatRequest;
import com.ruc.platform.ai.entity.AiProviderConfig;
import com.ruc.platform.ai.entity.AiMessage;
import com.ruc.platform.ai.mapper.AiConversationMapper;
import com.ruc.platform.ai.mapper.AiMessageMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @Test
    void returnsFallbackWhenNoActiveConfigExists() {
        AiConfigService configService = mock(AiConfigService.class);
        when(configService.findActiveConfig()).thenReturn(null);
        AiChatService service = newService(configService, null);
        AiChatRequest request = new AiChatRequest();
        request.setMessage("我想请假");

        var response = service.chat(1001L, Set.of("student"), request);

        assertThat(response.getFallback()).isTrue();
        assertThat(response.getAnswer()).contains("请假审批流程");
        assertThat(response.getActions()).extracting("code").contains("leave");
    }

    @Test
    void usesActiveConfigAndClientAnswer() {
        AiProviderConfig config = config();
        AiConfigService configService = mock(AiConfigService.class);
        when(configService.findActiveConfig()).thenReturn(config);
        AiChatClient client = mock(AiChatClient.class);
        when(client.chat(any(), any())).thenReturn("你可以在请假审批流程中提交申请。");
        AiChatService service = newService(configService, client);
        AiChatRequest request = new AiChatRequest();
        request.setMessage("我想请假");

        var response = service.chat(1001L, Set.of("student"), request);

        assertThat(response.getFallback()).isFalse();
        assertThat(response.getAnswer()).isEqualTo("你可以在请假审批流程中提交申请。");
        assertThat(response.getActions()).extracting("code").contains("leave");
        verify(client).chat(any(), any());
    }

    @Test
    void fallsBackWhenClientThrowsException() {
        AiProviderConfig config = config();
        AiConfigService configService = mock(AiConfigService.class);
        when(configService.findActiveConfig()).thenReturn(config);
        AiChatClient client = mock(AiChatClient.class);
        when(client.chat(any(), any())).thenThrow(new RuntimeException("provider unavailable"));
        AiChatService service = newService(configService, client);
        AiChatRequest request = new AiChatRequest();
        request.setMessage("在哪里看通知");

        var response = service.chat(1001L, Set.of("student"), request);

        assertThat(response.getFallback()).isTrue();
        assertThat(response.getActions()).extracting("code").contains("notice");
    }

    @Test
    void reusesExistingConversationAndAppendsRecentHistory() {
        AiProviderConfig config = config();
        AiConfigService configService = mock(AiConfigService.class);
        when(configService.findActiveConfig()).thenReturn(config);
        AiChatClient client = mock(AiChatClient.class);
        when(client.chat(any(), any())).thenReturn("好的");
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiMessageMapper messageMapper = mock(AiMessageMapper.class);
        AiMessage oldUser = new AiMessage();
        oldUser.setRole("user");
        oldUser.setContent("上一轮问题");
        AiMessage oldAssistant = new AiMessage();
        oldAssistant.setRole("assistant");
        oldAssistant.setContent("上一轮回答");
        when(messageMapper.selectRecentByConversation(88L, 1001L, 6)).thenReturn(List.of(oldAssistant, oldUser));
        AiChatService service = new AiChatService(
                configService,
                client,
                new AiFeatureEntryService(),
                mock(AiContextService.class),
                conversationMapper,
                messageMapper
        );
        AiChatRequest request = new AiChatRequest();
        request.setConversationId(88L);
        request.setMessage("继续说");

        var response = service.chat(1001L, Set.of("student"), request);

        assertThat(response.getConversationId()).isEqualTo(88L);
        verify(messageMapper).selectRecentByConversation(88L, 1001L, 6);
        verify(client).chat(any(), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(org.mockito.ArgumentMatchers.any(AiMessage.class));
    }

    private AiChatService newService(AiConfigService configService, AiChatClient client) {
        return new AiChatService(
                configService,
                client,
                new AiFeatureEntryService(),
                mock(AiContextService.class),
                mock(AiConversationMapper.class),
                mock(AiMessageMapper.class)
        );
    }

    private AiProviderConfig config() {
        AiProviderConfig config = new AiProviderConfig();
        config.setProvider("deepseek");
        config.setBaseUrl("https://api.deepseek.com");
        config.setModel("deepseek-chat");
        config.setApiKeyCipher("c2stMTIz");
        config.setEnabled(true);
        config.setActive(true);
        config.setRetrievalTopK(5);
        config.setActionTopK(3);
        return config;
    }
}
