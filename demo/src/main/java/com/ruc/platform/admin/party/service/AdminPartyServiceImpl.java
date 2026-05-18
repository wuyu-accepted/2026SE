package com.ruc.platform.admin.party.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.admin.party.dto.*;
import com.ruc.platform.admin.party.mapper.AdminPartyMapper;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.party.entity.*;
import com.ruc.platform.party.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPartyServiceImpl implements AdminPartyService {

    private final AdminPartyMapper adminPartyMapper;
    private final PartyStageDefMapper stageDefMapper;
    private final PartyStepDefMapper stepDefMapper;
    private final PartyStudentProgressMapper progressMapper;
    private final PartyReportMapper reportMapper;
    private final PartyActivityApplicationMapper activityApplicationMapper;

    @Override
    public List<Map<String, Object>> listStages() {
        List<PartyStageDef> list = stageDefMapper.selectList(new LambdaQueryWrapper<PartyStageDef>()
                .orderByAsc(PartyStageDef::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PartyStageDef s : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("stageCode", s.getStageCode());
            m.put("stageName", s.getStageName());
            m.put("sortOrder", s.getSortOrder());
            m.put("description", s.getDescription());
            m.put("status", s.getStatus());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listSteps(String stageCode) {
        List<PartyStepDef> list = stepDefMapper.selectList(new LambdaQueryWrapper<PartyStepDef>()
                .eq(PartyStepDef::getStageCode, stageCode)
                .orderByAsc(PartyStepDef::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PartyStepDef s : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("stageCode", s.getStageCode());
            m.put("stepCode", s.getStepCode());
            m.put("stepName", s.getStepName());
            m.put("sortOrder", s.getSortOrder());
            m.put("description", s.getDescription());
            m.put("status", s.getStatus());
            result.add(m);
        }
        return result;
    }

    @Override
    public PageResult<PartyStudentProgressVO> listStudentProgress(PartyStudentProgressQueryDTO query) {
        IPage<?> page = new Page<>(query.getPageNum(), query.getPageSize());
        List<PartyStudentProgressVO> records = adminPartyMapper.selectStudentProgressPage(page, query);
        Long total = adminPartyMapper.countStudentProgress(query);
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    @Transactional
    public void updateStudentProgress(Long userId, UpdateProgressDTO dto) {
        PartyStudentProgress progress = progressMapper.selectByUserId(userId);
        if (progress == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该学生暂无党团进度记录");
        }
        progress.setCurrentStageCode(dto.getStageCode());
        progress.setCurrentStepCode(dto.getStepCode());
        progress.setUpdatedAt(LocalDateTime.now());
        progressMapper.updateById(progress);
        log.info("更新学生党团进度，userId: {}, stage: {}, step: {}", userId, dto.getStageCode(), dto.getStepCode());
    }

    @Override
    @Transactional
    public void batchImportProgress(BatchImportProgressDTO dto) {
        if (dto.getItems() == null) return;
        for (BatchImportProgressDTO.Item item : dto.getItems()) {
            PartyStudentProgress progress = progressMapper.selectByUserId(item.getUserId());
            if (progress == null) {
                progress = new PartyStudentProgress();
                progress.setUserId(item.getUserId());
                progress.setCurrentStageCode(item.getStageCode());
                progress.setCurrentStepCode(item.getStepCode());
                progress.setUpdatedAt(LocalDateTime.now());
                progressMapper.insert(progress);
            } else {
                progress.setCurrentStageCode(item.getStageCode());
                progress.setCurrentStepCode(item.getStepCode());
                progress.setUpdatedAt(LocalDateTime.now());
                progressMapper.updateById(progress);
            }
        }
        log.info("批量导入党团进度，共 {} 条", dto.getItems().size());
    }

    @Override
    public PageResult<PartyReportVO> listReports(ReportReviewQueryDTO query) {
        IPage<?> page = new Page<>(query.getPageNum(), query.getPageSize());
        List<PartyReportVO> records = adminPartyMapper.selectReportPage(page, query);
        Long total = adminPartyMapper.countReport(query);
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public PartyReportVO getReportDetail(Long id) {
        PartyReportVO vo = new PartyReportVO();
        PartyReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "思想汇报不存在");
        }
        vo.setId(report.getId());
        vo.setUserId(report.getUserId());
        vo.setStageCode(report.getStageCode());
        vo.setTitle(report.getTitle());
        vo.setContent(report.getContent());
        vo.setFileId(report.getFileId());
        vo.setSubmitTime(report.getSubmitTime());
        vo.setStatus(report.getStatus());
        vo.setReviewComment(report.getReviewComment());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        return vo;
    }

    @Override
    @Transactional
    public void approveReport(Long id, Long reviewerId, ReportReviewDTO dto) {
        PartyReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "思想汇报不存在");
        }
        report.setStatus(1);
        report.setReviewComment(dto.getComment());
        report.setReviewedBy(reviewerId);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.updateById(report);
        log.info("思想汇报审核通过，id: {}, reviewerId: {}", id, reviewerId);
    }

    @Override
    @Transactional
    public void rejectReport(Long id, Long reviewerId, ReportReviewDTO dto) {
        PartyReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "思想汇报不存在");
        }
        report.setStatus(2);
        report.setReviewComment(dto.getComment());
        report.setReviewedBy(reviewerId);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.updateById(report);
        log.info("思想汇报驳回，id: {}, reviewerId: {}", id, reviewerId);
    }

    @Override
    public PageResult<PartyActivityVO> listActivities(ActivityReviewQueryDTO query) {
        IPage<?> page = new Page<>(query.getPageNum(), query.getPageSize());
        List<PartyActivityVO> records = adminPartyMapper.selectActivityPage(page, query);
        Long total = adminPartyMapper.countActivity(query);
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public PartyActivityVO getActivityDetail(Long id) {
        PartyActivityApplication app = activityApplicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "活动申请不存在");
        }
        PartyActivityVO vo = new PartyActivityVO();
        vo.setId(app.getId());
        vo.setUserId(app.getUserId());
        vo.setTitle(app.getTitle());
        vo.setReason(app.getReason());
        vo.setEventDate(app.getEventDate() == null ? null : app.getEventDate().toString());
        vo.setStatus(app.getStatus());
        vo.setReviewComment(app.getReviewComment());
        vo.setSubmitTime(app.getSubmitTime());
        vo.setReviewedAt(app.getReviewedAt());
        return vo;
    }

    @Override
    @Transactional
    public void approveActivity(Long id, Long reviewerId, ActivityReviewDTO dto) {
        PartyActivityApplication app = activityApplicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "活动申请不存在");
        }
        app.setStatus(1);
        app.setReviewComment(dto.getComment());
        app.setReviewedBy(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        activityApplicationMapper.updateById(app);
        log.info("活动申请审核通过，id: {}, reviewerId: {}", id, reviewerId);
    }

    @Override
    @Transactional
    public void rejectActivity(Long id, Long reviewerId, ActivityReviewDTO dto) {
        PartyActivityApplication app = activityApplicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "活动申请不存在");
        }
        app.setStatus(2);
        app.setReviewComment(dto.getComment());
        app.setReviewedBy(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        activityApplicationMapper.updateById(app);
        log.info("活动申请驳回，id: {}, reviewerId: {}", id, reviewerId);
    }
}
