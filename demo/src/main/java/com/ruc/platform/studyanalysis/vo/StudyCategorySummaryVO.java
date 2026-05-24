package com.ruc.platform.studyanalysis.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudyCategorySummaryVO {

    private String category;

    private BigDecimal requiredCredits;

    private BigDecimal earnedCredits;

    private BigDecimal remainingCredits;
}
