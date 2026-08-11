package com.familyplan.config;

import com.familyplan.entity.UserInfo;
import com.familyplan.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动数据初始化：确保默认管理员账户存在且密码哈希有效。
 * 可自动修复 init.sql 中遗留的占位哈希，无需手动重跑 SQL。
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private static final String PLACEHOLDER_PASSWORD =
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        UserInfo admin = userInfoService.getUserByUsername(DEFAULT_ADMIN_USERNAME);
        if (admin == null) {
            UserInfo user = new UserInfo();
            user.setUsername(DEFAULT_ADMIN_USERNAME);
            user.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            user.setNickname("系统管理员");
            user.setRole("ADMIN");
            user.setStatus(1);
            userInfoService.save(user);
            log.info("已创建默认管理员账户: {}", DEFAULT_ADMIN_USERNAME);
        } else if (PLACEHOLDER_PASSWORD.equals(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            userInfoService.updateById(admin);
            log.info("已修正默认管理员账户(admin)的密码哈希");
        }
    }
}
