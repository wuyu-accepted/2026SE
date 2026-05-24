package com.ruc.platform.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.ai.client.AiChatClient;
import com.ruc.platform.ai.dto.AiChatRequest;
import com.ruc.platform.ai.entity.AiConversation;
import com.ruc.platform.ai.entity.AiMessage;
import com.ruc.platform.ai.entity.AiProviderConfig;
import com.ruc.platform.ai.mapper.AiConversationMapper;
import com.ruc.platform.ai.mapper.AiMessageMapper;
import com.ruc.platform.ai.vo.AiActionVO;
import com.ruc.platform.ai.vo.AiChatResponse;
import com.ruc.platform.ai.vo.AiCitationVO;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiChatService {
    private static final String BUILTIN_SYSTEM_PROMPT = """
            你是学院学生综合服务与党团管理平台的 AI 助手。
            只能基于系统提供的可见知识、用户上下文和可用功能入口回答。
            不能编造政策、通知、入口、审批结果或个人数据。
            如果上下文不足，明确说明暂未找到依据，并建议查看相关模块或联系辅导员。
            回答应简洁、中文、适合学生阅读。
            """;

    private final AiConfigService configService;
    private final AiChatClient aiChatClient;
    private final AiFeatureEntryService featureEntryService;
    private final AiContextService contextService;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiChatResponse chat(Long userId, Set<String> roles, AiChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "问题不能为空");
        }
        String question = request.getMessage().trim();
        if (question.length() > 1000) {
            throw new BizException(ResultCode.PARAM_ERROR, "问题不能超过 1000 字");
        }
        AiProviderConfig config = configService.findActiveConfig();
        int retrievalTopK = config == null || config.getRetrievalTopK() == null ? 5 : config.getRetrievalTopK();
        int actionTopK = config == null || config.getActionTopK() == null ? 3 : config.getActionTopK();
        List<AiCitationVO> citations = contextService.searchKnowledge(question, retrievalTopK);
        List<AiActionVO> actions = featureEntryService.match(question, roles, List.of(), actionTopK);
        Long conversationId = ensureConversation(userId, request.getConversationId(), question);
        saveUserMessage(userId, conversationId, question);

        if (config == null || config.getApiKeyCipher() == null || config.getApiKeyCipher().isBlank() || aiChatClient == null) {
            return fallback(userId, conversationId, question, citations, actions, null, null);
        }
        long start = System.currentTimeMillis();
        try {
            String answer = aiChatClient.chat(config, buildMessages(userId, conversationId, config, question, citations, actions, roles));
            AiChatResponse response = response(conversationId, answer, false, citations, actions);
            saveAssistantMessage(userId, conversationId, answer, config, response, "success", null, (int) (System.currentTimeMillis() - start));
            return response;
        } catch (Exception e) {
            return fallback(userId, conversationId, question, citations, actions, config, e.getMessage());
        }
    }

    private List<AiChatClient.Message> buildMessages(Long userId, Long conversationId, AiProviderConfig config, String question, List<AiCitationVO> citations, List<AiActionVO> actions, Set<String> roles) {
        List<AiChatClient.Message> messages = new ArrayList<>();
        String systemPrompt = BUILTIN_SYSTEM_PROMPT + "\n" + (config.getSystemPrompt() == null ? "" : config.getSystemPrompt());
        messages.add(new AiChatClient.Message("system", systemPrompt));
        messages.addAll(recentMessages(userId, conversationId));
        messages.add(new AiChatClient.Message("user", buildUserPrompt(question, citations, actions, roles)));
        return messages;
    }

    private List<AiChatClient.Message> recentMessages(Long userId, Long conversationId) {
        if (messageMapper == null || conversationId == null) {
            return List.of();
        }
        try {
            List<AiMessage> recent = messageMapper.selectRecentByConversation(conversationId, userId, 6);
            List<AiChatClient.Message> messages = new ArrayList<>();
            for (int i = recent.size() - 1; i >= 0; i--) {
                AiMessage message = recent.get(i);
                if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
                    messages.add(new AiChatClient.Message(message.getRole(), message.getContent()));
                }
            }
            return messages;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String buildUserPrompt(String question, List<AiCitationVO> citations, List<AiActionVO> actions, Set<String> roles) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(question).append("\n\n");
        builder.append("用户角色：").append(roles == null ? "" : String.join(",", roles)).append("\n\n");
        builder.append("可见知识：\n");
        if (citations.isEmpty()) {
            builder.append("无\n");
        } else {
            for (int i = 0; i < citations.size(); i++) {
                AiCitationVO citation = citations.get(i);
                builder.append(i + 1).append(". ").append(citation.getTitle()).append("：").append(nullToEmpty(citation.getSummary())).append("\n");
            }
        }
        builder.append("\n可用功能入口：\n");
        if (actions.isEmpty()) {
            builder.append("无\n");
        } else {
            for (int i = 0; i < actions.size(); i++) {
                AiActionVO action = actions.get(i);
                builder.append(i + 1).append(". ").append(action.getTitle()).append("，路径：").append(action.getPath()).append("，说明：").append(action.getDescription()).append("\n");
            }
        }
        return builder.toString();
    }

    private AiChatResponse fallback(Long userId, Long conversationId, String question, List<AiCitationVO> citations, List<AiActionVO> actions, AiProviderConfig config, String error) {
        StringBuilder answer = new StringBuilder("我暂时无法连接 AI 服务，但可以先根据平台资料和功能入口为你提供建议。");
        if (!actions.isEmpty()) {
            answer.append("你可以查看“").append(actions.get(0).getTitle()).append("”。");
        } else if (!citations.isEmpty()) {
            answer.append("你可以先阅读“").append(citations.get(0).getTitle()).append("”。");
        } else {
            answer.append("暂未找到明确资料，建议查看服务页或联系辅导员。");
        }
        AiChatResponse response = response(conversationId, answer.toString(), true, citations, actions);
        saveAssistantMessage(userId, conversationId, response.getAnswer(), config, response, "fallback", error, null);
        return response;
    }

    private AiChatResponse response(Long conversationId, String answer, boolean fallback, List<AiCitationVO> citations, List<AiActionVO> actions) {
        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversationId);
        response.setAnswer(answer);
        response.setFallback(fallback);
        response.setCitations(citations);
        response.setActions(actions);
        return response;
    }

    private Long ensureConversation(Long userId, Long conversationId, String question) {
        if (conversationId != null) {
            return conversationId;
        }
        if (conversationMapper == null) {
            return null;
        }
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(question.length() > 30 ? question.substring(0, 30) : question);
        conversation.setClientType("miniprogram");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    private void saveAssistantMessage(Long userId, Long conversationId, String content, AiProviderConfig config, AiChatResponse response, String status, String error, Integer latencyMs) {
        if (messageMapper == null) {
            return;
        }
        try {
            AiMessage message = new AiMessage();
            message.setConversationId(conversationId);
            message.setUserId(userId);
            message.setRole("assistant");
            message.setContent(content);
            message.setProvider(config == null ? null : config.getProvider());
            message.setModel(config == null ? null : config.getModel());
            message.setCitationsJson(objectMapper.writeValueAsString(response.getCitations()));
            message.setActionsJson(objectMapper.writeValueAsString(response.getActions()));
            message.setLatencyMs(latencyMs);
            message.setStatus(status);
            message.setErrorMessage(error == null ? null : error.substring(0, Math.min(500, error.length())));
            message.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(message);
        } catch (Exception ignored) {
        }
    }

    private void saveUserMessage(Long userId, Long conversationId, String content) {
        if (messageMapper == null) {
            return;
        }
        try {
            AiMessage message = new AiMessage();
            message.setConversationId(conversationId);
            message.setUserId(userId);
            message.setRole("user");
            message.setContent(content);
            message.setStatus("success");
            message.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(message);
        } catch (Exception ignored) {
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
