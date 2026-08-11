package com.familyplan.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类汇总项
 */
@Data
public class CategoryItem {
    /** 分类名称 */
    private String category;
    /** 金额 */
    private BigDecimal amount;

    public CategoryItem() {
    }

    public CategoryItem(String category, BigDecimal amount) {
        this.category = category;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }
}
