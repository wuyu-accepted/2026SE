package com.ruc.platform.student.controller;

import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.student.dto.StudentImportDTO;
import com.ruc.platform.student.dto.StudentQueryDTO;
import com.ruc.platform.student.service.StudentService;
import com.ruc.platform.student.vo.StudentImportBatchVO;
import com.ruc.platform.student.vo.StudentListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @PostMapping
    public Result<StudentListItemVO> importStudent(@RequestBody StudentImportDTO importDTO) {
        log.info("管理端导入学生，studentNo: {}", importDTO.getStudentNo());
        return Result.ok(studentService.importStudent(importDTO));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        String content = "\uFEFF学号,姓名,初始密码,身份类型,学生身份/年级,性别,专业,班级,政治面貌,手机号,邮箱,生源地,宿舍\n" +
                "00001001,测试学生1001,password,student,2023本,男,计算机科学与技术,2023级1班,共青团员,13900001001,stu1001@example.com,北京,东区1号楼101\n" +
                "00001002,测试骨干1002,password,cadre,2022硕,女,软件工程,2022级硕士班,中共党员,13900001002,stu1002@example.com,上海,西区2号楼202\n";
        String filename = URLEncoder.encode("学生批量导入模板.csv", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/batch-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<StudentImportBatchVO> importStudents(@RequestParam("file") MultipartFile file) {
        log.info("管理端批量导入学生，filename: {}", file.getOriginalFilename());
        return Result.ok(studentService.importStudents(file));
    }
}
