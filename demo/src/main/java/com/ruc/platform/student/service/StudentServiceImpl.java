package com.ruc.platform.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.auth.entity.Role;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.entity.UserRole;
import com.ruc.platform.auth.mapper.RoleMapper;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.auth.mapper.UserRoleMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.common.util.GradeUtils;
import com.ruc.platform.student.dto.StudentProfileUpdateDTO;
import com.ruc.platform.student.dto.StudentQueryDTO;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import com.ruc.platform.student.vo.StudentListItemVO;
import com.ruc.platform.student.vo.StudentProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.ruc.platform.auth.AuthConstants.ROLE_CADRE;
import static com.ruc.platform.auth.AuthConstants.ROLE_COUNSELOR;
import static com.ruc.platform.auth.AuthConstants.ROLE_ADMIN;
import static com.ruc.platform.auth.AuthConstants.ROLE_STUDENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public StudentProfileVO getProfileByUserId(Long userId) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = createProfileIfMissing(userId);
        }
        return convertToVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileVO updateProfile(Long userId, StudentProfileUpdateDTO updateDTO) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = createProfileIfMissing(userId);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setRealName(fallback(updateDTO.getRealName(), user.getRealName()));
        user.setPhone(fallback(updateDTO.getPhone(), user.getPhone()));
        user.setEmail(fallback(updateDTO.getEmail(), user.getEmail()));
        String nextAuthType = normalizeAuthType(fallback(updateDTO.getAuthType(), profile.getAuthType()));
        user.setAccountType(nextAuthType);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        if (updateDTO.getGender() != null) {
            profile.setGender(updateDTO.getGender());
        }
        if (updateDTO.getGrade() != null) {
            profile.setGrade(GradeUtils.normalizeRequiredGrade(updateDTO.getGrade()));
        }
        profile.setMajor(fallback(updateDTO.getMajor(), profile.getMajor()));
        profile.setClassName(fallback(updateDTO.getClassName(), profile.getClassName()));
        profile.setPoliticalStatus(fallback(updateDTO.getPoliticalStatus(), profile.getPoliticalStatus()));
        profile.setAuthType(nextAuthType);
        profile.setBio(fallback(updateDTO.getBio(), profile.getBio()));
        profile.setHometown(fallback(updateDTO.getHometown(), profile.getHometown()));
        profile.setDormitory(fallback(updateDTO.getDormitory(), profile.getDormitory()));
        profile.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.updateById(profile);
        syncCadreRole(userId, nextAuthType);

        log.info("更新学生档案成功，userId: {}", userId);
        return convertToVO(profile);
    }

    @Override
    public PageResult<StudentListItemVO> listStudents(StudentQueryDTO queryDTO) {
        long pageNum = normalizePageNum(queryDTO.getPageNum());
        long pageSize = normalizePageSize(queryDTO.getPageSize());
        long offset = (pageNum - 1) * pageSize;

        String keyword = cleanNullable(queryDTO.getKeyword());
        String grade = cleanNullable(queryDTO.getGrade());
        String major = cleanNullable(queryDTO.getMajor());
        String className = cleanNullable(queryDTO.getClassName());
        String authType = cleanNullable(queryDTO.getAuthType());

        if (authType != null) {
            authType = normalizeAuthType(authType);
        }

        Long total = studentProfileMapper.countStudents(keyword, grade, major, className, authType);
        List<StudentListItemVO> records = studentProfileMapper.selectStudentPage(
                keyword,
                grade,
                major,
                className,
                authType,
                pageSize,
                offset
        );
        return PageResult.of(total == null ? 0L : total, pageNum, pageSize, records);
    }

    private StudentProfileVO convertToVO(StudentProfile profile) {
        StudentProfileVO vo = new StudentProfileVO();
        BeanUtils.copyProperties(profile, vo);
        User user = userMapper.selectById(profile.getUserId());
        if (user != null) {
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
        }
        return vo;
    }

    private StudentProfile createProfileIfMissing(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (ROLE_COUNSELOR.equalsIgnoreCase(user.getAccountType()) || ROLE_ADMIN.equalsIgnoreCase(user.getAccountType())) {
            throw new BizException(ResultCode.NOT_FOUND, "当前账号不是学生账号，暂无学生档案");
        }

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setStudentNo(user.getStudentNo());
        profile.setAuthType(normalizeAuthType(user.getAccountType()));
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.insert(profile);

        log.warn("检测到学生档案缺失，已自动补建最小档案，userId: {}, studentNo: {}", userId, user.getStudentNo());
        return profile;
    }

    private String normalizeAuthType(String authType) {
        String normalized = authType == null ? "" : authType.trim();
        if (normalized.isEmpty() || ROLE_STUDENT.equalsIgnoreCase(normalized)) {
            return ROLE_STUDENT;
        }
        if (ROLE_CADRE.equalsIgnoreCase(normalized)) {
            return ROLE_CADRE;
        }
        throw new BizException(ResultCode.PARAM_ERROR, "身份类型仅支持 student 或 cadre");
    }

    private void syncCadreRole(Long userId, String authType) {
        Role cadreRole = roleMapper.selectByRoleCode(ROLE_CADRE);
        if (cadreRole == null) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "学生骨干角色不存在，请检查初始化数据");
        }
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, cadreRole.getId());
        UserRole existing = userRoleMapper.selectOne(wrapper);
        if (ROLE_CADRE.equals(authType) && existing == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(cadreRole.getId());
            userRoleMapper.insert(userRole);
        } else if (ROLE_STUDENT.equals(authType) && existing != null) {
            userRoleMapper.delete(wrapper);
        }
    }

    private String fallback(String incoming, String current) {
        return incoming == null ? current : incoming.trim();
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

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
