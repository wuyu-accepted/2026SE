package com.ruc.platform.admin.counselor.service;

import com.ruc.platform.admin.counselor.dto.CounselorCreateDTO;
import com.ruc.platform.admin.counselor.dto.CounselorUpdateDTO;
import com.ruc.platform.admin.counselor.mapper.CounselorMapper;
import com.ruc.platform.admin.counselor.vo.CounselorVO;
import com.ruc.platform.auth.entity.Role;
import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.entity.UserRole;
import com.ruc.platform.auth.mapper.RoleMapper;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.auth.mapper.UserRoleMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.ruc.platform.auth.AuthConstants.ROLE_COUNSELOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCounselorServiceImpl implements AdminCounselorService {

    private final CounselorMapper counselorMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<CounselorVO> listAll() {
        return counselorMapper.selectCounselors();
    }

    @Override
    @Transactional
    public void create(CounselorCreateDTO dto) {
        if (userMapper.selectByStudentNo(dto.getStudentNo()) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "该工号已存在");
        }
        Role counselorRole = roleMapper.selectByRoleCode(ROLE_COUNSELOR);
        if (counselorRole == null) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "辅导员角色不存在");
        }

        User user = new User();
        user.setStudentNo(dto.getStudentNo());
        user.setRealName(dto.getRealName());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setAccountType(ROLE_COUNSELOR);
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole ur = new UserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(counselorRole.getId());
        userRoleMapper.insert(ur);

        log.info("新增辅导员账号，id: {}, studentNo: {}", user.getId(), dto.getStudentNo());
    }

    @Override
    @Transactional
    public void update(Long id, CounselorUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("更新辅导员账号，id: {}", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
        log.info("删除辅导员账号，id: {}", id);
    }
}
