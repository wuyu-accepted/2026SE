package com.ruc.platform.student.service;

import com.ruc.platform.student.dto.StudentProfileUpdateDTO;
import com.ruc.platform.student.dto.StudentQueryDTO;
import com.ruc.platform.student.dto.StudentImportDTO;
import com.ruc.platform.student.vo.StudentImportBatchVO;
import com.ruc.platform.student.vo.StudentProfileVO;
import com.ruc.platform.student.vo.StudentListItemVO;
import com.ruc.platform.common.api.PageResult;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 管理端导入/创建学生账号
     * @param importDTO 学生账号与档案信息
     * @return 创建后的学生信息
     */
    StudentListItemVO importStudent(StudentImportDTO importDTO);

    /**
     * 管理端通过 CSV 批量导入学生账号
     * @param file CSV 文件
     * @return 导入结果
     */
    StudentImportBatchVO importStudents(MultipartFile file);
}
