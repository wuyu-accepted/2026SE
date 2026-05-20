package com.ruc.platform.admin.party.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.admin.party.dto.*;
import com.ruc.platform.admin.party.mapper.AdminPartyMapper;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.mapper.UserMapper;
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
    private final PartyStageHistoryMapper stageHistoryMapper;
    private final PartyReportMapper reportMapper;
    private final PartyActivityApplicationMapper activityApplicationMapper;
    private final UserMapper userMapper;

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
        PartyStudentProgressQueryDTO actualQuery = query == null ? new PartyStudentProgressQueryDTO() : query;
        IPage<?> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        List<PartyStudentProgressVO> records = adminPartyMapper.selectStudentProgressPage(page, actualQuery);
        Long total = adminPartyMapper.countStudentProgress(actualQuery);
        return PageResult.of(total == null ? 0L : total, actualQuery.getPageNum(), actualQuery.getPageSize(),
                records == null ? Collections.emptyList() : records);
    }

    @Override
    public PartyStudentProgressAdminVO getStudentProgressDetail(String studentNo, String realName) {
        User user = resolveUser(studentNo, realName);
        PartyStudentProgress progress = progressMapper.selectByUserId(user.getId());
        List<PartyStageHistory> histories = stageHistoryMapper.selectByUserId(user.getId());

        PartyStudentProgressAdminVO vo = new PartyStudentProgressAdminVO();
        vo.setUserId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setRealName(user.getRealName());
        if (progress != null) {
            vo.setCurrentStageCode(progress.getCurrentStageCode());
            vo.setCurrentStepCode(progress.getCurrentStepCode());
        }
        if (histories == null || histories.isEmpty()) {
            vo.setStageHistories(Collections.emptyList());
            return vo;
        }

        List<PartyStageHistoryItemVO> items = histories.stream().map(history -> {
            PartyStageHistoryItemVO item = new PartyStageHistoryItemVO();
            item.setStageCode(history.getStageCode());
            item.setStartTime(history.getStartTime());
            item.setEndTime(history.getEndTime());
            item.setRemark(history.getRemark());
            return item;
        }).toList();
        vo.setStageHistories(items);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "导入数据不能为空");
        }
        for (BatchImportProgressDTO.Item item : dto.getItems()) {
            if (item == null || item.getUserId() == null) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "导入数据缺少 userId（当前版本支持按学号/姓名导入，请刷新前端或改用新导入格式）");
            }
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
        ReportReviewQueryDTO actualQuery = query == null ? new ReportReviewQueryDTO() : query;
        IPage<?> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        List<PartyReportVO> records = adminPartyMapper.selectReportPage(page, actualQuery);
        Long total = adminPartyMapper.countReport(actualQuery);
        return PageResult.of(total == null ? 0L : total, actualQuery.getPageNum(), actualQuery.getPageSize(),
                records == null ? Collections.emptyList() : records);
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
        User user = userMapper.selectById(report.getUserId());
        if (user != null) {
            vo.setStudentNo(user.getStudentNo());
            vo.setRealName(user.getRealName());
        }
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
        ActivityReviewQueryDTO actualQuery = query == null ? new ActivityReviewQueryDTO() : query;
        IPage<?> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        List<PartyActivityVO> records = adminPartyMapper.selectActivityPage(page, actualQuery);
        Long total = adminPartyMapper.countActivity(actualQuery);
        return PageResult.of(total == null ? 0L : total, actualQuery.getPageNum(), actualQuery.getPageSize(),
                records == null ? Collections.emptyList() : records);
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
        User user = userMapper.selectById(app.getUserId());
        if (user != null) {
            vo.setStudentNo(user.getStudentNo());
            vo.setRealName(user.getRealName());
        }
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

    private User resolveUser(String studentNo, String realName) {
        String studentNoValue = studentNo == null ? "" : studentNo.trim();
        String realNameValue = realName == null ? "" : realName.trim();
        if (studentNoValue.isEmpty() && realNameValue.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "学号或姓名至少填写一个");
        }
        if (!studentNoValue.isEmpty()) {
            User user = userMapper.selectByStudentNo(studentNoValue);
            if (user == null) {
                throw new BizException(ResultCode.NOT_FOUND, "学号不存在: " + studentNoValue);
            }
            return user;
        }
        List<User> users = userMapper.selectByRealName(realNameValue);
        if (users == null || users.isEmpty()) {
            throw new BizException(ResultCode.NOT_FOUND, "姓名不存在: " + realNameValue);
        }
        if (users.size() > 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "姓名匹配到多个学生，请使用学号查询");
        }
        return users.get(0);
    }
}
