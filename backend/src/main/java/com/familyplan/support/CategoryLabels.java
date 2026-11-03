package com.familyplan.support;

import java.util.Map;

/**
 * 导出与展示用的分类中文名。未知编码原样返回，避免丢数据。
 */
public final class CategoryLabels {

    private static final Map<String, String> CATEGORIES = Map.ofEntries(
            Map.entry("SALARY", "工资"),
            Map.entry("BONUS", "奖金"),
            Map.entry("INVESTMENT", "投资"),
            Map.entry("PART_TIME", "兼职"),
            Map.entry("SIDEJOB", "兼职"),
            Map.entry("GIFT", "红包"),
            Map.entry("OTHER", "其他"),
            Map.entry("FOOD", "餐饮"),
            Map.entry("TRANSPORT", "交通"),
            Map.entry("SHOPPING", "购物"),
            Map.entry("HOUSING", "居住"),
            Map.entry("EDUCATION", "教育"),
            Map.entry("MEDICAL", "医疗"),
            Map.entry("ENTERTAINMENT", "娱乐"),
            Map.entry("SAVINGS", "储蓄"),
            Map.entry("UTILITY", "水电气"),
            Map.entry("SOCIAL", "人情"),
            Map.entry("CHILDCARE", "育儿"),
            Map.entry("TOTAL", "总预算"),
            Map.entry("PROPERTY", "物业费"),
            Map.entry("PARKING", "车位费"),
            Map.entry("PARKING_MANAGE", "车位管理费"),
            Map.entry("PARKMGMT", "车位管理费"),
            Map.entry("PHONE", "话费"),
            Map.entry("INTERNET", "网费")
    );

    private static final Map<String, String> EXPENSE_TYPES = Map.of(
            "NORMAL", "普通",
            "BILL", "账单",
            "SAVING", "储蓄"
    );

    private static final Map<String, String> PERIODS = Map.of(
            "MONTH", "月度",
            "YEAR", "年度",
            "WEEK", "周",
            "MONTHLY", "月度",
            "YEARLY", "年度",
            "CUSTOM", "自定义"
    );

    private CategoryLabels() {
    }

    public static String category(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return CATEGORIES.getOrDefault(code, code);
    }

    public static String expenseType(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return EXPENSE_TYPES.getOrDefault(code, code);
    }

    public static String period(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return PERIODS.getOrDefault(code, code);
    }
}
