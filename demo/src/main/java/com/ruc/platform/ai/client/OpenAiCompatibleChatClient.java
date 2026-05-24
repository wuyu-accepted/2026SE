package com.ruc.platform.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.ai.entity.AiProviderConfig;
import com.ruc.platform.ai.service.AiKeyCodec;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleChatClient implements AiChatClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String chat(AiProviderConfig config, List<Message> messages) {
        try {
            String apiKey = AiKeyCodec.decrypt(config.getApiKeyCipher());
            if (apiKey == null || apiKey.isBlank()) {
                throw new BizException(ResultCode.BIZ_ERROR, "AI 配置缺少 API Key");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModel());
            body.put("messages", messages.stream().map(message -> Map.of("role", message.role(), "content", message.content())).toList());
            if (config.getTemperature() != null) {
                body.put("temperature", config.getTemperature());
            }
            if (config.getTopP() != null) {
                body.put("top_p", config.getTopP());
            }
            if (config.getMaxTokens() != null) {
                body.put("max_tokens", config.getMaxTokens());
            }
            if (config.getPresencePenalty() != null) {
                body.put("presence_penalty", config.getPresencePenalty());
            }
            if (config.getFrequencyPenalty() != null) {
                body.put("frequency_penalty", config.getFrequencyPenalty());
            }
            if (config.getResponseFormat() != null && !config.getResponseFormat().isBlank()) {
                body.put("response_format", Map.of("type", config.getResponseFormat()));
            }
            body.put("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds() == null ? 30 : config.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ResultCode.BIZ_ERROR, "AI 供应商返回错误：" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BizException(ResultCode.BIZ_ERROR, "AI 供应商返回空内容");
            }
            return content.asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.BIZ_ERROR, "AI 调用失败：" + e.getMessage());
        }
    }
}
