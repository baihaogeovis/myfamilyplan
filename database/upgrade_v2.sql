-- ============================================
-- 家庭成长系统 二期升级脚本
-- 新增 4 大功能模块数据表：
--   1. daily_task      每日任务表(家庭/工作打勾工作台)
--   2. fixed_expense   固定支出表(物业费/车位费/话费网费等)
--   3. life_plan       美好生活计划表(家电购买/年度旅游)
--   4. growth_plan     儿童成长计划表(短期/长期目标)
-- 适用：已在运行一期数据库的系统，直接执行本脚本即可
-- ============================================

USE family_plan;

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
