package com.ruc.platform.student.service;

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
import com.ruc.platform.student.dto.StudentImportDTO;
import com.ruc.platform.student.dto.StudentProfileUpdateDTO;
import com.ruc.platform.student.dto.StudentQueryDTO;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import com.ruc.platform.student.vo.StudentImportBatchVO;
import com.ruc.platform.student.vo.StudentListItemVO;
import com.ruc.platform.student.vo.StudentProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final PasswordEncoder passwordEncoder;

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
        profile.setBio(fallback(updateDTO.getBio(), profile.getBio()));
        profile.setHometown(fallback(updateDTO.getHometown(), profile.getHometown()));
        profile.setDormitory(fallback(updateDTO.getDormitory(), profile.getDormitory()));
        profile.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.updateById(profile);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentListItemVO importStudent(StudentImportDTO importDTO) {
        String studentNo = requireText(importDTO.getStudentNo(), "学号不能为空");
        String realName = requireText(importDTO.getRealName(), "姓名不能为空");
        String grade = GradeUtils.normalizeRequiredGrade(importDTO.getGrade());
        String authType = normalizeAuthType(importDTO.getAuthType());
        if (!isDigits(studentNo)) {
            throw new BizException(ResultCode.PARAM_ERROR, "学号只能填写数字");
        }

        if (userMapper.selectByStudentNo(studentNo) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "该学号已存在");
        }

        Role studentRole = requireRole(ROLE_STUDENT);
        Role cadreRole = ROLE_CADRE.equals(authType) ? requireRole(ROLE_CADRE) : null;

        User user = new User();
        user.setStudentNo(studentNo);
        user.setRealName(realName);
        user.setPasswordHash(passwordEncoder.encode(defaultPassword(importDTO.getPassword(), studentNo)));
        user.setAccountType(authType);
        user.setPhone(clean(importDTO.getPhone()));
        user.setEmail(clean(importDTO.getEmail()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        assignRole(user.getId(), studentRole);
        if (cadreRole != null) {
            assignRole(user.getId(), cadreRole);
        }

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setStudentNo(studentNo);
        profile.setGender(importDTO.getGender());
        profile.setGrade(grade);
        profile.setMajor(clean(importDTO.getMajor()));
        profile.setClassName(clean(importDTO.getClassName()));
        profile.setPoliticalStatus(clean(importDTO.getPoliticalStatus()));
        profile.setHometown(clean(importDTO.getHometown()));
        profile.setDormitory(clean(importDTO.getDormitory()));
        profile.setAuthType(authType);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.insert(profile);

        StudentListItemVO vo = new StudentListItemVO();
        vo.setId(profile.getId());
        vo.setUserId(user.getId());
        vo.setStudentNo(studentNo);
        vo.setRealName(realName);
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setGender(profile.getGender());
        vo.setGrade(profile.getGrade());
        vo.setMajor(profile.getMajor());
        vo.setClassName(profile.getClassName());
        vo.setPoliticalStatus(profile.getPoliticalStatus());
        vo.setAuthType(profile.getAuthType());
        vo.setHometown(profile.getHometown());
        vo.setDormitory(profile.getDormitory());
        vo.setUpdatedAt(profile.getUpdatedAt());
        return vo;
    }

    @Override
    public StudentImportBatchVO importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "请上传 CSV 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".csv")) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅支持 CSV 文件");
        }

        StudentImportBatchVO result = new StudentImportBatchVO();
        List<String> lines = readCsvLines(file);
        if (lines.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "CSV 文件不能为空");
        }

        List<String> headers = parseCsvLine(lines.get(0));
        Map<String, Integer> headerIndex = buildHeaderIndex(headers);
        requireCsvColumn(headerIndex, "studentNo", "学号");
        requireCsvColumn(headerIndex, "realName", "姓名");
        requireCsvColumn(headerIndex, "grade", "学生身份/年级");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            int rowNumber = i + 1;
            result.setTotalCount(result.getTotalCount() + 1);
            try {
                List<String> columns = parseCsvLine(line);
                StudentImportDTO dto = new StudentImportDTO();
                dto.setStudentNo(csvValue(columns, headerIndex, "studentNo"));
                dto.setRealName(csvValue(columns, headerIndex, "realName"));
                dto.setPassword(csvValue(columns, headerIndex, "password"));
                dto.setAuthType(csvValue(columns, headerIndex, "authType"));
                dto.setGender(parseGender(csvValue(columns, headerIndex, "gender")));
                dto.setGrade(csvValue(columns, headerIndex, "grade"));
                dto.setMajor(csvValue(columns, headerIndex, "major"));
                dto.setClassName(csvValue(columns, headerIndex, "className"));
                dto.setPoliticalStatus(csvValue(columns, headerIndex, "politicalStatus"));
                dto.setPhone(csvValue(columns, headerIndex, "phone"));
                dto.setEmail(csvValue(columns, headerIndex, "email"));
                dto.setHometown(csvValue(columns, headerIndex, "hometown"));
                dto.setDormitory(csvValue(columns, headerIndex, "dormitory"));

                StudentListItemVO imported = importStudent(dto);
                result.getRecords().add(imported);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getErrors().add("第 " + rowNumber + " 行：" + e.getMessage());
            }
        }
        return result;
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

    private String fallback(String incoming, String current) {
        return incoming == null ? current : incoming.trim();
    }

    private String requireText(String value, String message) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, message);
        }
        return cleaned;
    }

    private Role requireRole(String roleCode) {
        Role role = roleMapper.selectByRoleCode(roleCode);
        if (role == null) {
            throw new BizException(ResultCode.SYSTEM_ERROR, roleCode + " 角色不存在，请检查初始化数据");
        }
        return role;
    }

    private String defaultPassword(String password, String studentNo) {
        String cleaned = clean(password);
        return cleaned.isEmpty() ? studentNo : cleaned;
    }

    private void assignRole(Long userId, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private List<String> readCsvLines(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "CSV 文件读取失败");
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = stripBom(headers.get(i)).trim();
            String key = normalizeCsvHeader(header);
            if (!key.isEmpty()) {
                index.put(key, i);
            }
        }
        return index;
    }

    private String normalizeCsvHeader(String header) {
        return switch (header) {
            case "studentNo", "学号" -> "studentNo";
            case "realName", "姓名" -> "realName";
            case "password", "初始密码", "密码" -> "password";
            case "authType", "身份类型" -> "authType";
            case "gender", "性别" -> "gender";
            case "grade", "学生身份/年级", "年级" -> "grade";
            case "major", "专业" -> "major";
            case "className", "班级" -> "className";
            case "politicalStatus", "政治面貌" -> "politicalStatus";
            case "phone", "手机号" -> "phone";
            case "email", "邮箱" -> "email";
            case "hometown", "生源地" -> "hometown";
            case "dormitory", "宿舍" -> "dormitory";
            default -> header;
        };
    }

    private void requireCsvColumn(Map<String, Integer> headerIndex, String key, String label) {
        if (!headerIndex.containsKey(key)) {
            throw new BizException(ResultCode.PARAM_ERROR, "CSV 缺少必填列：" + label);
        }
    }

    private String csvValue(List<String> columns, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key);
        if (index == null || index >= columns.size()) {
            return "";
        }
        return clean(stripBom(columns.get(index)));
    }

    private Integer parseGender(String value) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            return null;
        }
        if ("男".equals(cleaned)) {
            return 1;
        }
        if ("女".equals(cleaned)) {
            return 2;
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "性别仅支持 1/2 或 男/女");
        }
    }

    private String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private boolean isDigits(String value) {
        return value != null && value.matches("\\d+");
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
