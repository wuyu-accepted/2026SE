package com.ruc.platform.config;

import com.ruc.platform.auth.entity.User;
import com.ruc.platform.auth.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initAdminPasswords() {
        List<AccountSeed> seeds = Arrays.asList(
                new AccountSeed("admin", "admin123"),
                new AccountSeed("10000001", "counselor123"),
                new AccountSeed("00000001", "password"),
                new AccountSeed("00000002", "password"),
                new AccountSeed("00000003", "password"),
                new AccountSeed("2023001", "password"),
                new AccountSeed("2023002", "password"),
                new AccountSeed("2023003", "password")
        );

        for (AccountSeed seed : seeds) {
            User user = userMapper.selectByStudentNo(seed.studentNo);
            if (user != null && ("PLACEHOLDER".equals(user.getPasswordHash()) ||
                    !passwordEncoder.matches(seed.password, user.getPasswordHash()))) {
                user.setPasswordHash(passwordEncoder.encode(seed.password));
                userMapper.updateById(user);
                log.info("已更新账号密码: {}, id: {}", seed.studentNo, user.getId());
            }
        }
    }

    private record AccountSeed(String studentNo, String password) {}
}
