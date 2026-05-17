package com.ruc.platform.admin.party.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.admin.party.dto.PartyProgressBatchImportDTO;
import com.ruc.platform.admin.party.dto.PartyProgressImportItemDTO;
import com.ruc.platform.admin.party.vo.PartyProgressBatchImportResultVO;
import com.ruc.platform.admin.party.vo.PartyProgressImportRowVO;
import com.ruc.platform.admin.party.vo.PartyStageOptionVO;
import com.ruc.platform.admin.party.vo.PartyStepOptionVO;
import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.party.entity.PartyStageDef;
import com.ruc.platform.party.entity.PartyStageHistory;
import com.ruc.platform.party.entity.PartyStudentProgress;
import com.ruc.platform.party.entity.PartyStepDef;
import com.ruc.platform.party.mapper.PartyStageDefMapper;
import com.ruc.platform.party.mapper.PartyStageHistoryMapper;
import com.ruc.platform.party.mapper.PartyStudentProgressMapper;
import com.ruc.platform.party.mapper.PartyStepDefMapper;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import cn.dev33.satoken.stp.StpUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyAdminServiceImpl implements PartyAdminService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserMapper userMapper;
    private final PartyStudentProgressMapper progressMapper;
    private final PartyStageDefMapper stageDefMapper;
    private final PartyStepDefMapper stepDefMapper;
    private final PartyStageHistoryMapper stageHistoryMapper;
    private final StudentProfileMapper studentProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PartyProgressBatchImportResultVO batchImportProgress(PartyProgressBatchImportDTO dto) {
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

        Long operatorUserId = null;
        try {
            operatorUserId = StpUtil.getLoginIdAsLong();
        } catch (Exception ignored) {
        }

        for (int i = 0; i < items.size(); i++) {
            PartyProgressImportItemDTO item = items.get(i);
            PartyProgressImportRowVO row = new PartyProgressImportRowVO();
            row.setRowNo(i + 1);
            row.setStudentNo(item == null ? null : safeTrim(item.getStudentNo()));
            row.setRealName(item == null ? null : safeTrim(item.getRealName()));
            row.setStageCode(item == null ? null : safeTrim(item.getStageCode()));
            row.setStepCode(item == null ? null : safeTrim(item.getStepCode()));
            try {
                User user = processRow(item, stageMap, operatorUserId);
                if (user != null) {
                    row.setStudentNo(user.getStudentNo());
                    row.setRealName(user.getRealName());
                }
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

    private User processRow(PartyProgressImportItemDTO item, Map<String, PartyStageDef> stageMap, Long operatorUserId) {
        if (item == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "行数据为空");
        }

        String studentNo = safeTrim(item.getStudentNo());
        String realName = safeTrim(item.getRealName());
        if (!StringUtils.hasText(studentNo) && !StringUtils.hasText(realName)) {
            throw new BizException(ResultCode.PARAM_ERROR, "学号或姓名至少填写一个");
        }
        if (StringUtils.hasText(studentNo) && !studentNo.matches("\\d+")) {
            throw new BizException(ResultCode.PARAM_ERROR, "学号必须为数字");
        }

        String stageCode = safeTrim(item.getStageCode());
        if (!StringUtils.hasText(stageCode)) {
            throw new BizException(ResultCode.PARAM_ERROR, "stageCode不能为空");
        }
        if (!stageMap.containsKey(stageCode)) {
            throw new BizException(ResultCode.PARAM_ERROR, "stageCode不存在: " + stageCode);
        }

        User user = null;
        if (StringUtils.hasText(studentNo)) {
            user = userMapper.selectByStudentNo(studentNo);
            if (user == null) {
                throw new BizException(ResultCode.NOT_FOUND, "学号不存在: " + studentNo);
            }
        } else {
            List<User> users = userMapper.selectByRealName(realName);
            if (users == null || users.isEmpty()) {
                throw new BizException(ResultCode.NOT_FOUND, "姓名不存在: " + realName);
            }
            if (users.size() > 1) {
                throw new BizException(ResultCode.PARAM_ERROR, "姓名匹配到多个学生，请使用学号导入: " + realName);
            }
            user = users.get(0);
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
            if (!stepDefs.isEmpty()) {
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
