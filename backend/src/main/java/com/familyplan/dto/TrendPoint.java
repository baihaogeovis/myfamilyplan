package com.familyplan.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收支趋势点
 */
@Data
public class TrendPoint {
    /** 日期 yyyy-MM-dd */
    private String date;
    /** 收入 */
    private BigDecimal income;
    /** 支出 */
    private BigDecimal expense;

    public TrendPoint(String date, BigDecimal income, BigDecimal expense) {
        this.date = date;
        this.income = income;
        this.expense = expense;
    }
}
