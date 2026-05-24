package com.ruc.platform.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.ai.client.AiChatClient;
import com.ruc.platform.ai.dto.AiConfigSaveDTO;
import com.ruc.platform.ai.dto.AiConfigTestDTO;
import com.ruc.platform.ai.entity.AiProviderConfig;
import com.ruc.platform.ai.mapper.AiProviderConfigMapper;
import com.ruc.platform.ai.vo.AiConfigTestVO;
import com.ruc.platform.ai.vo.AiConfigVO;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiConfigService {
    private static final String DEFAULT_DEEPSEEK_URL = "https://api.deepseek.com";

    private final AiProviderConfigMapper configMapper;
    private final AiChatClient aiChatClient;

    public List<AiConfigVO> list() {
        return configMapper.selectList(new LambdaQueryWrapper<AiProviderConfig>().orderByDesc(AiProviderConfig::getActive).orderByDesc(AiProviderConfig::getUpdatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public AiConfigVO active() {
        AiProviderConfig config = findActiveConfig();
        return config == null ? null : toVO(config);
    }

    public AiProviderConfig findActiveConfig() {
        return configMapper.selectOne(new LambdaQueryWrapper<AiProviderConfig>()
                .eq(AiProviderConfig::getActive, true)
                .eq(AiProviderConfig::getEnabled, true)
                .last("LIMIT 1"));
    }

    public Long create(Long operatorId, AiConfigSaveDTO dto) {
        AiProviderConfig config = new AiProviderConfig();
        apply(config, dto, operatorId, true);
        config.setActive(false);
        config.setCreatedAt(LocalDateTime.now());
        configMapper.insert(config);
        return config.getId();
    }

    public void update(Long operatorId, Long id, AiConfigSaveDTO dto) {
        AiProviderConfig config = requireConfig(id);
        String oldCipher = config.getApiKeyCipher();
        String oldMask = config.getApiKeyMask();
        apply(config, dto, operatorId, false);
        if (!hasText(dto.getApiKey())) {
            config.setApiKeyCipher(oldCipher);
            config.setApiKeyMask(oldMask);
        }
        configMapper.updateById(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activate(Long operatorId, Long id) {
        AiProviderConfig config = requireConfig(id);
        if (Boolean.TRUE.equals(config.getActive())) {
            config.setActive(false);
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(operatorId);
            configMapper.updateById(config);
            return;
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BizException(ResultCode.BIZ_ERROR, "配置未启用，不能设为当前使用");
        }
        configMapper.clearActive();
        config.setActive(true);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(operatorId);
        configMapper.updateById(config);
    }

    public void delete(Long id) {
        AiProviderConfig config = requireConfig(id);
        if (Boolean.TRUE.equals(config.getActive())) {
            throw new BizException(ResultCode.BIZ_ERROR, "当前启用配置不能删除");
        }
        configMapper.deleteById(id);
    }

    public AiConfigTestVO test(Long id, AiConfigTestDTO dto) {
        AiProviderConfig config = requireConfig(id);
        AiConfigTestVO vo = new AiConfigTestVO();
        vo.setProvider(config.getProvider());
        vo.setModel(config.getModel());
        long start = System.currentTimeMillis();
        try {
            String message = hasText(dto == null ? null : dto.getMessage()) ? dto.getMessage() : "请用一句话回复：连接测试成功";
            String answer = aiChatClient == null ? "连接测试成功。" : aiChatClient.chat(config, List.of(
                    new AiChatClient.Message("system", "你是连接测试助手。"),
                    new AiChatClient.Message("user", message)
            ));
            vo.setSuccess(true);
            vo.setAnswer(answer);
        } catch (Exception e) {
            vo.setSuccess(false);
            vo.setErrorMessage(e.getMessage());
        }
        vo.setLatencyMs((int) (System.currentTimeMillis() - start));
        return vo;
    }

    private void apply(AiProviderConfig config, AiConfigSaveDTO dto, Long operatorId, boolean requireKey) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "配置不能为空");
        }
        validate(dto, requireKey);
        config.setConfigName(dto.getConfigName().trim());
        config.setProvider(dto.getProvider().trim());
        config.setBaseUrl(normalizeBaseUrl(dto.getBaseUrl(), dto.getProvider()));
        config.setModel(dto.getModel().trim());
        config.setTemperature(dto.getTemperature() == null ? new BigDecimal("0.30") : dto.getTemperature());
        config.setTopP(dto.getTopP() == null ? BigDecimal.ONE : dto.getTopP());
        config.setMaxTokens(dto.getMaxTokens() == null ? 1200 : dto.getMaxTokens());
        config.setPresencePenalty(dto.getPresencePenalty() == null ? BigDecimal.ZERO : dto.getPresencePenalty());
        config.setFrequencyPenalty(dto.getFrequencyPenalty() == null ? BigDecimal.ZERO : dto.getFrequencyPenalty());
        config.setResponseFormat(dto.getResponseFormat());
        config.setTimeoutSeconds(dto.getTimeoutSeconds() == null ? 30 : dto.getTimeoutSeconds());
        config.setStreamEnabled(dto.getStreamEnabled() != null && dto.getStreamEnabled());
        config.setRetrievalTopK(dto.getRetrievalTopK() == null ? 5 : dto.getRetrievalTopK());
        config.setActionTopK(dto.getActionTopK() == null ? 3 : dto.getActionTopK());
        config.setSystemPrompt(dto.getSystemPrompt());
        config.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(operatorId);
        if (hasText(dto.getApiKey())) {
            config.setApiKeyCipher(AiKeyCodec.encrypt(dto.getApiKey().trim()));
            config.setApiKeyMask(AiKeyCodec.mask(dto.getApiKey().trim()));
        }
    }

    private void validate(AiConfigSaveDTO dto, boolean requireKey) {
        if (!hasText(dto.getConfigName())) {
            throw new BizException(ResultCode.PARAM_ERROR, "配置名称不能为空");
        }
        if (!hasText(dto.getProvider())) {
            throw new BizException(ResultCode.PARAM_ERROR, "供应商不能为空");
        }
        if (!List.of("deepseek", "openai-compatible", "openai").contains(dto.getProvider())) {
            throw new BizException(ResultCode.PARAM_ERROR, "不支持的 AI 供应商");
        }
        if (!hasText(dto.getModel())) {
            throw new BizException(ResultCode.PARAM_ERROR, "模型不能为空");
        }
        if (requireKey && !hasText(dto.getApiKey())) {
            throw new BizException(ResultCode.PARAM_ERROR, "API Key 不能为空");
        }
        checkRange(dto.getTemperature(), new BigDecimal("0"), new BigDecimal("2"), "temperature");
        checkRange(dto.getTopP(), new BigDecimal("0"), new BigDecimal("1"), "topP");
        checkInt(dto.getMaxTokens(), 1, 8192, "maxTokens");
        checkInt(dto.getTimeoutSeconds(), 5, 120, "timeoutSeconds");
        checkInt(dto.getRetrievalTopK(), 1, 10, "retrievalTopK");
        checkInt(dto.getActionTopK(), 1, 10, "actionTopK");
    }

    private void checkRange(BigDecimal value, BigDecimal min, BigDecimal max, String name) {
        if (value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0)) {
            throw new BizException(ResultCode.PARAM_ERROR, name + " 超出允许范围");
        }
    }

    private void checkInt(Integer value, int min, int max, String name) {
        if (value != null && (value < min || value > max)) {
            throw new BizException(ResultCode.PARAM_ERROR, name + " 超出允许范围");
        }
    }

    private AiProviderConfig requireConfig(Long id) {
        AiProviderConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BizException(ResultCode.NOT_FOUND, "AI 配置不存在");
        }
        return config;
    }

    public String decryptApiKey(AiProviderConfig config) {
        return AiKeyCodec.decrypt(config.getApiKeyCipher());
    }

    private AiConfigVO toVO(AiProviderConfig config) {
        AiConfigVO vo = new AiConfigVO();
        vo.setId(config.getId());
        vo.setConfigName(config.getConfigName());
        vo.setProvider(config.getProvider());
        vo.setBaseUrl(config.getBaseUrl());
        vo.setApiKeyMask(config.getApiKeyMask());
        vo.setHasApiKey(hasText(config.getApiKeyCipher()));
        vo.setModel(config.getModel());
        vo.setTemperature(config.getTemperature());
        vo.setTopP(config.getTopP());
        vo.setMaxTokens(config.getMaxTokens());
        vo.setPresencePenalty(config.getPresencePenalty());
        vo.setFrequencyPenalty(config.getFrequencyPenalty());
        vo.setResponseFormat(config.getResponseFormat());
        vo.setTimeoutSeconds(config.getTimeoutSeconds());
        vo.setStreamEnabled(config.getStreamEnabled());
        vo.setRetrievalTopK(config.getRetrievalTopK());
        vo.setActionTopK(config.getActionTopK());
        vo.setSystemPrompt(config.getSystemPrompt());
        vo.setEnabled(config.getEnabled());
        vo.setActive(config.getActive());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        vo.setUpdatedBy(config.getUpdatedBy());
        return vo;
    }

    private String normalizeBaseUrl(String baseUrl, String provider) {
        String value = hasText(baseUrl) ? baseUrl.trim() : ("deepseek".equals(provider) ? DEFAULT_DEEPSEEK_URL : "");
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new BizException(ResultCode.PARAM_ERROR, "Base URL 必须以 http:// 或 https:// 开头");
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
