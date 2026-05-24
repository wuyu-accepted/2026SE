package com.ruc.platform.admin.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.admin.notice.dto.NoticeCreateDTO;
import com.ruc.platform.admin.notice.dto.NoticeQueryDTO;
import com.ruc.platform.admin.notice.dto.NoticeTargetDTO;
import com.ruc.platform.admin.notice.dto.NoticeUpdateDTO;
import com.ruc.platform.admin.notice.vo.NoticeDetailVO;
import com.ruc.platform.admin.notice.vo.NoticeListItemVO;
import com.ruc.platform.admin.notice.vo.NoticePublishResultVO;
import com.ruc.platform.admin.notice.vo.NoticeStatsVO;
import com.ruc.platform.admin.notice.vo.NoticeTargetEstimateVO;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.knowledgeness.service.KnowledgeLocalSearchService;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.ruc.platform.auth.AuthConstants.ROLE_CADRE;
import static com.ruc.platform.auth.AuthConstants.ROLE_STUDENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeServiceImpl implements AdminNoticeService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    private final NoticeMapper noticeMapper;
    private final UserMessageMapper userMessageMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final ObjectMapper objectMapper;
    private final KnowledgeLocalSearchService localSearchService;

    @Override
    public PageResult<NoticeListItemVO> listNotices(NoticeQueryDTO queryDTO) {
        long pageNum = normalizePageNum(queryDTO.getPageNum());
        long pageSize = normalizePageSize(queryDTO.getPageSize());
        LambdaQueryWrapper<Notice> countWrapper = buildQueryWrapper(queryDTO);
        Long total = noticeMapper.selectCount(countWrapper);
        List<Notice> notices = total == null || total == 0
                ? Collections.emptyList()
                : noticeMapper.selectList(buildQueryWrapper(queryDTO)
                        .orderByDesc(Notice::getUpdatedAt)
                        .orderByDesc(Notice::getId)
                        .last("LIMIT " + pageSize + " OFFSET " + ((pageNum - 1) * pageSize)));
        List<NoticeListItemVO> records = notices.stream()
                .map(this::toListItemVO)
                .collect(Collectors.toList());
        return PageResult.of(total == null ? 0L : total, pageNum, pageSize, records);
    }

    @Override
    public NoticeDetailVO getNoticeDetail(Long id) {
        return toDetailVO(requireNotice(id));
    }

    @Override
    @Transactional
    public NoticeDetailVO createNotice(Long creatorId, NoticeCreateDTO createDTO) {
        Notice notice = new Notice();
        applyCreateFields(notice, createDTO, creatorId);
        noticeMapper.insert(notice);
        return toDetailVO(notice);
    }

    @Override
    @Transactional
    public NoticeDetailVO updateNotice(Long id, NoticeUpdateDTO updateDTO) {
        Notice notice = requireNotice(id);
        applyUpdateFields(notice, updateDTO);
        noticeMapper.updateById(notice);
        if (Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus()) && localSearchService != null) {
            localSearchService.indexNotice(notice);
        }
        return toDetailVO(noticeMapper.selectById(id));
    }

    @Override
    @Transactional
    public NoticePublishResultVO publishNotice(Long id, Long publisherId) {
        Notice notice = requireNotice(id);
        if (Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "通知已发布，不能重复发布");
        }
        Long existingCount = userMessageMapper.countByNoticeId(id);
        if (existingCount != null && existingCount > 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "通知已有投递记录，不能重复发布");
        }

        NoticeTargetDTO target = parseTarget(notice.getTargetTags());
        List<Long> userIds = studentProfileMapper.selectTargetStudentUserIds(
                target.getGrades(),
                target.getMajors(),
                target.getClassName(),
                target.getAuthType()
        );
        if (userIds == null || userIds.isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "当前筛选条件下没有可投递学生");
        }

        LocalDateTime now = LocalDateTime.now();
        notice.setStatus(STATUS_PUBLISHED);
        notice.setPublishTime(now);
        notice.setFeedbackCounselorId(publisherId);
        notice.setUpdatedAt(now);
        noticeMapper.updateById(notice);

        for (Long userId : userIds) {
            UserMessage message = new UserMessage();
            message.setUserId(userId);
            message.setNoticeId(id);
            message.setTitle(notice.getTitle());
            message.setSummary(notice.getSummary());
            message.setReadStatus(0);
            message.setCreatedAt(now);
            userMessageMapper.insert(message);
        }
        if (localSearchService != null) {
            localSearchService.indexNotice(notice);
        }
        log.info("发布通知成功，noticeId: {}, deliveredCount: {}", id, userIds.size());
        return new NoticePublishResultVO(id, (long) userIds.size());
    }

    @Override
    public NoticeTargetEstimateVO estimateTarget(Long id) {
        Notice notice = requireNotice(id);
        NoticeTargetDTO target = parseTarget(notice.getTargetTags());
        List<Long> userIds = studentProfileMapper.selectTargetStudentUserIds(
                target.getGrades(),
                target.getMajors(),
                target.getClassName(),
                target.getAuthType()
        );
        NoticeTargetEstimateVO vo = new NoticeTargetEstimateVO();
        vo.setTarget(target);
        vo.setTargetCount(userIds == null ? 0L : (long) userIds.size());
        return vo;
    }

    @Override
    public NoticeStatsVO getNoticeStats(Long id) {
        requireNotice(id);
        Long deliveredCount = safeCount(userMessageMapper.countByNoticeId(id));
        Long readCount = safeCount(userMessageMapper.countReadByNoticeId(id));
        Long unreadCount = Math.max(0L, deliveredCount - readCount);
        NoticeStatsVO vo = new NoticeStatsVO();
        vo.setNoticeId(id);
        vo.setDeliveredCount(deliveredCount);
        vo.setReadCount(readCount);
        vo.setUnreadCount(unreadCount);
        vo.setReadRate(deliveredCount == 0 ? 0D : Math.round(readCount * 10000D / deliveredCount) / 100D);
        return vo;
    }

    @Override
    @Transactional
    public void offlineNotice(Long id) {
        Notice notice = requireNotice(id);
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "只有已发布通知可以下架");
        }
        notice.setStatus(STATUS_OFFLINE);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        if (localSearchService != null) {
            localSearchService.deleteSource("notice", id);
        }
    }

    @Override
    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = requireNotice(id);
        if (Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "已发布通知请先下架后再删除");
        }
        Long deliveredCount = userMessageMapper.countByNoticeId(id);
        if (deliveredCount != null && deliveredCount > 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "通知已有投递记录，暂不支持删除");
        }
        noticeMapper.deleteById(id);
    }

    private LambdaQueryWrapper<Notice> buildQueryWrapper(NoticeQueryDTO queryDTO) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        String keyword = cleanNullable(queryDTO.getKeyword());
        String noticeType = cleanNullable(queryDTO.getNoticeType());
        if (keyword != null) {
            wrapper.and(w -> w.like(Notice::getTitle, keyword).or().like(Notice::getSummary, keyword));
        }
        if (noticeType != null) {
            wrapper.eq(Notice::getNoticeType, noticeType);
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Notice::getStatus, queryDTO.getStatus());
        }
        return wrapper;
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(ResultCode.NOT_FOUND, "通知不存在");
        }
        return notice;
    }

    private void applyCreateFields(Notice notice, NoticeCreateDTO dto, Long creatorId) {
        LocalDateTime now = LocalDateTime.now();
        notice.setTitle(cleanRequired(dto.getTitle(), "通知标题不能为空"));
        notice.setSummary(cleanNullable(dto.getSummary()));
        notice.setContent(cleanRequired(dto.getContent(), "通知正文不能为空"));
        notice.setNoticeType(cleanNullable(dto.getNoticeType()));
        notice.setTag(cleanNullable(dto.getTag()));
        notice.setPriority(normalizePriority(dto.getPriority()));
        notice.setAttachmentFileId(dto.getAttachmentFileId());
        notice.setFeedbackCounselorId(null);
        notice.setFeedbackCadreIds(serializeLongIds(normalizeLongIds(dto.getFeedbackCadreIds())));
        notice.setIsBanner(Boolean.TRUE.equals(dto.getIsBanner()));
        notice.setStatus(STATUS_DRAFT);
        notice.setCreatedBy(creatorId);
        notice.setTargetTags(serializeTarget(normalizeTarget(dto.getTarget())));
        notice.setCreatedAt(now);
        notice.setUpdatedAt(now);
    }

    private void applyUpdateFields(Notice notice, NoticeUpdateDTO dto) {
        notice.setTitle(cleanRequired(dto.getTitle(), "通知标题不能为空"));
        notice.setSummary(cleanNullable(dto.getSummary()));
        notice.setContent(cleanRequired(dto.getContent(), "通知正文不能为空"));
        notice.setNoticeType(cleanNullable(dto.getNoticeType()));
        notice.setTag(cleanNullable(dto.getTag()));
        notice.setPriority(normalizePriority(dto.getPriority()));
        notice.setAttachmentFileId(dto.getAttachmentFileId());
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus())) {
            notice.setFeedbackCounselorId(null);
        }
        notice.setFeedbackCadreIds(serializeLongIds(normalizeLongIds(dto.getFeedbackCadreIds())));
        notice.setIsBanner(Boolean.TRUE.equals(dto.getIsBanner()));
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus())) {
            notice.setTargetTags(serializeTarget(normalizeTarget(dto.getTarget())));
        }
        notice.setUpdatedAt(LocalDateTime.now());
    }

    private NoticeListItemVO toListItemVO(Notice notice) {
        NoticeListItemVO vo = new NoticeListItemVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setSummary(notice.getSummary());
        vo.setNoticeType(notice.getNoticeType());
        vo.setTag(notice.getTag());
        vo.setStatus(notice.getStatus());
        vo.setPriority(notice.getPriority());
        vo.setAttachmentFileId(notice.getAttachmentFileId());
        vo.setFeedbackCounselorId(notice.getFeedbackCounselorId());
        vo.setFeedbackCadreIds(parseLongIds(notice.getFeedbackCadreIds()));
        vo.setPublishTime(notice.getPublishTime());
        vo.setCreatedAt(notice.getCreatedAt());
        vo.setUpdatedAt(notice.getUpdatedAt());
        vo.setDeliveredCount(safeCount(userMessageMapper.countByNoticeId(notice.getId())));
        vo.setTarget(parseTarget(notice.getTargetTags()));
        return vo;
    }

    private NoticeDetailVO toDetailVO(Notice notice) {
        NoticeDetailVO vo = new NoticeDetailVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setSummary(notice.getSummary());
        vo.setContent(notice.getContent());
        vo.setNoticeType(notice.getNoticeType());
        vo.setTag(notice.getTag());
        vo.setStatus(notice.getStatus());
        vo.setPriority(notice.getPriority());
        vo.setAttachmentFileId(notice.getAttachmentFileId());
        vo.setFeedbackCounselorId(notice.getFeedbackCounselorId());
        vo.setFeedbackCadreIds(parseLongIds(notice.getFeedbackCadreIds()));
        vo.setCreatedBy(notice.getCreatedBy());
        vo.setPublishTime(notice.getPublishTime());
        vo.setCreatedAt(notice.getCreatedAt());
        vo.setUpdatedAt(notice.getUpdatedAt());
        vo.setDeliveredCount(safeCount(userMessageMapper.countByNoticeId(notice.getId())));
        vo.setTarget(parseTarget(notice.getTargetTags()));
        return vo;
    }

    private List<Long> normalizeLongIds(List<Long> ids) {
        List<Long> normalized = new ArrayList<>();
        if (ids == null) {
            return normalized;
        }
        for (Long id : ids) {
            if (id != null && !normalized.contains(id)) {
                normalized.add(id);
            }
        }
        return normalized;
    }

    private String serializeLongIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "通知反馈骨干序列化失败");
        }
    }

    private List<Long> parseLongIds(String rawIds) {
        if (rawIds == null || rawIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return normalizeLongIds(objectMapper.readValue(rawIds, new TypeReference<List<Long>>() {}));
        } catch (JsonProcessingException e) {
            log.warn("通知反馈骨干解析失败，rawIds: {}", rawIds, e);
            return Collections.emptyList();
        }
    }

    private NoticeTargetDTO normalizeTarget(NoticeTargetDTO target) {
        NoticeTargetDTO normalized = new NoticeTargetDTO();
        if (target == null) {
            return normalized;
        }
        List<String> grades = normalizeMultiValues(target.getGrades(), target.getGrade());
        List<String> majors = normalizeMultiValues(target.getMajors(), target.getMajor());
        normalized.setGrades(grades);
        normalized.setGrade(grades.isEmpty() ? null : grades.get(0));
        normalized.setMajors(majors);
        normalized.setMajor(majors.isEmpty() ? null : majors.get(0));
        normalized.setClassName(cleanNullable(target.getClassName()));
        String authType = cleanNullable(target.getAuthType());
        if (authType != null && !ROLE_STUDENT.equals(authType) && !ROLE_CADRE.equals(authType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "身份类型仅支持 student 或 cadre");
        }
        normalized.setAuthType(authType);
        return normalized;
    }

    private List<String> normalizeMultiValues(List<String> values, String legacyValue) {
        List<String> normalized = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                addCleanValue(normalized, value);
            }
        }
        addCleanValue(normalized, legacyValue);
        return normalized;
    }

    private void addCleanValue(List<String> values, String value) {
        String cleaned = cleanNullable(value);
        if (cleaned == null) {
            return;
        }
        String[] parts = cleaned.split("[\\s,，、;；]+");
        for (String part : parts) {
            String item = cleanNullable(part);
            if (item != null && !values.contains(item)) {
                values.add(item);
            }
        }
    }

    private String serializeTarget(NoticeTargetDTO target) {
        try {
            return objectMapper.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "通知发布范围序列化失败");
        }
    }

    private NoticeTargetDTO parseTarget(String targetTags) {
        if (targetTags == null || targetTags.trim().isEmpty()) {
            return new NoticeTargetDTO();
        }
        try {
            return normalizeTarget(objectMapper.readValue(targetTags, NoticeTargetDTO.class));
        } catch (JsonProcessingException e) {
            log.warn("通知发布范围解析失败，targetTags: {}", targetTags, e);
            return new NoticeTargetDTO();
        }
    }

    private int normalizePriority(Integer priority) {
        int value = priority == null ? 0 : priority;
        if (value < 0 || value > 2) {
            throw new BizException(ResultCode.PARAM_ERROR, "优先级仅支持0、1、2");
        }
        return value;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private String cleanRequired(String value, String message) {
        String cleaned = cleanNullable(value);
        if (cleaned == null) {
            throw new BizException(ResultCode.PARAM_ERROR, message);
        }
        return cleaned;
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }
}
