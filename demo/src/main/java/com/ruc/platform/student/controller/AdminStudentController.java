package com.ruc.platform.student.controller;

import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.student.dto.StudentQueryDTO;
import com.ruc.platform.student.service.StudentService;
import com.ruc.platform.student.vo.StudentListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;

    @GetMapping
    public Result<PageResult<StudentListItemVO>> listStudents(StudentQueryDTO queryDTO) {
        log.info("管理端查询学生列表，条件: {}", queryDTO);
        return Result.ok(studentService.listStudents(queryDTO));
    }
}
