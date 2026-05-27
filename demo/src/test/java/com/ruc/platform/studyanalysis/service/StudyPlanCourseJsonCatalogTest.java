package com.ruc.platform.studyanalysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyPlanCourseJsonCatalogTest {

    private final StudyPlanCourseJsonCatalog catalog = new StudyPlanCourseJsonCatalog(new ObjectMapper(), "course.json");

    @Test
    void loadsGeneratedCourseJsonAndResolvesCoreFields() {
        assertTrue(catalog.isSupportedMajor("计算机科学与技术"));
        assertTrue(catalog.isSupportedMajor("软件工程"));
        assertTrue(catalog.isSupportedMajor("信息安全"));
        assertTrue(catalog.isSupportedMajor("数据科学与大数据技术"));
        assertTrue(catalog.isSupportedMajor("信息管理与信息系统"));

        assertEquals(new BigDecimal("4"), catalog.findCredits("计算机科学与技术", "程序设计").orElseThrow());
        assertEquals("专业模块", catalog.findModule("计算机科学与技术", "程序设计").orElseThrow());
        assertEquals("通识模块", catalog.findModule("计算机科学与技术", "思想道德与法治").orElseThrow());
        assertEquals("创新训练与科学研究", catalog.findModule("计算机科学与技术", "研究训练").orElseThrow());
        assertEquals("素质拓展与发展指导", catalog.findModule("计算机科学与技术", "综合设计").orElseThrow());
    }

    @Test
    void calculatesRequiredCreditsByModuleFromNonElectiveCourses() {
        Map<String, BigDecimal> credits = catalog.getRequiredCreditsByModule("计算机科学与技术");

        assertEquals(new BigDecimal("70"), credits.get("专业模块"));
        assertEquals(new BigDecimal("19"), credits.get("通识模块"));
        assertEquals(new BigDecimal("10"), credits.get("创新训练与科学研究"));
        assertEquals(new BigDecimal("2"), credits.get("素质拓展与发展指导"));
    }
}
