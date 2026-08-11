package com.familyplan.entity;

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
    
    private Integer month;
}
