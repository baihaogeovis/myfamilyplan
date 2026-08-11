package com.familyplan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 最近收支流水
 */
@Data
public class RecentTransaction {
    private Long id;
    /** INCOME / EXPENSE */
    private String type;
    private String category;
    private BigDecimal amount;
    private LocalDate date;
    private String remark;

    public RecentTransaction(Long id, String type, String category, BigDecimal amount, LocalDate date, String remark) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.remark = remark;
    }
}
