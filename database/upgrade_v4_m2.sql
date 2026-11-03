-- ============================================
-- 家庭成长系统 M2 升级脚本
-- 固定支出缴费提醒：notification 增加业务幂等键 biz_key
-- 同一家庭、同一账单、同一账期只保留一条提醒
-- 适用：已在运行的库。可重复执行。
-- 说明：后端启动时 DataInitializer 也会幂等补上该列和唯一索引
-- ============================================

USE family_plan;

SET @biz_key_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification' AND COLUMN_NAME = 'biz_key'
);
SET @add_biz_key = IF(
    @biz_key_exists = 0,
    'ALTER TABLE notification ADD COLUMN biz_key VARCHAR(64) NULL COMMENT ''业务幂等键(固定支出提醒 FE:{账单ID}:{yyyyMM}，手工提醒为空)''',
    'SELECT 1'
);
PREPARE stmt_biz_key FROM @add_biz_key;
EXECUTE stmt_biz_key;
DEALLOCATE PREPARE stmt_biz_key;

SET @biz_idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification' AND INDEX_NAME = 'uk_notification_biz'
);
SET @add_biz_idx = IF(
    @biz_idx_exists = 0,
    'ALTER TABLE notification ADD UNIQUE INDEX uk_notification_biz (family_id, biz_key)',
    'SELECT 1'
);
PREPARE stmt_biz_idx FROM @add_biz_idx;
EXECUTE stmt_biz_idx;
DEALLOCATE PREPARE stmt_biz_idx;
