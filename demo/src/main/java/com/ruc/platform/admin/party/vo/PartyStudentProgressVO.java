package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyStudentProgressVO {
    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private String currentStageCode;
    private String currentStageName;
    private String currentStepCode;
    private String currentStepName;
    private LocalDateTime updatedAt;
}
