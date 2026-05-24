package com.ruc.platform.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.auth.dto.AccountLoginDTO;
import com.ruc.platform.auth.dto.AccountRegisterDTO;
import com.ruc.platform.auth.dto.WxLoginDTO;
import com.ruc.platform.auth.entity.Role;
import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.entity.UserRole;
import com.ruc.platform.auth.mapper.RoleMapper;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.auth.mapper.UserRoleMapper;
import com.ruc.platform.auth.vo.LoginVO;
import com.ruc.platform.auth.vo.UserVO;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.common.util.GradeUtils;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ruc.platform.auth.AuthConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(AccountRegisterDTO registerDTO) {
        String clientType = normalizeClientType(registerDTO.getClientType());
        if (CLIENT_WEB.equals(clientType)) {
            return registerCounselor(registerDTO);
        }
        return registerStudent(registerDTO);
    }

    private LoginVO registerStudent(AccountRegisterDTO registerDTO) {
        String studentNo = clean(registerDTO.getStudentNo());
        if (userMapper.selectByStudentNo(studentNo) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "该学号已注册");
        }
        if (!clean(registerDTO.getPassword()).equals(clean(registerDTO.getConfirmPassword()))) {
            throw new BizException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        String grade = GradeUtils.normalizeRequiredGrade(registerDTO.getGrade());
        String authType = normalizeAuthType(registerDTO.getAuthType());
        Role studentRole = roleMapper.selectByRoleCode(ROLE_STUDENT);
        if (studentRole == null) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "学生角色不存在，请检查初始化数据");
        }

        User user = new User();
        user.setStudentNo(studentNo);
        user.setRealName(clean(registerDTO.getRealName()));
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setAccountType(authType);
        user.setPhone(clean(registerDTO.getPhone()));
        user.setEmail(clean(registerDTO.getEmail()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        assignRole(user.getId(), studentRole);
        if (ROLE_CADRE.equals(authType)) {
            Role cadreRole = roleMapper.selectByRoleCode(ROLE_CADRE);
            if (cadreRole == null) {
                throw new BizException(ResultCode.SYSTEM_ERROR, "学生骨干角色不存在，请检查初始化数据");
            }
            assignRole(user.getId(), cadreRole);
        }

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setStudentNo(studentNo);
        profile.setGender(registerDTO.getGender());
        profile.setGrade(grade);
        profile.setMajor(clean(registerDTO.getMajor()));
        profile.setClassName(clean(registerDTO.getClassName()));
        profile.setPoliticalStatus(clean(registerDTO.getPoliticalStatus()));
        profile.setBio(clean(registerDTO.getBio()));
        profile.setHometown(clean(registerDTO.getHometown()));
        profile.setDormitory(clean(registerDTO.getDormitory()));
        profile.setAuthType(authType);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.insert(profile);

        log.info("账号注册成功，userId: {}, studentNo: {}", user.getId(), studentNo);
        return buildLoginVO(user);
    }

    private LoginVO registerCounselor(AccountRegisterDTO registerDTO) {
        String jobNo = clean(registerDTO.getStudentNo());
        if (ROLE_ADMIN.equalsIgnoreCase(jobNo)) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员账号不允许通过注册创建");
        }
        if (!isDigits(jobNo)) {
            throw new BizException(ResultCode.PARAM_ERROR, "辅导员工号只能填写数字");
        }
        if (userMapper.selectByStudentNo(jobNo) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "该工号已注册");
        }
        if (!clean(registerDTO.getPassword()).equals(clean(registerDTO.getConfirmPassword()))) {
            throw new BizException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        Role counselorRole = roleMapper.selectByRoleCode(ROLE_COUNSELOR);
        if (counselorRole == null) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "辅导员角色不存在，请检查初始化数据");
        }

        User user = new User();
        user.setStudentNo(jobNo);
        user.setRealName(clean(registerDTO.getRealName()));
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setAccountType(ROLE_COUNSELOR);
        user.setPhone(clean(registerDTO.getPhone()));
        user.setEmail(clean(registerDTO.getEmail()));
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        assignRole(user.getId(), counselorRole);

        log.info("辅导员账号注册成功，等待管理员审核，userId: {}, jobNo: {}", user.getId(), jobNo);
        LoginVO loginVO = new LoginVO();
        loginVO.setNeedBind(true);
        loginVO.setToken(null);
        loginVO.setUser(buildUserVO(user));
        return loginVO;
    }

    @Override
    public LoginVO login(AccountLoginDTO loginDTO) {
        String clientType = normalizeClientType(loginDTO.getClientType());
        String account = clean(loginDTO.getStudentNo());
        if (CLIENT_WEB.equals(clientType) && !ROLE_ADMIN.equalsIgnoreCase(account) && !isDigits(account)) {
            throw new BizException(ResultCode.PARAM_ERROR, "工号只能填写数字，管理员账号固定为 admin");
        }
        User user = userMapper.selectByStudentNo(account);
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "学号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0 && ROLE_COUNSELOR.equals(user.getAccountType())) {
            throw new BizException(ResultCode.FORBIDDEN, "辅导员账号正在等待管理员审核");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        Set<String> roles = new HashSet<>(userMapper.selectRoleCodesByUserId(user.getId()));
        if (CLIENT_MINIPROGRAM.equals(clientType) && !(roles.contains(ROLE_STUDENT) || roles.contains(ROLE_CADRE))) {
            throw new BizException(ResultCode.FORBIDDEN, "该账号仅允许在管理端登录");
        }
        if (CLIENT_WEB.equals(clientType) && !(roles.contains(ROLE_COUNSELOR) || roles.contains(ROLE_ADMIN))) {
            throw new BizException(ResultCode.FORBIDDEN, "学生账号请在小程序端登录");
        }
        return buildLoginVO(user);
    }

    @Override
    public LoginVO wxLogin(WxLoginDTO wxLoginDTO) {
        throw new BizException(ResultCode.FORBIDDEN, "当前版本不使用微信绑定登录，请改用学号密码登录");
    }

    @Override
    public UserVO getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return buildUserVO(user);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private LoginVO buildLoginVO(User user) {
        StpUtil.login(user.getId());
        LoginVO loginVO = new LoginVO();
        loginVO.setNeedBind(false);
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setUser(buildUserVO(user));
        return loginVO;
    }

    private UserVO buildUserVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setRealName(user.getRealName());
        userVO.setStudentNo(user.getStudentNo());
        userVO.setAccountType(user.getAccountType());
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        userVO.setRoles(roles);

        StudentProfile profile = studentProfileMapper.selectByUserId(user.getId());
        if (profile != null) {
            userVO.setAuthType(profile.getAuthType());
            userVO.setClassName(profile.getClassName());
            userVO.setAvatarUrl(profile.getAvatarUrl());
        }
        return userVO;
    }

    private String normalizeAuthType(String authType) {
        String normalized = clean(authType);
        if (normalized.isEmpty()) {
            return ROLE_STUDENT;
        }
        if (ROLE_STUDENT.equalsIgnoreCase(normalized)) {
            return ROLE_STUDENT;
        }
        if (ROLE_CADRE.equalsIgnoreCase(normalized)) {
            return ROLE_CADRE;
        }
        throw new BizException(ResultCode.PARAM_ERROR, "身份类型仅支持 student 或 cadre");
    }

    private String normalizeClientType(String clientType) {
        String normalized = clean(clientType);
        if (normalized.isEmpty()) {
            return CLIENT_MINIPROGRAM;
        }
        if (CLIENT_MINIPROGRAM.equalsIgnoreCase(normalized)) {
            return CLIENT_MINIPROGRAM;
        }
        if (CLIENT_WEB.equalsIgnoreCase(normalized)) {
            return CLIENT_WEB;
        }
        throw new BizException(ResultCode.PARAM_ERROR, "登录端类型仅支持 miniprogram 或 web");
    }

    private void assignRole(Long userId, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isDigits(String value) {
        return value != null && value.matches("\\d+");
    }
}
