package com.familyplan.dto;

import com.familyplan.entity.FixedExpense;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金总览统计结果
 */
@Data
public class FinanceOverview {

    /** 累计收入(存了多少) */
    private BigDecimal totalIncome;

    /** 累计支出(花了多少) */
    private BigDecimal totalExpense;

    /** 累计结余(当前存款) */
    private BigDecimal totalBalance;

    /** 本月收入 */
    private BigDecimal monthIncome;

    /** 本月支出 */
    private BigDecimal monthExpense;

    /** 本月结余 */
    private BigDecimal monthBalance;

    /** 本月固定支出合计 */
    private BigDecimal monthFixedTotal;

    /** 固定支出配置列表 */
    private List<FixedExpense> fixedExpenses;
}
