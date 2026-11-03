package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资金分配实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fund_allocation")
public class FundAllocation extends BaseEntity {
    
    private Long memberId;
    
    private String category;
    
    private BigDecimal percentage;
    
    private String period;
    
    private Integer year;

    /** 月份；年度分配为空。更新时需要把月度改成年度时清空该列。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer month;
}
