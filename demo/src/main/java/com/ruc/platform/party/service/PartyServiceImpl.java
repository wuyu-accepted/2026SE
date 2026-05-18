package com.ruc.platform.party.service;

import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.party.dto.PartyReportDTO;
import com.ruc.platform.party.dto.PartyActivityCreateDTO;
import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import com.ruc.platform.party.entity.PartyActivityApplication;
import com.ruc.platform.party.entity.PartyReminder;
import com.ruc.platform.party.entity.PartyReport;
import com.ruc.platform.party.entity.PartyStageDef;
import com.ruc.platform.party.entity.PartyStageHistory;
import com.ruc.platform.party.entity.PartyStudentProgress;
import com.ruc.platform.party.entity.PartyStudentRecord;
import com.ruc.platform.party.entity.PartyStepDef;
import com.ruc.platform.party.entity.PartyStepGuidance;
import com.ruc.platform.party.mapper.PartyActivityApplicationMapper;
import com.ruc.platform.party.mapper.PartyReminderMapper;
import com.ruc.platform.party.mapper.PartyReportMapper;
import com.ruc.platform.party.mapper.PartyStageDefMapper;
import com.ruc.platform.party.mapper.PartyStageHistoryMapper;
import com.ruc.platform.party.mapper.PartyStudentProgressMapper;
import com.ruc.platform.party.mapper.PartyStudentRecordMapper;
import com.ruc.platform.party.mapper.PartyStepDefMapper;
import com.ruc.platform.party.mapper.PartyStepGuidanceMapper;
import com.ruc.platform.party.vo.PartyActivityListItemVO;
import com.ruc.platform.party.vo.PartyGuidanceVO;
import com.ruc.platform.party.vo.PartyOverviewVO;
import com.ruc.platform.party.vo.PartyRecordVO;
import com.ruc.platform.party.vo.PartyReminderVO;
import com.ruc.platform.party.vo.PartyStageHistoryVO;
import com.ruc.platform.party.vo.PartyStageVO;
import com.ruc.platform.party.vo.PartyStepVO;
import com.ruc.platform.party.vo.PartyTrackerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService {

    private final PartyStudentProgressMapper progressMapper;
    private final PartyStudentRecordMapper recordMapper;
    private final PartyReminderMapper reminderMapper;
    private final PartyReportMapper reportMapper;
    private final PartyActivityApplicationMapper activityApplicationMapper;
    private final PartyStageDefMapper stageDefMapper;
    private final PartyStepDefMapper stepDefMapper;
    private final PartyStageHistoryMapper stageHistoryMapper;
    private final PartyStepGuidanceMapper stepGuidanceMapper;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Override
    public PartyOverviewVO getOverview(Long userId) {
        PartyStudentProgress progress = progressMapper.selectByUserId(userId);
        if (progress == null) {
            throw new BizException(ResultCode.NOT_FOUND, "党团进度不存在");
        }

        List<PartyReminder> pendingReminders = reminderMapper.selectPendingByUserId(userId);
        PartyOverviewVO vo = new PartyOverviewVO();
        vo.setCurrentStageCode(progress.getCurrentStageCode());
        vo.setCurrentStageName(getStageNameByCode(progress.getCurrentStageCode()));
        vo.setPendingReminders(pendingReminders.size());
        return vo;
    }

    @Override
    public List<PartyRecordVO> getRecords(Long userId) {
        return recordMapper.selectByUserId(userId).stream()
                .map(this::convertToRecordVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartyReminderVO> getReminders(Long userId) {
        return reminderMapper.selectPendingByUserId(userId).stream()
                .map(this::convertToReminderVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(Long userId, PartyReportDTO reportDTO) {
        if (reportDTO.getFileId() != null) {
            FileMetadata metadata = fileService.getFileMetadata(reportDTO.getFileId());
            if (!isAllowedReportFile(metadata)) {
                throw new BizException(ResultCode.PARAM_ERROR, "思想汇报仅支持 PDF/Word 附件");
            }
        }
        PartyStudentProgress progress = progressMapper.selectByUserId(userId);
        PartyReport report = new PartyReport();
        report.setUserId(userId);
        report.setStageCode(progress == null ? null : progress.getCurrentStageCode());
        report.setTitle(reportDTO.getTitle());
        report.setContent(reportDTO.getContent());
        report.setFileId(reportDTO.getFileId());
        report.setSubmitTime(LocalDateTime.now());
        report.setStatus(0);
        reportMapper.insert(report);
        log.info("提交思想汇报成功，userId: {}, reportId: {}", userId, report.getId());
    }

    @Override
    public List<PartyReport> listMyReports(Long userId) {
        return reportMapper.selectList(new LambdaQueryWrapper<PartyReport>()
                .eq(PartyReport::getUserId, userId)
                .orderByDesc(PartyReport::getSubmitTime)
                .orderByDesc(PartyReport::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivityApplication(Long userId, PartyActivityCreateDTO dto) {
        PartyActivityApplication app = new PartyActivityApplication();
        app.setUserId(userId);
        app.setTitle(dto.getTitle());
        app.setReason(dto.getReason());
        app.setEventDate(dto.getEventDate());
        app.setReviewerId(dto.getReviewerId());
        app.setStatus(0);
        app.setSubmitTime(LocalDateTime.now());
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        activityApplicationMapper.insert(app);
        return app.getId();
    }

    @Override
    public List<PartyActivityListItemVO> listMyActivities(Long userId) {
        List<PartyActivityApplication> apps = activityApplicationMapper.selectList(new LambdaQueryWrapper<PartyActivityApplication>()
                .eq(PartyActivityApplication::getUserId, userId)
                .orderByDesc(PartyActivityApplication::getSubmitTime)
                .orderByDesc(PartyActivityApplication::getCreatedAt));
        return apps.stream().map(app -> {
            PartyActivityListItemVO vo = new PartyActivityListItemVO();
            vo.setId(app.getId());
            vo.setTitle(app.getTitle());
            vo.setReason(app.getReason());
            vo.setEventDate(app.getEventDate());
            vo.setStatus(app.getStatus());
            vo.setSubmitTime(app.getSubmitTime());
            vo.setReviewComment(app.getReviewComment());
            vo.setReviewedAt(app.getReviewedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PartyTrackerVO getTracker(Long userId) {
        PartyStudentProgress progress = progressMapper.selectByUserId(userId);
        if (progress == null) {
            throw new BizException(ResultCode.NOT_FOUND, "党团进度不存在");
        }

        List<PartyStageDef> stageDefs = stageDefMapper.selectList(
                new LambdaQueryWrapper<PartyStageDef>()
                        .eq(PartyStageDef::getStatus, 1)
                        .orderByAsc(PartyStageDef::getSortOrder)
        );

        Map<String, PartyStageDef> stageDefMap = stageDefs.stream()
                .collect(Collectors.toMap(PartyStageDef::getStageCode, item -> item, (a, b) -> a));

        List<PartyStageVO> stages = stageDefs.stream().map(stageDef -> {
            PartyStageVO stageVO = new PartyStageVO();
            stageVO.setStageCode(stageDef.getStageCode());
            stageVO.setStageName(stageDef.getStageName());
            stageVO.setSortOrder(stageDef.getSortOrder());

            List<PartyStepDef> stepDefs = stepDefMapper.selectEnabledByStageCode(stageDef.getStageCode());
            List<PartyStepVO> steps = stepDefs.stream().map(stepDef -> {
                PartyStepVO stepVO = new PartyStepVO();
                stepVO.setStepCode(stepDef.getStepCode());
                stepVO.setStepName(stepDef.getStepName());
                stepVO.setSortOrder(stepDef.getSortOrder());
                return stepVO;
            }).collect(Collectors.toList());
            stageVO.setSteps(steps);
            return stageVO;
        }).collect(Collectors.toList());

        String currentStageCode = progress.getCurrentStageCode();
        PartyStageDef currentStageDef = stageDefMap.get(currentStageCode);
        String currentStageName = currentStageDef == null ? getStageNameByCode(currentStageCode) : currentStageDef.getStageName();

        String currentStepCode = progress.getCurrentStepCode();
        if (!StringUtils.hasText(currentStepCode)) {
            List<PartyStepDef> stepDefs = stepDefMapper.selectEnabledByStageCode(currentStageCode);
            if (!stepDefs.isEmpty()) {
                currentStepCode = stepDefs.get(0).getStepCode();
            }
        }

        String currentStepName = resolveStepName(stages, currentStageCode, currentStepCode);

        PartyStageHistory history = stageHistoryMapper.selectLatestByUserIdAndStageCode(userId, currentStageCode);
        PartyStageHistoryVO historyVO = null;
        if (history != null) {
            historyVO = new PartyStageHistoryVO();
            historyVO.setStageCode(history.getStageCode());
            historyVO.setStageName(currentStageName);
            historyVO.setStartTime(history.getStartTime());
            historyVO.setEndTime(history.getEndTime());
            historyVO.setRemark(history.getRemark());
        }

        List<PartyGuidanceVO> guidances = Collections.emptyList();
        if (StringUtils.hasText(currentStepCode)) {
            List<PartyStepGuidance> guidanceList = stepGuidanceMapper.selectEnabledByStepCode(currentStepCode);
            guidances = guidanceList.stream().map(this::convertToGuidanceVO).collect(Collectors.toList());
        }

        PartyTrackerVO trackerVO = new PartyTrackerVO();
        trackerVO.setCurrentStageCode(currentStageCode);
        trackerVO.setCurrentStageName(currentStageName);
        trackerVO.setCurrentStepCode(currentStepCode);
        trackerVO.setCurrentStepName(currentStepName);
        trackerVO.setStages(stages);
        trackerVO.setCurrentStageHistory(historyVO);
        trackerVO.setGuidances(guidances);
        return trackerVO;
    }

    private PartyGuidanceVO convertToGuidanceVO(PartyStepGuidance guidance) {
        PartyGuidanceVO vo = new PartyGuidanceVO();
        vo.setTitle(guidance.getTitle());
        vo.setContent(guidance.getContent());
        vo.setPriority(guidance.getPriority());
        vo.setMaterials(parseMaterials(guidance.getMaterials()));
        return vo;
    }

    private List<String> parseMaterials(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<>() {});
            } catch (Exception ignored) {
                return Collections.singletonList(trimmed);
            }
        }
        return List.of(trimmed.split("\\r?\\n")).stream().map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList());
    }

    private String resolveStepName(List<PartyStageVO> stages, String stageCode, String stepCode) {
        if (!StringUtils.hasText(stageCode) || !StringUtils.hasText(stepCode)) {
            return "";
        }
        for (PartyStageVO stage : stages) {
            if (!stageCode.equals(stage.getStageCode()) || stage.getSteps() == null) {
                continue;
            }
            for (PartyStepVO step : stage.getSteps()) {
                if (stepCode.equals(step.getStepCode())) {
                    return step.getStepName();
                }
            }
        }
        return "";
    }

    private String getStageNameByCode(String stageCode) {
        return switch (stageCode) {
            case "applicant" -> "入党申请人";
            case "activist" -> "积极分子";
            case "development_target" -> "发展对象";
            case "probationary_member" -> "预备党员";
            case "full_member" -> "正式党员";
            default -> "未知阶段";
        };
    }

    private PartyRecordVO convertToRecordVO(PartyStudentRecord record) {
        PartyRecordVO vo = new PartyRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private PartyReminderVO convertToReminderVO(PartyReminder reminder) {
        PartyReminderVO vo = new PartyReminderVO();
        BeanUtils.copyProperties(reminder, vo);
        return vo;
    }

    private boolean isAllowedReportFile(FileMetadata metadata) {
        if (metadata == null) {
            return false;
        }
        String name = metadata.getOriginName() == null ? "" : metadata.getOriginName().toLowerCase();
        if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")) {
            return true;
        }
        String mime = metadata.getMimeType() == null ? "" : metadata.getMimeType().toLowerCase();
        return mime.equals("application/pdf")
                || mime.equals("application/msword")
                || mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
