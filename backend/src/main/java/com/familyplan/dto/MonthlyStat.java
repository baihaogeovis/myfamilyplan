package com.familyplan.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度统计项
 */
@Data
public class MonthlyStat {
    /** 月份（1-12） */
    private Integer month;
    /** 当月收入 */
    private BigDecimal income;
    /** 当月支出 */
    private BigDecimal expense;

    public MonthlyStat() {
    }

    public MonthlyStat(Integer month, BigDecimal income, BigDecimal expense) {
        this.month = month;
        this.income = income == null ? BigDecimal.ZERO : income;
        this.expense = expense == null ? BigDecimal.ZERO : expense;
    }
}
