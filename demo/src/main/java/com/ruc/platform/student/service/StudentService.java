package com.ruc.platform.student.service;

import com.ruc.platform.student.dto.StudentProfileUpdateDTO;
import com.ruc.platform.student.dto.StudentQueryDTO;
import com.ruc.platform.student.vo.StudentProfileVO;
import com.ruc.platform.student.vo.StudentListItemVO;
import com.ruc.platform.common.api.PageResult;

/**
 * 学生服务接口
 */
public interface StudentService {

    /**
     * 根据用户ID获取学生档案
     * @param userId 用户ID
     * @return 学生档案VO
     */
    StudentProfileVO getProfileByUserId(Long userId);

    /**
     * 更新学生档案（学生可编辑的字段）
     * @param userId 用户ID
     * @param updateDTO 更新数据
     * @return 更新后的学生档案
     */
    StudentProfileVO updateProfile(Long userId, StudentProfileUpdateDTO updateDTO);

    /**
     * 管理端分页查询学生列表
     * @param queryDTO 查询条件
     * @return 学生分页列表
     */
    PageResult<StudentListItemVO> listStudents(StudentQueryDTO queryDTO);
}
