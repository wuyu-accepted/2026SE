package com.ruc.platform.admin.party.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruc.platform.admin.party.dto.ActivityReviewQueryDTO;
import com.ruc.platform.admin.party.dto.PartyStudentProgressQueryDTO;
import com.ruc.platform.admin.party.dto.ReportReviewQueryDTO;
import com.ruc.platform.admin.party.vo.PartyActivityVO;
import com.ruc.platform.admin.party.vo.PartyReportVO;
import com.ruc.platform.admin.party.vo.PartyStudentProgressVO;
import com.ruc.platform.party.entity.PartyStageDef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminPartyMapper {

    List<PartyStudentProgressVO> selectStudentProgressPage(IPage<?> page, @Param("q") PartyStudentProgressQueryDTO query);
    Long countStudentProgress(@Param("q") PartyStudentProgressQueryDTO query);

    List<PartyReportVO> selectReportPage(IPage<?> page, @Param("q") ReportReviewQueryDTO query);
    Long countReport(@Param("q") ReportReviewQueryDTO query);

    List<PartyActivityVO> selectActivityPage(IPage<?> page, @Param("q") ActivityReviewQueryDTO query);
    Long countActivity(@Param("q") ActivityReviewQueryDTO query);

    @Select("SELECT * FROM party_stage_def WHERE status = 1 ORDER BY sort_order ASC")
    List<PartyStageDef> selectEnabledStages();
}
