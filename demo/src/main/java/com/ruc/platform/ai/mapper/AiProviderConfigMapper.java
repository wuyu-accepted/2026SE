package com.ruc.platform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.ai.entity.AiProviderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiProviderConfigMapper extends BaseMapper<AiProviderConfig> {
    @Update("UPDATE ai_provider_config SET active = FALSE WHERE active = TRUE")
    void clearActive();
}
