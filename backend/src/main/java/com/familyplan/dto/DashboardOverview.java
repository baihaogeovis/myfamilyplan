package com.familyplan.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表盘概览数据
 */
@Data
public class DashboardOverview {
    /** 当月收入 */
    private BigDecimal monthIncome;
    /** 当月支出 */
    private BigDecimal monthExpense;
    /** 当月预算剩余 */
    private BigDecimal budgetRemaining;
    /** 待我审批数量 */
    private Long pendingCount;
    /** 家庭成员数量 */
    private Long memberCount;
}
