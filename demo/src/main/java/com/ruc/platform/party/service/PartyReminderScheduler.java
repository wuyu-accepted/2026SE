package com.ruc.platform.party.service;

import com.ruc.platform.party.entity.PartyReminder;
import com.ruc.platform.party.entity.PartyStageHistory;
import com.ruc.platform.party.entity.PartyStudentProgress;
import com.ruc.platform.party.mapper.PartyReminderMapper;
import com.ruc.platform.party.mapper.PartyStageHistoryMapper;
import com.ruc.platform.party.mapper.PartyStudentProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyReminderScheduler {

    private static final int REMINDER_AHEAD_DAYS = 7;

    private final PartyStudentProgressMapper progressMapper;
    private final PartyStageHistoryMapper stageHistoryMapper;
    private final PartyReminderMapper reminderMapper;

    @Scheduled(cron = "${party.reminder.scheduler-cron:0 0 6 * * *}")
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerateReminders() {
        log.info("开始自动生成党团提醒");
        List<PartyStudentProgress> allProgress = progressMapper.selectList(null);
        int generated = 0;
        for (PartyStudentProgress progress : allProgress) {
            generated += generateForUser(progress);
        }
        log.info("党团提醒生成完毕，共生成 {} 条", generated);
        return generated;
    }

    private int generateForUser(PartyStudentProgress progress) {
        Long userId = progress.getUserId();
        String stageCode = progress.getCurrentStageCode();
        PartyStageHistory history = stageHistoryMapper.selectLatestByUserIdAndStageCode(userId, stageCode);
        if (history == null || history.getStartTime() == null) {
            return 0;
        }
        LocalDateTime stageStart = history.getStartTime();
        int count = 0;
        switch (stageCode) {
            case "activist":
                if (tryCreatePeriodicReportReminder(userId, stageStart, 3, "积极分子")) count++;
                if (tryCreatePeriodicReportReminder(userId, stageStart, 6, "积极分子")) count++;
                break;
            case "development_target":
                if (tryCreatePeriodicReportReminder(userId, stageStart, 3, "发展对象")) count++;
                if (tryCreatePeriodicReportReminder(userId, stageStart, 6, "发展对象")) count++;
                break;
            case "probationary_member":
                if (tryCreatePeriodicReportReminder(userId, stageStart, 3, "预备党员")) count++;
                if (tryCreatePeriodicReportReminder(userId, stageStart, 6, "预备党员")) count++;
                if (tryCreateProbationaryExpiryReminder(userId, stageStart)) count++;
                break;
            default:
                break;
        }
        return count;
    }

    private boolean tryCreatePeriodicReportReminder(Long userId, LocalDateTime stageStart, int months, String stageLabel) {
        LocalDateTime deadline = stageStart.plusMonths(months);
        if (LocalDateTime.now().isBefore(deadline.minusDays(REMINDER_AHEAD_DAYS))) {
            return false;
        }
        String title = stageLabel + "思想汇报提交提醒";
        String content = String.format(
                "你进入%s阶段已满%d个月，请按时提交思想汇报。截止日期：%s",
                stageLabel, months, deadline.toLocalDate()
        );
        if (hasExistingReminder(userId, title, deadline)) {
            return false;
        }
        insertReminder(userId, title, content, deadline, "report");
        log.info("生成提醒: userId={}, title={}, deadline={}", userId, title, deadline);
        return true;
    }

    private boolean tryCreateProbationaryExpiryReminder(Long userId, LocalDateTime stageStart) {
        LocalDateTime deadline = stageStart.plusYears(1);
        if (LocalDateTime.now().isBefore(deadline.minusDays(REMINDER_AHEAD_DAYS))) {
            return false;
        }
        String title = "预备党员转正提醒";
        String content = String.format(
                "你的预备期将于 %s 届满，请提前准备转正申请材料。",
                deadline.toLocalDate()
        );
        if (hasExistingReminder(userId, title, deadline)) {
            return false;
        }
        insertReminder(userId, title, content, deadline, "meeting");
        log.info("生成提醒: userId={}, title={}, deadline={}", userId, title, deadline);
        return true;
    }

    private boolean hasExistingReminder(Long userId, String title, LocalDateTime deadline) {
        Long count = reminderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PartyReminder>()
                        .eq(PartyReminder::getUserId, userId)
                        .eq(PartyReminder::getTitle, title)
                        .eq(PartyReminder::getDeadline, deadline)
        );
        return count != null && count > 0;
    }

    private void insertReminder(Long userId, String title, String content, LocalDateTime deadline, String reminderType) {
        PartyReminder reminder = new PartyReminder();
        reminder.setUserId(userId);
        reminder.setTitle(title);
        reminder.setContent(content);
        reminder.setDeadline(deadline);
        reminder.setStatus(0);
        reminder.setReminderType(reminderType);
        reminder.setCreatedAt(LocalDateTime.now());
        reminderMapper.insert(reminder);
    }
}
