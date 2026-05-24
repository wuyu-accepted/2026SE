package com.ruc.platform.student.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.student.dto.StudentHonorUpsertDTO;
import com.ruc.platform.student.dto.StudentProfileUpdateDTO;
import com.ruc.platform.student.service.StudentService;
import com.ruc.platform.student.vo.StudentHonorTermGroupVO;
import com.ruc.platform.student.vo.StudentHonorVO;
import com.ruc.platform.student.vo.StudentProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生档案控制器
 * 处理学生档案查询和更新请求
 */
@Slf4j
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * 获取本人学生档案
     * 需要登录态，从token中获取用户ID
     * 
     * @return 学生档案信息
     */
    @GetMapping("/profile")
    public Result<StudentProfileVO> getProfile() {
        long userId = StpUtil.getLoginIdAsLong();
        log.info("获取学生档案，userId: {}", userId);
        
        // 查询学生档案
        StudentProfileVO profile = studentService.getProfileByUserId(userId);
        return Result.ok(profile);
    }

    /**
     * 更新本人学生档案
     * 只允许更新学生可编辑的字段
     * 
     * @param updateDTO 更新数据
     * @return 更新后的学生档案
     */
    @PutMapping("/profile")
    public Result<StudentProfileVO> updateProfile(@Valid @RequestBody StudentProfileUpdateDTO updateDTO) {
        long userId = StpUtil.getLoginIdAsLong();
        log.info("更新学生档案，userId: {}", userId);
        
        StudentProfileVO profile = studentService.updateProfile(userId, updateDTO);
        return Result.ok(profile);
    }

    @PostMapping("/profile/avatar")
    public Result<StudentProfileVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        StudentProfileVO profile = studentService.uploadAvatar(userId, file);
        return Result.ok(profile);
    }

    @GetMapping("/profile/honors")
    public Result<List<StudentHonorTermGroupVO>> listMyHonors() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studentService.listMyHonorGroups(userId));
    }

    @PostMapping("/profile/honors")
    public Result<StudentHonorVO> createMyHonor(@Valid @RequestBody StudentHonorUpsertDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studentService.createMyHonor(userId, dto));
    }

    @PutMapping("/profile/honors/{id}")
    public Result<StudentHonorVO> updateMyHonor(@PathVariable Long id, @Valid @RequestBody StudentHonorUpsertDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studentService.updateMyHonor(userId, id, dto));
    }

    @DeleteMapping("/profile/honors/{id}")
    public Result<Void> deleteMyHonor(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        studentService.deleteMyHonor(userId, id);
        return Result.ok();
    }
}
