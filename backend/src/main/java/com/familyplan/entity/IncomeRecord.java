package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收入记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("income_record")
public class IncomeRecord extends BaseEntity {
    
    private Long memberId;
    
    private BigDecimal amount;
    
    private String category;
    
    private String source;
    
    private LocalDate incomeDate;
    
    private String remark;
}
