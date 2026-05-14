package com.ruc.platform.party.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.party.entity.PartyStepDef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PartyStepDefMapper extends BaseMapper<PartyStepDef> {

    @Select("SELECT * FROM party_step_def WHERE stage_code = #{stageCode} AND status = 1 ORDER BY sort_order ASC")
    List<PartyStepDef> selectEnabledByStageCode(@Param("stageCode") String stageCode);
}
