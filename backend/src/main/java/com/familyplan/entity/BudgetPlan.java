package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算计划实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("budget_plan")
public class BudgetPlan extends BaseEntity {
    
    private Long memberId;
    
    private String periodType;
    
    private BigDecimal amount;
    
    private Integer month;
    
    private Integer year;
    
    private Integer week;
    
    private String category;
}
