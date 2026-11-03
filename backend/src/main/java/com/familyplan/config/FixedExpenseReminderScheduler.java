package com.familyplan.config;

import com.familyplan.service.FixedExpenseReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 固定支出缴费提醒的定时入口。
 *
 * <p>真正写提醒有两条路，用的是同一个 {@link FixedExpenseReminderService}：
 * <ol>
 *   <li>打开提醒列表，或刷新未读数量时，按当前家庭立即生成。这是平时能在提醒中心里看到的原因。</li>
 *   <li>本类每天按 {@code familyplan.reminder.cron}（默认每天 08:10，时区 {@code familyplan.reminder.zone}）
 *       扫描全部家庭，避免当天没人打开应用。</li>
 * </ol>
 * 提前天数见 {@code familyplan.reminder.lead-days}（默认 3，不含到期当天）。
 * 同一账单同一账期只保留一条，键为 {@code FE:{账单ID}:{yyyyMM}}。
 */
@Slf4j
@Configuration
@EnableScheduling
public class FixedExpenseReminderScheduler {

    @Autowired
    private FixedExpenseReminderService reminderService;

    @Scheduled(cron = "${familyplan.reminder.cron:0 10 8 * * ?}", zone = "${familyplan.reminder.zone:Asia/Shanghai}")
    public void generateDaily() {
        log.info("开始定时生成固定支出缴费提醒");
        reminderService.generateAll();
    }
}
