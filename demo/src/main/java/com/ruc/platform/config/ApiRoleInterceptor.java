package com.ruc.platform.config;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.auth.service.RoleAccessService;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.ruc.platform.auth.AuthConstants.*;

@Component
@RequiredArgsConstructor
public class ApiRoleInterceptor implements HandlerInterceptor {

    private final RoleAccessService roleAccessService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/") || path.equals("/api/health")) {
            return true;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        if (path.startsWith("/api/admin/roles")
                || path.startsWith("/api/admin/counselors")
                || path.startsWith("/api/admin/audit-logs")
                || path.startsWith("/api/admin/ai-config")
                || path.startsWith("/api/wechat/official-account/menu")) {
            require(userId, ROLE_ADMIN);
            return true;
        }

        if (path.startsWith("/api/notice-feedback/counselor")) {
            require(userId, ROLE_COUNSELOR, ROLE_ADMIN);
            return true;
        }

        if (path.startsWith("/api/notice-feedback/cadre")
                || path.contains("/cadre-reply")
                || path.contains("/escalate")) {
            require(userId, ROLE_CADRE);
            return true;
        }

        if (path.contains("/counselor-reply")) {
            require(userId, ROLE_COUNSELOR, ROLE_ADMIN);
            return true;
        }

        if (path.startsWith("/api/admin/") || path.startsWith("/api/leave/reviewer/")) {
            require(userId, ROLE_COUNSELOR, ROLE_ADMIN);
            return true;
        }

        if (path.startsWith("/api/ai/chat")) {
            require(userId, ROLE_STUDENT, ROLE_CADRE);
            return true;
        }

        if (path.startsWith("/api/home")
                || path.startsWith("/api/student/")
                || path.startsWith("/api/party/me/")
                || path.startsWith("/api/leave/me/")
                || path.startsWith("/api/messages/")) {
            require(userId, ROLE_STUDENT, ROLE_CADRE);
            return true;
        }

        return true;
    }

    private void require(Long userId, String... roleCodes) {
        if (!roleAccessService.hasAnyRole(userId, roleCodes)) {
            throw new BizException(ResultCode.FORBIDDEN, "当前账号无权访问该功能");
        }
    }
}
