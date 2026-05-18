package com.ruc.platform.admin.party.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.admin.party.dto.PartyProgressBatchImportDTO;
import com.ruc.platform.admin.party.dto.PartyProgressImportItemDTO;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.party.entity.*;
import com.ruc.platform.party.mapper.*;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPartyServiceImpl implements AdminPartyService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserMapper userMapper;
    private final PartyStudentProgressMapper progressMapper;
    private final PartyStageDefMapper stageDefMapper;
    private final PartyStepDefMapper stepDefMapper;
    private final PartyStageHistoryMapper stageHistoryMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final PartyReportMapper reportMapper;
    private final PartyActivityApplicationMapper activityApplicationMapper;

    @Override
    public List<PartyStageOptionVO> listStageOptions() {
        List<PartyStageDef> stageDefs = stageDefMapper.selectList(
                new LambdaQueryWrapper<PartyStageDef>()
                        .eq(PartyStageDef::getStatus, 1)
                        .orderByAsc(PartyStageDef::getSortOrder)
        );
        if (stageDefs == null || stageDefs.isEmpty()) {
            return Collections.emptyList();
        }
        return stageDefs.stream().map(def -> {
            PartyStageOptionVO vo = new PartyStageOptionVO();
            vo.setStageCode(def.getStageCode());
            vo.setStageName(def.getStageName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PartyStepOptionVO> listStepOptions(String stageCode) {
        String normalized = safeTrim(stageCode);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ResultCode.PARAM_ERROR, "stageCode不能为空");
        }
        List<PartyStepDef> stepDefs = stepDefMapper.selectEnabledByStageCode(normalized);
        if (stepDefs == null || stepDefs.isEmpty()) {
            return Collections.emptyList();
        }
        return stepDefs.stream().map(def -> {
            PartyStepOptionVO vo = new PartyStepOptionVO();
            vo.setStepCode(def.getStepCode());
            vo.setStepName(def.getStepName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PartyProgressBatchImportResultVO batchImportProgress(Long operatorUserId, PartyProgressBatchImportDTO dto) {
        List<PartyProgressImportItemDTO> items = dto == null ? null : dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "导入数据不能为空");
        }

        List<PartyStageDef> stageDefs = stageDefMapper.selectList(
                new LambdaQueryWrapper<PartyStageDef>()
                        .eq(PartyStageDef::getStatus, 1)
                        .orderByAsc(PartyStageDef::getSortOrder)
        );
        Map<String, PartyStageDef> stageMap = stageDefs.stream()
                .collect(Collectors.toMap(PartyStageDef::getStageCode, s -> s, (a, b) -> a));

        List<PartyProgressImportRowVO> rows = new ArrayList<>(items.size());
        int success = 0;

        for (int i = 0; i < items.size(); i++) {
            PartyProgressImportItemDTO item = items.get(i);
            PartyProgressImportRowVO row = new PartyProgressImportRowVO();
            row.setRowNo(i + 1);
            row.setStudentNo(item == null ? null : safeTrim(item.getStudentNo()));
            row.setRealName(item == null ? null : safeTrim(item.getRealName()));
            row.setStageCode(item == null ? null : safeTrim(item.getStageCode()));
            row.setStepCode(item == null ? null : safeTrim(item.getStepCode()));
            try {
                User user = processProgressRow(operatorUserId, item, stageMap);
                row.setStudentNo(user.getStudentNo());
                row.setRealName(user.getRealName());
                row.setSuccess(true);
                row.setMessage("OK");
                success++;
            } catch (BizException e) {
                row.setSuccess(false);
                row.setMessage(e.getMessage());
            } catch (Exception e) {
                row.setSuccess(false);
                row.setMessage("系统错误");
            }
            rows.add(row);
        }

        PartyProgressBatchImportResultVO result = new PartyProgressBatchImportResultVO();
        result.setTotal(items.size());
        result.setSuccessCount(success);
        result.setFailCount(items.size() - success);
        result.setRows(rows);
        return result;
    }

    @Override
    public PartyStudentProgressAdminVO queryStudentProgress(String studentNo, String realName) {
        User user = resolveUser(studentNo, realName);
        PartyStudentProgress progress = progressMapper.selectByUserId(user.getId());

        List<PartyStageHistory> histories = stageHistoryMapper.selectByUserId(user.getId());
        List<PartyStageHistoryItemVO> historyVOs = histories.stream().map(h -> {
            PartyStageHistoryItemVO vo = new PartyStageHistoryItemVO();
            vo.setStageCode(h.getStageCode());
            vo.setStartTime(h.getStartTime());
            vo.setEndTime(h.getEndTime());
            vo.setRemark(h.getRemark());
            return vo;
        }).collect(Collectors.toList());

        PartyStudentProgressAdminVO vo = new PartyStudentProgressAdminVO();
        vo.setUserId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setRealName(user.getRealName());
        if (progress != null) {
            vo.setCurrentStageCode(progress.getCurrentStageCode());
            vo.setCurrentStepCode(progress.getCurrentStepCode());
        }
        vo.setStageHistories(historyVOs);
        return vo;
    }

    @Override
    public List<PartyReportAdminListItemVO> listReports(Integer status) {
        LambdaQueryWrapper<PartyReport> wrapper = new LambdaQueryWrapper<PartyReport>()
                .orderByDesc(PartyReport::getSubmitTime)
                .orderByDesc(PartyReport::getCreatedAt);
        if (status != null) {
            wrapper.eq(PartyReport::getStatus, status);
        }
        List<PartyReport> reports = reportMapper.selectList(wrapper);
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = loadUsersById(reports.stream().map(PartyReport::getUserId).filter(Objects::nonNull).distinct().toList());

        return reports.stream().map(r -> {
            User u = userMap.get(r.getUserId());
            PartyReportAdminListItemVO vo = new PartyReportAdminListItemVO();
            vo.setId(r.getId());
            vo.setStudentNo(u == null ? "" : u.getStudentNo());
            vo.setRealName(u == null ? "" : u.getRealName());
            vo.setTitle(r.getTitle());
            vo.setFileId(r.getFileId());
            vo.setStatus(r.getStatus());
            vo.setSubmitTime(r.getSubmitTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PartyReportAdminDetailVO getReportDetail(Long id) {
        PartyReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "思想汇报不存在");
        }
        User user = userMapper.selectById(report.getUserId());

        PartyReportAdminDetailVO vo = new PartyReportAdminDetailVO();
        vo.setId(report.getId());
        vo.setUserId(report.getUserId());
        vo.setStudentNo(user == null ? "" : user.getStudentNo());
        vo.setRealName(user == null ? "" : user.getRealName());
        vo.setStageCode(report.getStageCode());
        vo.setTitle(report.getTitle());
        vo.setContent(report.getContent());
        vo.setFileId(report.getFileId());
        vo.setStatus(report.getStatus());
        vo.setReviewComment(report.getReviewComment());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setSubmitTime(report.getSubmitTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReport(Long operatorUserId, Long id, String comment) {
        PartyReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "思想汇报不存在");
        }
        if (report.getStatus() != null && report.getStatus() != 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "该思想汇报已处理");
        }
        report.setStatus(1);
        report.setReviewComment(comment);
        report.setReviewedBy(operatorUserId);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(Long operatorUserId, Long id, String comment) {
        PartyReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "思想汇报不存在");
        }
        if (report.getStatus() != null && report.getStatus() != 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "该思想汇报已处理");
        }
        report.setStatus(2);
        report.setReviewComment(comment);
        report.setReviewedBy(operatorUserId);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    @Override
    public List<PartyActivityAdminListItemVO> listActivities(Integer status) {
        LambdaQueryWrapper<PartyActivityApplication> wrapper = new LambdaQueryWrapper<PartyActivityApplication>()
                .orderByDesc(PartyActivityApplication::getSubmitTime)
                .orderByDesc(PartyActivityApplication::getCreatedAt);
        if (status != null) {
            wrapper.eq(PartyActivityApplication::getStatus, status);
        }
        List<PartyActivityApplication> apps = activityApplicationMapper.selectList(wrapper);
        if (apps == null || apps.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = loadUsersById(apps.stream().map(PartyActivityApplication::getUserId).filter(Objects::nonNull).distinct().toList());

        return apps.stream().map(app -> {
            User u = userMap.get(app.getUserId());
            PartyActivityAdminListItemVO vo = new PartyActivityAdminListItemVO();
            vo.setId(app.getId());
            vo.setStudentNo(u == null ? "" : u.getStudentNo());
            vo.setRealName(u == null ? "" : u.getRealName());
            vo.setTitle(app.getTitle());
            vo.setEventDate(app.getEventDate());
            vo.setStatus(app.getStatus());
            vo.setSubmitTime(app.getSubmitTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PartyActivityAdminDetailVO getActivityDetail(Long id) {
        PartyActivityApplication app = activityApplicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND, "党团活动申请不存在");
        }
        User user = userMapper.selectById(app.getUserId());

        PartyActivityAdminDetailVO vo = new PartyActivityAdminDetailVO();
        vo.setId(app.getId());
        vo.setUserId(app.getUserId());
        vo.setStudentNo(user == null ? "" : user.getStudentNo());
        vo.setRealName(user == null ? "" : user.getRealName());
        vo.setTitle(app.getTitle());
        vo.setReason(app.getReason());
        vo.setEventDate(app.getEventDate());
        vo.setReviewerId(app.getReviewerId());
        vo.setStatus(app.getStatus());
        vo.setReviewComment(app.getReviewComment());
        vo.setReviewedBy(app.getReviewedBy());
        vo.setReviewedAt(app.getReviewedAt());
        vo.setSubmitTime(app.getSubmitTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveActivity(Long operatorUserId, Long id, String comment) {
        PartyActivityApplication app = activityApplicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND, "党团活动申请不存在");
        }
        if (app.getStatus() != null && app.getStatus() != 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "该申请已处理");
        }
        app.setStatus(1);
        app.setReviewComment(comment);
        app.setReviewedBy(operatorUserId);
        app.setReviewedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        activityApplicationMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectActivity(Long operatorUserId, Long id, String comment) {
        PartyActivityApplication app = activityApplicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND, "党团活动申请不存在");
        }
        if (app.getStatus() != null && app.getStatus() != 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "该申请已处理");
        }
        app.setStatus(2);
        app.setReviewComment(comment);
        app.setReviewedBy(operatorUserId);
        app.setReviewedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        activityApplicationMapper.updateById(app);
    }

    private User processProgressRow(Long operatorUserId, PartyProgressImportItemDTO item, Map<String, PartyStageDef> stageMap) {
        if (item == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "行数据为空");
        }

        User user = resolveUser(item.getStudentNo(), item.getRealName());

        String stageCode = safeTrim(item.getStageCode());
        if (!StringUtils.hasText(stageCode)) {
            throw new BizException(ResultCode.PARAM_ERROR, "stageCode不能为空");
        }
        if (!stageMap.containsKey(stageCode)) {
            throw new BizException(ResultCode.PARAM_ERROR, "stageCode不存在: " + stageCode);
        }

        LocalDateTime startTime = parseDateOnly(item.getStartTime());
        LocalDateTime endTime = parseDateOnly(item.getEndTime());
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BizException(ResultCode.PARAM_ERROR, "结束时间不能早于开始时间");
        }

        validateBusinessRules(user.getId(), stageCode, startTime);

        String stepCode = safeTrim(item.getStepCode());
        if (StringUtils.hasText(stepCode)) {
            PartyStepDef step = stepDefMapper.selectOne(new LambdaQueryWrapper<PartyStepDef>()
                    .eq(PartyStepDef::getStepCode, stepCode)
                    .eq(PartyStepDef::getStatus, 1)
                    .last("LIMIT 1"));
            if (step == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "stepCode不存在: " + stepCode);
            }
            if (!stageCode.equals(step.getStageCode())) {
                throw new BizException(ResultCode.PARAM_ERROR, "stepCode不属于stageCode: " + stepCode);
            }
        } else {
            List<PartyStepDef> stepDefs = stepDefMapper.selectEnabledByStageCode(stageCode);
            if (stepDefs != null && !stepDefs.isEmpty()) {
                stepCode = stepDefs.get(0).getStepCode();
            }
        }

        PartyStudentProgress progress = progressMapper.selectByUserId(user.getId());
        if (progress == null) {
            progress = new PartyStudentProgress();
            progress.setUserId(user.getId());
            progress.setCurrentStageCode(stageCode);
            progress.setCurrentStepCode(stepCode);
            progress.setUpdatedAt(LocalDateTime.now());
            progressMapper.insert(progress);
        } else {
            progress.setCurrentStageCode(stageCode);
            progress.setCurrentStepCode(stepCode);
            progress.setUpdatedAt(LocalDateTime.now());
            progressMapper.updateById(progress);
        }

        String remark = safeTrim(item.getRemark());
        if (startTime != null || endTime != null || StringUtils.hasText(remark)) {
            upsertStageHistory(user.getId(), stageCode, startTime, endTime, remark, operatorUserId);
        }

        return user;
    }

    private void upsertStageHistory(Long userId, String stageCode, LocalDateTime startTime, LocalDateTime endTime, String remark, Long operatorUserId) {
        PartyStageHistory latest = stageHistoryMapper.selectLatestByUserIdAndStageCode(userId, stageCode);
        if (latest != null && latest.getEndTime() == null && latest.getStartTime() != null && startTime != null) {
            latest.setStartTime(startTime);
            latest.setEndTime(endTime);
            latest.setRemark(remark);
            latest.setUpdatedBy(operatorUserId);
            latest.setUpdatedAt(LocalDateTime.now());
            stageHistoryMapper.updateById(latest);
            return;
        }
        PartyStageHistory history = new PartyStageHistory();
        history.setUserId(userId);
        history.setStageCode(stageCode);
        history.setStartTime(startTime);
        history.setEndTime(endTime);
        history.setRemark(remark);
        history.setUpdatedBy(operatorUserId);
        history.setUpdatedAt(LocalDateTime.now());
        history.setCreatedAt(LocalDateTime.now());
        stageHistoryMapper.insert(history);
    }

    private void validateBusinessRules(Long userId, String stageCode, LocalDateTime stageStartTime) {
        LocalDate checkDate = stageStartTime == null ? LocalDate.now() : stageStartTime.toLocalDate();

        if ("applicant".equals(stageCode)) {
            if (stageStartTime == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "申请入党需填写申请书落款日期（开始日期）");
            }
            validateAtLeast18(userId, checkDate);
            return;
        }

        if ("activist".equals(stageCode)) {
            if (stageStartTime == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "积极分子需填写确定为积极分子日期（开始日期）");
            }
            validateAtLeast18(userId, checkDate);
            PartyStageHistory applicant = requirePreviousStageStart(userId, "applicant",
                    "缺少申请入党开始日期，无法判断是否完成党课学习小组结业/推优");
            LocalDate applicantDate = applicant.getStartTime().toLocalDate();
            if (checkDate.isBefore(applicantDate)) {
                throw new BizException(ResultCode.PARAM_ERROR, "积极分子日期不能早于申请入党日期");
            }
            return;
        }

        if ("development_target".equals(stageCode)) {
            if (stageStartTime == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "发展对象需填写确定为发展对象日期（开始日期）");
            }
            validateAtLeast18(userId, checkDate);
            PartyStageHistory activist = requirePreviousStageStart(userId, "activist",
                    "缺少积极分子开始日期，无法校验满一年以上（且无法判断院党校结业/推优）");
            LocalDate activistDate = activist.getStartTime().toLocalDate();
            if (checkDate.isBefore(activistDate)) {
                throw new BizException(ResultCode.PARAM_ERROR, "发展对象日期不能早于积极分子日期");
            }
            if (activistDate.plusYears(1).isAfter(checkDate)) {
                throw new BizException(ResultCode.PARAM_ERROR, "成为积极分子需满一年以上方可确定为发展对象，当前不满足");
            }
            return;
        }

        if ("probationary_member".equals(stageCode)) {
            if (stageStartTime == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "预备党员阶段需填写入党时间（支部大会通过日期）");
            }
            validateAtLeast18(userId, checkDate);
            PartyStageHistory devTarget = requirePreviousStageStart(userId, "development_target",
                    "缺少发展对象开始日期，无法判断是否完成校党校结业/支部接收/党委审批");
            LocalDate devTargetDate = devTarget.getStartTime().toLocalDate();
            if (checkDate.isBefore(devTargetDate)) {
                throw new BizException(ResultCode.PARAM_ERROR, "预备党员入党时间不能早于发展对象日期");
            }
            return;
        }

        if ("full_member".equals(stageCode)) {
            if (stageStartTime == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "正式党员需填写转正日期（开始日期）");
            }
            validateAtLeast18(userId, checkDate);
            PartyStageHistory probationary = requirePreviousStageStart(userId, "probationary_member",
                    "缺少预备党员开始日期，无法校验预备期满一年");
            LocalDate probationaryDate = probationary.getStartTime().toLocalDate();
            if (checkDate.isBefore(probationaryDate)) {
                throw new BizException(ResultCode.PARAM_ERROR, "转正日期不能早于入党时间（预备党员开始日期）");
            }
            if (probationaryDate.plusYears(1).isAfter(checkDate)) {
                throw new BizException(ResultCode.PARAM_ERROR, "预备期需满一年后方可转正，当前不满足");
            }
        }
    }

    private PartyStageHistory requirePreviousStageStart(Long userId, String prevStageCode, String message) {
        PartyStageHistory prev = stageHistoryMapper.selectLatestByUserIdAndStageCode(userId, prevStageCode);
        if (prev == null || prev.getStartTime() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, message);
        }
        return prev;
    }

    private void validateAtLeast18(Long userId, LocalDate atDate) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null || !StringUtils.hasText(profile.getIdCard())) {
            return;
        }
        LocalDate birthDate = parseBirthDateFromIdCard(profile.getIdCard());
        if (birthDate == null) {
            return;
        }
        int years = Period.between(birthDate, atDate).getYears();
        if (years < 18) {
            throw new BizException(ResultCode.PARAM_ERROR, "未满18周岁不可申请入党（身份证信息校验）");
        }
    }

    private LocalDate parseBirthDateFromIdCard(String idCard) {
        String value = safeTrim(idCard);
        if (value.length() != 18) {
            return null;
        }
        String birth = value.substring(6, 14);
        if (!birth.matches("\\d{8}")) {
            return null;
        }
        try {
            int year = Integer.parseInt(birth.substring(0, 4));
            int month = Integer.parseInt(birth.substring(4, 6));
            int day = Integer.parseInt(birth.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateOnly(String raw) {
        String value = safeTrim(raw);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.contains(" ") || value.contains("T")) {
            throw new BizException(ResultCode.PARAM_ERROR, "时间只支持到日: " + value + "（格式 yyyy-MM-dd）");
        }
        try {
            LocalDate date = LocalDate.parse(value, DATE);
            return date.atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        throw new BizException(ResultCode.PARAM_ERROR, "时间格式不正确: " + value + "（格式 yyyy-MM-dd）");
    }

    private User resolveUser(String studentNoRaw, String realNameRaw) {
        String studentNo = safeTrim(studentNoRaw);
        String realName = safeTrim(realNameRaw);
        if (!StringUtils.hasText(studentNo) && !StringUtils.hasText(realName)) {
            throw new BizException(ResultCode.PARAM_ERROR, "学号或姓名至少填写一个");
        }
        if (StringUtils.hasText(studentNo) && !studentNo.matches("\\d+")) {
            throw new BizException(ResultCode.PARAM_ERROR, "学号必须为数字");
        }
        if (StringUtils.hasText(studentNo)) {
            User user = userMapper.selectByStudentNo(studentNo);
            if (user == null) {
                throw new BizException(ResultCode.NOT_FOUND, "学号不存在: " + studentNo);
            }
            return user;
        }
        List<User> users = userMapper.selectByRealName(realName);
        if (users == null || users.isEmpty()) {
            throw new BizException(ResultCode.NOT_FOUND, "姓名不存在: " + realName);
        }
        if (users.size() > 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "姓名匹配到多个学生，请使用学号导入: " + realName);
        }
        return users.get(0);
    }

    private Map<Long, User> loadUsersById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
