package com.familyplan.support;

import com.familyplan.common.BusinessException;
import com.familyplan.entity.FundAllocation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 资金分配校验：单条比例在 (0, 100]，同一成员同一周期合计不超过 100%。
 */
public final class FundAllocationRules {

    public static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private FundAllocationRules() {
    }

    public static final class PeriodKey {
        public final String period;
        public final int year;
        public final Integer month;

        public PeriodKey(String period, int year, Integer month) {
            this.period = period;
            this.year = year;
            this.month = month;
        }
    }

    public static PeriodKey periodKey(String period, Integer year, Integer month) {
        if (period == null || period.isBlank()) {
            throw new BusinessException("请选择分配周期");
        }
        String normalized = period.trim().toUpperCase();
        if (!"MONTH".equals(normalized) && !"YEAR".equals(normalized)) {
            throw new BusinessException("分配周期仅支持月度或年度");
        }
        if (year == null || year < 2000 || year > 2100) {
            throw new BusinessException("请选择有效年份");
        }
        Integer resolvedMonth = null;
        if ("MONTH".equals(normalized)) {
            if (month == null || month < 1 || month > 12) {
                throw new BusinessException("请选择月份");
            }
            resolvedMonth = month;
        }
        return new PeriodKey(normalized, year, resolvedMonth);
    }

    public static BigDecimal normalizePercentage(BigDecimal raw) {
        if (raw == null) {
            throw new BusinessException("请填写分配比例");
        }
        BigDecimal value = raw.stripTrailingZeros();
        if (value.scale() < 0) {
            value = value.setScale(0);
        }
        if (value.scale() > 2) {
            throw new BusinessException("分配比例最多保留两位小数");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("分配比例需大于 0 且不超过 100");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static void normalize(FundAllocation item) {
        if (item.getCategory() == null || item.getCategory().isBlank()) {
            throw new BusinessException("请选择分配分类");
        }
        String category = item.getCategory().trim();
        if (category.length() > 50) {
            throw new BusinessException("分类名称过长");
        }
        item.setCategory(category);
        PeriodKey key = periodKey(item.getPeriod(), item.getYear(), item.getMonth());
        item.setPeriod(key.period);
        item.setYear(key.year);
        item.setMonth(key.month);
        item.setPercentage(normalizePercentage(item.getPercentage()));
    }

    /**
     * others 为同一周期里除当前记录外的比例合计，incoming 为本次比例。
     */
    public static void assertWithinHundred(BigDecimal others, BigDecimal incoming) {
        BigDecimal base = others == null ? BigDecimal.ZERO : others;
        BigDecimal next = incoming == null ? BigDecimal.ZERO : incoming;
        BigDecimal total = base.add(next);
        if (total.compareTo(HUNDRED) > 0) {
            throw new BusinessException("该周期分配比例合计不能超过 100%，当前其他项已占 "
                    + base.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + "%，加上本次将达到 "
                    + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
        }
    }
}
