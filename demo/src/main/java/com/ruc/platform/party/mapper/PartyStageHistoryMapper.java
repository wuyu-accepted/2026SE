package com.ruc.platform.party.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.party.entity.PartyStageHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PartyStageHistoryMapper extends BaseMapper<PartyStageHistory> {

    @Select("SELECT * FROM party_stage_history WHERE user_id = #{userId} ORDER BY (start_time IS NULL) ASC, start_time DESC, created_at DESC")
    List<PartyStageHistory> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM party_stage_history WHERE user_id = #{userId} AND stage_code = #{stageCode} ORDER BY (start_time IS NULL) ASC, start_time DESC, created_at DESC LIMIT 1")
    PartyStageHistory selectLatestByUserIdAndStageCode(@Param("userId") Long userId, @Param("stageCode") String stageCode);
}
