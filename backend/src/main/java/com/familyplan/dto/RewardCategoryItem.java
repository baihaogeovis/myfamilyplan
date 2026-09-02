package com.familyplan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 奖励额度分类汇总项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardCategoryItem {
    /** 分类 */
    private String category;
    /** 额度(正数, 表示该分类消耗量) */
    private Integer points;
}
