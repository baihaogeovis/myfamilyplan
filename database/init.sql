-- 创建数据库
CREATE DATABASE IF NOT EXISTS family_plan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE family_plan;

-- 家庭表(家庭账号:一个家庭一个登录账号,家庭成员共用该账号登录)
CREATE TABLE IF NOT EXISTS family (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家庭ID',
    family_name VARCHAR(100) NOT NULL COMMENT '家庭名称',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '家庭登录账号',
    password VARCHAR(200) NOT NULL COMMENT '密码(加密)',
    avatar VARCHAR(255) COMMENT '家庭头像URL',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_family_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表';

-- 用户表(保留兼容历史数据,新系统以家庭账号登录)
CREATE TABLE IF NOT EXISTS user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(加密)',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    role VARCHAR(20) DEFAULT 'ADMIN' COMMENT '角色(ADMIN-管理员,USER-普通用户)',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表(兼容保留)';

-- 家庭成员表
CREATE TABLE IF NOT EXISTS family_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID',
    family_id BIGINT NOT NULL COMMENT '所属家庭ID',
    user_id BIGINT COMMENT '关联用户ID(兼容保留)',
    member_name VARCHAR(50) NOT NULL COMMENT '成员姓名',
    relation VARCHAR(20) NOT NULL COMMENT '关系(OWNER-户主,SPOUSE-配偶,CHILD-子女,PARENT-父母,OTHER-其他)',
    gender VARCHAR(10) COMMENT '性别(男/女)',
    birthday DATE COMMENT '生日',
    phone VARCHAR(20) COMMENT '手机号',
    id_card VARCHAR(18) COMMENT '身份证号',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色(ADMIN-管理员,USER-普通用户)',
    avatar VARCHAR(255) COMMENT '头像URL',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_family_id (family_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员表';

-- 收入记录表
CREATE TABLE IF NOT EXISTS income_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收入ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    category VARCHAR(50) NOT NULL COMMENT '分类(SALARY-工资,BONUS-奖金,INVESTMENT-投资收益,OTHER-其他)',
    source VARCHAR(100) COMMENT '来源',
    income_date DATE NOT NULL COMMENT '收入日期',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id),
    INDEX idx_income_date (income_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收入记录表';

-- 支出记录表
CREATE TABLE IF NOT EXISTS expense_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支出ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    category VARCHAR(50) NOT NULL COMMENT '分类(FOOD-餐饮,TRANSPORT-交通,HOUSING-住房,EDUCATION-教育,MEDICAL-医疗,ENTERTAINMENT-娱乐,SHOPPING-购物,SAVINGS-储蓄,OTHER-其他)',
    type VARCHAR(20) DEFAULT 'NORMAL' COMMENT '类型(NORMAL-普通支出,BILL-账单,SAVING-储蓄)',
    expense_date DATE NOT NULL COMMENT '支出日期',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id),
    INDEX idx_expense_date (expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出记录表';

-- 预算计划表
CREATE TABLE IF NOT EXISTS budget_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预算ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    period_type VARCHAR(20) NOT NULL COMMENT '周期类型(WEEK-周,MONTH-月,YEAR-年)',
    amount DECIMAL(10,2) NOT NULL COMMENT '预算金额',
    month INT COMMENT '月份(1-12)',
    year INT COMMENT '年份',
    week INT COMMENT '周数(1-53)',
    category VARCHAR(50) COMMENT '分类(为空表示总预算)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id),
    INDEX idx_year_month (year, month),
    INDEX idx_year_week (year, week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算计划表';

-- 资金审批表
CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '申请金额',
    purpose VARCHAR(500) NOT NULL COMMENT '申请用途',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING-待审批,APPROVED-已通过,REJECTED-已拒绝)',
    approver_id BIGINT COMMENT '审批人ID',
    remark VARCHAR(500) COMMENT '审批备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    approve_time DATETIME COMMENT '审批时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金审批表';

-- 资金分配表
CREATE TABLE IF NOT EXISTS fund_allocation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分配ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    percentage DECIMAL(5,2) NOT NULL COMMENT '分配比例(%)',
    period VARCHAR(20) NOT NULL COMMENT '周期(MONTH-月,YEAR-年)',
    year INT COMMENT '年份',
    month INT COMMENT '月份',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金分配表';

-- 通知提醒表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    family_id BIGINT NOT NULL COMMENT '所属家庭ID',
    user_id BIGINT COMMENT '关联用户ID(兼容保留)',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(20) NOT NULL COMMENT '类型(BILL-账单提醒,BUDGET-预算预警,APPROVAL-审批通知,SYSTEM-系统通知)',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读(0-未读,1-已读)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_family_id (family_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知提醒表';

-- 已存在数据库的结构补丁：为 notification 补齐 update_time/deleted 字段（如已存在可忽略报错）
ALTER TABLE notification ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE notification ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)';
ALTER TABLE notification ADD COLUMN family_id BIGINT COMMENT '所属家庭ID';
ALTER TABLE notification ADD INDEX idx_family_id (family_id);

-- 历史数据迁移补丁：历史通知归属默认家庭(如已迁移可忽略)
UPDATE notification SET family_id = 1 WHERE family_id IS NULL;

-- 插入默认家庭账号 (家庭账号/密码: admin/admin123)
INSERT INTO family (family_name, username, password, status) VALUES 
('我的家庭', 'admin', '$2a$10$pJqMd5MkmTQUNbEFrAFaj.PMkVh4RrQXmuBKLNjXpglFJ.xIfZic.', 1);

-- 插入默认家庭成员(属于默认家庭)
INSERT INTO family_member (family_id, user_id, member_name, relation, role, status) VALUES 
(1, 1, '管理员', 'OWNER', 'ADMIN', 1);

-- 已存在数据库的结构补丁：为 family_member 补齐新字段（如已存在可忽略报错）
ALTER TABLE family_member ADD COLUMN gender VARCHAR(10) COMMENT '性别(男/女)';
ALTER TABLE family_member ADD COLUMN birthday DATE COMMENT '生日';
ALTER TABLE family_member ADD COLUMN avatar VARCHAR(255) COMMENT '头像URL';
ALTER TABLE family_member ADD COLUMN remark VARCHAR(500) COMMENT '备注';
ALTER TABLE family_member ADD COLUMN family_id BIGINT COMMENT '所属家庭ID';
ALTER TABLE family_member ADD INDEX idx_family_id (family_id);

-- 历史数据迁移补丁：为旧数据创建默认家庭并挂接(如 family 表已有默认家庭可忽略)
INSERT INTO family (family_name, username, password, status)
SELECT '我的家庭', 'admin', '$2a$10$pJqMd5MkmTQUNbEFrAFaj.PMkVh4RrQXmuBKLNjXpglFJ.xIfZic.', 1
WHERE NOT EXISTS (SELECT 1 FROM family WHERE username = 'admin');

UPDATE family_member SET family_id = 1 WHERE family_id IS NULL;

-- ============================================
-- 二期扩展：4 大新功能模块数据表
-- ============================================

-- 每日任务表(家庭/工作打勾工作台)
CREATE TABLE IF NOT EXISTS daily_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_type VARCHAR(20) DEFAULT 'FAMILY' COMMENT '类型(FAMILY-家庭,WORK-工作)',
    task_date DATE NOT NULL COMMENT '任务日期',
    completed TINYINT DEFAULT 0 COMMENT '是否完成(0-未完成,1-已完成)',
    priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '优先级(HIGH-高,MEDIUM-中,LOW-低)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_date (member_id, task_date),
    INDEX idx_task_date (task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日任务表';

-- 固定支出表(物业费/车位费/车位管理费/话费网费等)
CREATE TABLE IF NOT EXISTS fixed_expense (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    expense_name VARCHAR(50) NOT NULL COMMENT '支出名称(如物业费)',
    category VARCHAR(50) NOT NULL COMMENT '分类(PROPERTY-物业费,PARKING-车位费,PARKING_MANAGE-车位管理费,PHONE-话费,INTERNET-网费,UTILITY-水电气,OTHER-其他)',
    amount DECIMAL(10,2) NOT NULL COMMENT '月均金额',
    due_day INT COMMENT '每月缴费日(1-28)',
    active TINYINT DEFAULT 1 COMMENT '是否启用(0-停用,1-启用)',
    auto_record TINYINT DEFAULT 0 COMMENT '是否自动记账(0-否,1-是)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定支出表';

-- 美好生活计划表(家电购买/年度旅游)
CREATE TABLE IF NOT EXISTS life_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    member_id BIGINT NOT NULL COMMENT '成员ID',
    plan_type VARCHAR(20) NOT NULL COMMENT '类型(APPLIANCE-家电,TRAVEL-旅游)',
    plan_name VARCHAR(100) NOT NULL COMMENT '计划名称(家电名称/旅游目的地)',
    category VARCHAR(50) COMMENT '分类(家电:电视/冰箱/空调/洗衣机等; 旅游:国内/国外)',
    budget DECIMAL(12,2) COMMENT '预算金额',
    plan_date DATE COMMENT '计划时间/出行日期',
    status VARCHAR(20) DEFAULT 'PLANNED' COMMENT '状态(PLANNED-计划中,COMPLETED-已完成,CANCELLED-已取消)',
    priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '优先级(HIGH-高,MEDIUM-中,LOW-低)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id),
    INDEX idx_plan_type (plan_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美好生活计划表';

-- 儿童成长计划表(短期/长期目标)
CREATE TABLE IF NOT EXISTS growth_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    member_id BIGINT NOT NULL COMMENT '成员ID(孩子)',
    title VARCHAR(100) NOT NULL COMMENT '目标标题',
    goal_type VARCHAR(20) NOT NULL COMMENT '类型(SHORT-短期目标,LONG-长期目标)',
    category VARCHAR(50) COMMENT '分类(学习/运动/艺术/习惯/社交/其他)',
    description TEXT COMMENT '目标描述',
    target_date DATE COMMENT '目标达成日期',
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '状态(IN_PROGRESS-进行中,ACHIEVED-已达成,GIVEN_UP-已放弃)',
    progress INT DEFAULT 0 COMMENT '完成进度(0-100)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_member_id (member_id),
    INDEX idx_goal_type (goal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='儿童成长计划表';
