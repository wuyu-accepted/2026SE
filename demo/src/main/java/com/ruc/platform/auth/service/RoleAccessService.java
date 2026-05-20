package com.ruc.platform.auth.service;

import com.ruc.platform.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

import static com.ruc.platform.auth.AuthConstants.*;

@Service
@RequiredArgsConstructor
public class RoleAccessService {

    private final UserMapper userMapper;

    public Set<String> getRoleSet(Long userId) {
        return new HashSet<>(userMapper.selectRoleCodesByUserId(userId));
    }

    public boolean hasAnyRole(Long userId, String... roleCodes) {
        Set<String> roles = getRoleSet(userId);
        for (String roleCode : roleCodes) {
            if (roles.contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStudentSideUser(Long userId) {
        return hasAnyRole(userId, ROLE_STUDENT, ROLE_CADRE);
    }

    public boolean isWebSideUser(Long userId) {
        return hasAnyRole(userId, ROLE_COUNSELOR, ROLE_ADMIN);
    }

    public boolean isAdmin(Long userId) {
        return hasAnyRole(userId, ROLE_ADMIN);
    }
}
