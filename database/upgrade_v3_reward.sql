-- ============================================
-- 家庭成长系统 三期升级脚本
-- 新增「儿童奖励额度」模块数据表：
--   1. reward_account  奖励额度账户表(一个孩子一条, 余额为唯一真相)
--   2. reward_item     奖励活动方案表(家庭维度价目表: 花额度项目与赚额度项目)
--   3. reward_txn      奖励额度流水表(唯一账本, 仅 CONFIRMED 参与余额与统计)
-- 适用：已在运行一期/二期数据库的系统，直接执行本脚本即可
-- 说明：后端启动时 DataInitializer 也会幂等补建这三张表，漏跑本脚本不会导致接口报错
-- ============================================

USE family_plan;

-- 奖励额度账户表
CREATE TABLE IF NOT EXISTS reward_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账户ID',
    family_id BIGINT NOT NULL COMMENT '所属家庭ID',
    member_id BIGINT NOT NULL COMMENT '成员ID(孩子)',
    balance INT DEFAULT 0 COMMENT '当前可用额度',
    weekly_quota INT DEFAULT 0 COMMENT '每周期发放额度(0-不自动发放)',
    period_type VARCHAR(20) DEFAULT 'WEEK' COMMENT '发放周期(WEEK-周,MONTH-月)',
    carry_over TINYINT DEFAULT 1 COMMENT '余额是否跨周期结转(0-清零,1-累积)',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    UNIQUE KEY uk_account_member (member_id),
    INDEX idx_account_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励额度账户表';

-- 奖励活动方案表
CREATE TABLE IF NOT EXISTS reward_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '方案ID',
    family_id BIGINT NOT NULL COMMENT '所属家庭ID',
    item_name VARCHAR(50) NOT NULL COMMENT '方案名称(如看电视/认真学习)',
    item_type VARCHAR(10) NOT NULL COMMENT '类型(EARN-赚额度,SPEND-花额度)',
    category VARCHAR(20) COMMENT '分类(STUDY-学习,SPORTS-运动,HABIT-习惯,HOUSEWORK-家务,SCREEN-屏幕,TOY-玩具,SNACK-零食,ACTIVITY-活动,OTHER-其他)',
    pricing_mode VARCHAR(10) DEFAULT 'TIMES' COMMENT '计价方式(TIMES-按次,DURATION-按时长)',
    points INT NOT NULL COMMENT '单价额度(TIMES-每次额度,DURATION-每档额度)',
    unit_minutes INT DEFAULT 30 COMMENT '按时长计价的一档分钟数',
    icon VARCHAR(20) COMMENT '图标文字',
    color VARCHAR(20) COMMENT '展示颜色',
    sort_order INT DEFAULT 0 COMMENT '排序(小在前)',
    active TINYINT DEFAULT 1 COMMENT '是否启用(0-停用,1-启用)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_item_family_type (family_id, item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励活动方案表';

-- 奖励额度流水表
CREATE TABLE IF NOT EXISTS reward_txn (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    family_id BIGINT NOT NULL COMMENT '所属家庭ID',
    member_id BIGINT NOT NULL COMMENT '成员ID(孩子)',
    item_id BIGINT COMMENT '活动方案ID(手动调整为空)',
    item_name VARCHAR(50) COMMENT '方案名称快照',
    category VARCHAR(20) COMMENT '方案分类快照(用于流水分类统计)',
    direction VARCHAR(10) NOT NULL COMMENT '方向(GRANT-周期发放,EARN-行为赚取,SPEND-消费扣减,EXPIRE-周期清零,ADJUST-手动调整)',
    status VARCHAR(12) DEFAULT 'CONFIRMED' COMMENT '状态(PENDING-待确认,CONFIRMED-已生效,REJECTED-已驳回)',
    points INT NOT NULL COMMENT '本次变动额度(带符号, 消费与清零为负)',
    quantity INT DEFAULT 1 COMMENT '次数',
    minutes INT COMMENT '时长分钟数(按时长方案)',
    period_key VARCHAR(16) COMMENT '归属周期(如2026-W36/2026-08)',
    operator_member_id BIGINT COMMENT '操作/确认的父母成员ID',
    confirm_time DATETIME COMMENT '确认时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    INDEX idx_txn_member_time (member_id, create_time),
    INDEX idx_txn_member_status (member_id, status),
    INDEX idx_txn_member_period (member_id, period_key, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励额度流水表';
