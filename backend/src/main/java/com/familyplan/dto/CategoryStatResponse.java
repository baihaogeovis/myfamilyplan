package com.familyplan.dto;

import lombok.Data;

import java.util.List;

/**
 * 分类统计响应（当月收支按分类汇总）
 */
@Data
public class CategoryStatResponse {
    /** 收入分类汇总 */
    private List<CategoryItem> income;
    /** 支出分类汇总 */
    private List<CategoryItem> expense;
}
