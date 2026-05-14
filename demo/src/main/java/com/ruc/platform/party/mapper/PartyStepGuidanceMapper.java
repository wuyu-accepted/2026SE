package com.ruc.platform.party.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.party.entity.PartyStepGuidance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PartyStepGuidanceMapper extends BaseMapper<PartyStepGuidance> {

    @Select("SELECT * FROM party_step_guidance WHERE step_code = #{stepCode} AND status = 1 ORDER BY priority ASC, created_at DESC")
    List<PartyStepGuidance> selectEnabledByStepCode(@Param("stepCode") String stepCode);
}
