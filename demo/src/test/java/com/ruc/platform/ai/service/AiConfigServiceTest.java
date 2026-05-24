package com.ruc.platform.ai.service;

import com.ruc.platform.ai.dto.AiConfigSaveDTO;
import com.ruc.platform.ai.entity.AiProviderConfig;
import com.ruc.platform.ai.mapper.AiProviderConfigMapper;
import com.ruc.platform.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConfigServiceTest {

    @Test
    void createConfigMasksAndStoresApiKey() {
        AiProviderConfigMapper mapper = mock(AiProviderConfigMapper.class);
        AiConfigService service = new AiConfigService(mapper, null);
        AiConfigSaveDTO dto = deepSeekDto();
        dto.setApiKey("sk-1234567890abcdef");

        service.create(88L, dto);

        ArgumentCaptor<AiProviderConfig> captor = ArgumentCaptor.forClass(AiProviderConfig.class);
        verify(mapper).insert(captor.capture());
        AiProviderConfig saved = captor.getValue();
        assertThat(saved.getProvider()).isEqualTo("deepseek");
        assertThat(saved.getBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(saved.getApiKeyCipher()).isNotEqualTo("sk-1234567890abcdef");
        assertThat(saved.getApiKeyMask()).isEqualTo("sk-****cdef");
        assertThat(saved.getUpdatedBy()).isEqualTo(88L);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getActive()).isFalse();
    }

    @Test
    void apiKeyCodecUsesAuthenticatedEncryptionAndRejectsTampering() {
        String cipher = AiKeyCodec.encrypt("sk-test-secret");

        assertThat(cipher).startsWith("v1:");
        assertThat(cipher).doesNotContain("sk-test-secret");
        assertThat(AiKeyCodec.decrypt(cipher)).isEqualTo("sk-test-secret");
        assertThatThrownBy(() -> AiKeyCodec.decrypt(cipher.substring(0, cipher.length() - 2) + "xx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API Key 解密失败");
    }

    @Test
    void updateConfigRetainsExistingApiKeyWhenBlank() {
        AiProviderConfigMapper mapper = mock(AiProviderConfigMapper.class);
        AiProviderConfig existing = new AiProviderConfig();
        existing.setId(10L);
        existing.setApiKeyCipher("encrypted-key");
        existing.setApiKeyMask("sk-****1111");
        when(mapper.selectById(10L)).thenReturn(existing);
        AiConfigService service = new AiConfigService(mapper, null);
        AiConfigSaveDTO dto = deepSeekDto();
        dto.setApiKey("");
        dto.setModel("deepseek-reasoner");

        service.update(99L, 10L, dto);

        ArgumentCaptor<AiProviderConfig> captor = ArgumentCaptor.forClass(AiProviderConfig.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getApiKeyCipher()).isEqualTo("encrypted-key");
        assertThat(captor.getValue().getApiKeyMask()).isEqualTo("sk-****1111");
        assertThat(captor.getValue().getModel()).isEqualTo("deepseek-reasoner");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(99L);
    }

    @Test
    void activateConfigClearsOthersAndActivatesTarget() {
        AiProviderConfigMapper mapper = mock(AiProviderConfigMapper.class);
        AiProviderConfig existing = new AiProviderConfig();
        existing.setId(12L);
        existing.setEnabled(true);
        when(mapper.selectById(12L)).thenReturn(existing);
        AiConfigService service = new AiConfigService(mapper, null);

        service.activate(77L, 12L);

        verify(mapper).clearActive();
        ArgumentCaptor<AiProviderConfig> captor = ArgumentCaptor.forClass(AiProviderConfig.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getActive()).isTrue();
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(77L);
    }

    @Test
    void activateCurrentConfigClearsActiveFlag() {
        AiProviderConfigMapper mapper = mock(AiProviderConfigMapper.class);
        AiProviderConfig existing = new AiProviderConfig();
        existing.setId(12L);
        existing.setActive(true);
        existing.setEnabled(true);
        when(mapper.selectById(12L)).thenReturn(existing);
        AiConfigService service = new AiConfigService(mapper, null);

        service.activate(77L, 12L);

        ArgumentCaptor<AiProviderConfig> captor = ArgumentCaptor.forClass(AiProviderConfig.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getActive()).isFalse();
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(77L);
    }

    @Test
    void deleteActiveConfigIsRejected() {
        AiProviderConfigMapper mapper = mock(AiProviderConfigMapper.class);
        AiProviderConfig existing = new AiProviderConfig();
        existing.setId(13L);
        existing.setActive(true);
        when(mapper.selectById(13L)).thenReturn(existing);
        AiConfigService service = new AiConfigService(mapper, null);

        assertThatThrownBy(() -> service.delete(13L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前启用");
    }

    @Test
    void listReturnsMaskedConfigsOnly() {
        AiProviderConfigMapper mapper = mock(AiProviderConfigMapper.class);
        AiProviderConfig config = new AiProviderConfig();
        config.setId(1L);
        config.setConfigName("DeepSeek 默认");
        config.setProvider("deepseek");
        config.setBaseUrl("https://api.deepseek.com");
        config.setApiKeyCipher("secret-cipher");
        config.setApiKeyMask("sk-****abcd");
        config.setModel("deepseek-chat");
        config.setEnabled(true);
        config.setActive(true);
        when(mapper.selectList(any())).thenReturn(List.of(config));
        AiConfigService service = new AiConfigService(mapper, null);

        var list = service.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getApiKeyMask()).isEqualTo("sk-****abcd");
        assertThat(list.get(0).getHasApiKey()).isTrue();
    }

    private AiConfigSaveDTO deepSeekDto() {
        AiConfigSaveDTO dto = new AiConfigSaveDTO();
        dto.setConfigName("DeepSeek 默认");
        dto.setProvider("deepseek");
        dto.setBaseUrl("https://api.deepseek.com/");
        dto.setModel("deepseek-chat");
        dto.setTemperature(new java.math.BigDecimal("0.3"));
        dto.setTopP(new java.math.BigDecimal("1.0"));
        dto.setMaxTokens(1200);
        dto.setTimeoutSeconds(30);
        dto.setRetrievalTopK(5);
        dto.setActionTopK(3);
        dto.setEnabled(true);
        return dto;
    }
}
