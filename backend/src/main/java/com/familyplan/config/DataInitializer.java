package com.familyplan.config;

import com.familyplan.entity.Family;
import com.familyplan.entity.FamilyMember;
import com.familyplan.entity.UserInfo;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IFamilyService;
import com.familyplan.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动数据初始化：
 * 1. 自动执行老库结构迁移(多家庭改造所需的 family 表 / family_id 列等, 幂等可重复执行)
 * 2. 确保默认家庭账号与默认家庭成员存在
 * 3. 兼容保留: 初始化 user_info 中的 admin 账号(历史数据, 不再用于登录)
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_FAMILY_USERNAME = "admin";
    private static final String DEFAULT_FAMILY_PASSWORD = "admin123";

    @Autowired
    private IFamilyService familyService;

    @Autowired
    private IFamilyMemberService memberService;

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 老库结构迁移: 确保多家庭改造所需表/列存在(幂等, 可重复执行)
        migrateSchema();
        ensureDefaultFamily();
        // 老库历史成员补挂到默认家庭
        bindOrphanMembersToDefaultFamily();
        ensureDefaultFamilyMember();
        // 兼容保留：初始化 user_info 中的 admin 账号(历史数据, 不再用于登录)
        ensureLegacyAdminUser();
    }

    /**
     * 结构迁移: 幂等升级老库, 新库(已执行 init.sql)不会重复修改。
     */
    private void migrateSchema() {
        createFamilyTable();
        createFamilyMemberTable();
        createNotificationTable();

        // family_member 老表缺列补全
        addColumnIfMissing("family_member", "gender", "VARCHAR(10) COMMENT '性别(男/女)'");
        addColumnIfMissing("family_member", "birthday", "DATE COMMENT '生日'");
        addColumnIfMissing("family_member", "avatar", "VARCHAR(255) COMMENT '头像URL'");
        addColumnIfMissing("family_member", "remark", "VARCHAR(500) COMMENT '备注'");
        addColumnIfMissing("family_member", "family_id", "BIGINT COMMENT '所属家庭ID'");
        addIndexIfMissing("family_member", "idx_family_id",
                "ALTER TABLE family_member ADD INDEX idx_family_id (family_id)");

        // 老库 user_id 为 NOT NULL 且无默认值时, 新增成员会因缺省值失败, 统一补默认 0(幂等)
        ensureMemberUserIdDefault();

        // notification 老表缺列补全
        addColumnIfMissing("notification", "family_id", "BIGINT COMMENT '所属家庭ID'");
        addColumnIfMissing("notification", "update_time",
                "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
        addColumnIfMissing("notification", "deleted", "TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)'");
        addIndexIfMissing("notification", "idx_family_id",
                "ALTER TABLE notification ADD INDEX idx_family_id (family_id)");
    }

    private void createFamilyTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS family (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家庭ID'," +
                    "family_name VARCHAR(100) NOT NULL COMMENT '家庭名称'," +
                    "username VARCHAR(50) NOT NULL UNIQUE COMMENT '家庭登录账号'," +
                    "password VARCHAR(200) NOT NULL COMMENT '密码(加密)'," +
                    "avatar VARCHAR(255) COMMENT '家庭头像URL'," +
                    "remark VARCHAR(500) COMMENT '备注'," +
                    "status TINYINT DEFAULT 1 COMMENT '状态(0-禁用,1-启用)'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)'," +
                    "INDEX idx_family_username (username)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表'");
        } catch (Exception e) {
            log.warn("检查/创建 family 表失败(可忽略): {}", e.getMessage());
        }
    }

    private void createFamilyMemberTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS family_member (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID'," +
                    "family_id BIGINT COMMENT '所属家庭ID'," +
                    "user_id BIGINT COMMENT '关联用户ID(兼容保留)'," +
                    "member_name VARCHAR(50) NOT NULL COMMENT '成员姓名'," +
                    "relation VARCHAR(20) NOT NULL COMMENT '关系(OWNER-户主,SPOUSE-配偶,CHILD-子女,PARENT-父母,OTHER-其他)'," +
                    "gender VARCHAR(10) COMMENT '性别(男/女)'," +
                    "birthday DATE COMMENT '生日'," +
                    "phone VARCHAR(20) COMMENT '手机号'," +
                    "id_card VARCHAR(18) COMMENT '身份证号'," +
                    "role VARCHAR(20) DEFAULT 'USER' COMMENT '角色(ADMIN-管理员,USER-普通用户)'," +
                    "avatar VARCHAR(255) COMMENT '头像URL'," +
                    "remark VARCHAR(500) COMMENT '备注'," +
                    "status TINYINT DEFAULT 1 COMMENT '状态(0-禁用,1-启用)'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)'," +
                    "INDEX idx_family_id (family_id)," +
                    "INDEX idx_user_id (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员表'");
        } catch (Exception e) {
            log.warn("检查/创建 family_member 表失败(可忽略): {}", e.getMessage());
        }
    }

    private void createNotificationTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS notification (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID'," +
                    "family_id BIGINT COMMENT '所属家庭ID'," +
                    "user_id BIGINT COMMENT '关联用户ID(兼容保留)'," +
                    "title VARCHAR(100) NOT NULL COMMENT '通知标题'," +
                    "content TEXT COMMENT '通知内容'," +
                    "type VARCHAR(20) NOT NULL COMMENT '类型(BILL-账单提醒,BUDGET-预算预警,APPROVAL-审批通知,SYSTEM-系统通知)'," +
                    "is_read TINYINT DEFAULT 0 COMMENT '是否已读(0-未读,1-已读)'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)'," +
                    "INDEX idx_family_id (family_id)," +
                    "INDEX idx_user_id (user_id)," +
                    "INDEX idx_is_read (is_read)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知提醒表'");
        } catch (Exception e) {
            log.warn("检查/创建 notification 表失败(可忽略): {}", e.getMessage());
        }
    }

    /**
     * 列不存在时补列(幂等)
     */
    private void addColumnIfMissing(String table, String column, String ddlType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType);
            log.info("结构迁移: 已为表 {} 添加列 {}", table, column);
        } catch (Exception e) {
            log.warn("结构迁移: 为表 {} 添加列 {} 失败(可忽略): {}", table, column, e.getMessage());
        }
    }

    /**
     * 修正 family_member.user_id 缺省值问题(幂等):
     * 老库该列可能为 NOT NULL 且无默认值, 导致新增成员报 "Field 'user_id' doesn't have a default value"
     */
    private void ensureMemberUserIdDefault() {
        try {
            // 1. 历史 NULL 值置 0, 避免改成 NOT NULL 时报错
            jdbcTemplate.update("UPDATE family_member SET user_id = 0 WHERE user_id IS NULL");
            // 2. 统一为 NOT NULL DEFAULT 0
            jdbcTemplate.execute("ALTER TABLE family_member MODIFY COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '关联用户ID(兼容保留)'");
            log.info("结构迁移: family_member.user_id 已设置为默认值 0");
        } catch (Exception e) {
            log.warn("结构迁移: 修正 family_member.user_id 默认值失败(可忽略): {}", e.getMessage());
        }
    }

    /**
     * 索引不存在时补索引(幂等)
     */
    private void addIndexIfMissing(String table, String indexName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                    Integer.class, table, indexName);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("结构迁移: 已为表 {} 添加索引 {}", table, indexName);
        } catch (Exception e) {
            log.warn("结构迁移: 为表 {} 添加索引 {} 失败(可忽略): {}", table, indexName, e.getMessage());
        }
    }

    /**
     * 确保默认家庭账号存在
     */
    private void ensureDefaultFamily() {
        Family family = familyService.getByUsername(DEFAULT_FAMILY_USERNAME);
        if (family == null) {
            family = new Family();
            family.setFamilyName("我的家庭");
            family.setUsername(DEFAULT_FAMILY_USERNAME);
            family.setPassword(passwordEncoder.encode(DEFAULT_FAMILY_PASSWORD));
            family.setStatus(1);
            familyService.save(family);
            log.info("已创建默认家庭账号: {}", DEFAULT_FAMILY_USERNAME);
        }
    }

    /**
     * 老库历史成员(family_id 为空)补挂到默认家庭
     */
    private void bindOrphanMembersToDefaultFamily() {
        try {
            Family family = familyService.getByUsername(DEFAULT_FAMILY_USERNAME);
            if (family == null) {
                return;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE family_member SET family_id = ? WHERE family_id IS NULL", family.getId());
            if (updated > 0) {
                log.info("结构迁移: 已将 {} 条历史成员记录挂接到默认家庭({})", updated, DEFAULT_FAMILY_USERNAME);
            }
        } catch (Exception e) {
            log.warn("结构迁移: 历史成员挂接默认家庭失败(可忽略): {}", e.getMessage());
        }
    }

    /**
     * 确保默认家庭下至少有一个户主成员
     */
    private void ensureDefaultFamilyMember() {
        Family family = familyService.getByUsername(DEFAULT_FAMILY_USERNAME);
        if (family == null) {
            return;
        }
        List<FamilyMember> members = memberService.getListByFamilyId(family.getId());
        if (members.isEmpty()) {
            FamilyMember owner = new FamilyMember();
            owner.setFamilyId(family.getId());
            owner.setUserId(0L);
            owner.setMemberName("管理员");
            owner.setRelation("OWNER");
            owner.setRole("ADMIN");
            owner.setStatus(1);
            memberService.save(owner);
            log.info("已为默认家庭创建户主成员");
        }
    }

    /**
     * 兼容保留：历史 user_info 表 admin 账号(新系统登录不再使用该表)
     */
    private void ensureLegacyAdminUser() {
        UserInfo admin = userInfoService.getUserByUsername(DEFAULT_FAMILY_USERNAME);
        if (admin == null) {
            UserInfo user = new UserInfo();
            user.setUsername(DEFAULT_FAMILY_USERNAME);
            user.setPassword(passwordEncoder.encode(DEFAULT_FAMILY_PASSWORD));
            user.setNickname("系统管理员");
            user.setRole("ADMIN");
            user.setStatus(1);
            userInfoService.save(user);
        }
    }
}
