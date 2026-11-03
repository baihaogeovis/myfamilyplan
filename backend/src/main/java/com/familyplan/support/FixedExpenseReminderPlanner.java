package com.familyplan.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 根据固定支出缴费日，决定本月与下月要不要写提醒。
 * 不访问数据库。同一账单同一账期的幂等键固定为 FE:{id}:{yyyyMM}。
 */
public final class FixedExpenseReminderPlanner {

    private FixedExpenseReminderPlanner() {
    }

    public record ReminderDraft(String bizKey, String phase, String title, String content) {
    }

    public static String bizKey(Long expenseId, YearMonth period) {
        return "FE:" + expenseId + ":" + period.getYear()
                + String.format("%02d", period.getMonthValue());
    }

    public static LocalDate dueDate(YearMonth period, int dueDay) {
        int day = Math.min(Math.max(dueDay, 1), period.lengthOfMonth());
        return period.atDay(day);
    }

    /**
     * @param leadDays 到期前几天开始提醒，不含到期当天；当天单独记为「今天缴费」
     */
    public static List<ReminderDraft> plan(LocalDate today, int leadDays, Long expenseId,
                                           String expenseName, BigDecimal amount, Integer dueDay) {
        if (today == null || expenseId == null || dueDay == null || dueDay < 1) {
            return List.of();
        }
        int lead = Math.max(0, Math.min(leadDays, 27));
        String name = (expenseName == null || expenseName.isBlank()) ? "固定支出" : expenseName.trim();
        YearMonth current = YearMonth.from(today);
        List<ReminderDraft> drafts = new ArrayList<>(2);
        for (YearMonth period : List.of(current, current.plusMonths(1))) {
            LocalDate due = dueDate(period, dueDay);
            long daysUntil = ChronoUnit.DAYS.between(today, due);
            String phase = null;
            if (daysUntil == 0) {
                phase = "DUE";
            } else if (daysUntil > 0 && daysUntil <= lead) {
                phase = "UPCOMING";
            } else if (daysUntil < 0 && period.equals(current)) {
                phase = "OVERDUE";
            }
            if (phase != null) {
                drafts.add(draft(expenseId, period, phase, name, amount, due, daysUntil));
            }
        }
        return drafts;
    }

    private static ReminderDraft draft(Long expenseId, YearMonth period, String phase, String name,
                                       BigDecimal amount, LocalDate due, long daysUntil) {
        String prefix = switch (phase) {
            case "DUE" -> "今天缴费：";
            case "OVERDUE" -> "缴费已逾期：";
            default -> "即将缴费：";
        };
        String title = prefix + name;
        if (title.length() > 100) {
            title = title.substring(0, 100);
        }
        String dateText = due.getMonthValue() + "月" + due.getDayOfMonth() + "日";
        StringBuilder content = new StringBuilder();
        content.append("「").append(name).append("」");
        switch (phase) {
            case "DUE" -> content.append("今天到期");
            case "UPCOMING" -> content.append("将于 ").append(dateText)
                    .append(" 到期（还有 ").append(daysUntil).append(" 天）");
            default -> content.append("已于 ").append(dateText)
                    .append(" 到期，逾期 ").append(Math.abs(daysUntil)).append(" 天");
        }
        if (amount != null) {
            content.append("，金额 ¥")
                    .append(amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        content.append("。");
        if ("OVERDUE".equals(phase)) {
            content.append("请尽快缴费。");
        } else {
            content.append("请记得缴费。");
        }
        return new ReminderDraft(bizKey(expenseId, period), phase, title, content.toString());
    }
}
