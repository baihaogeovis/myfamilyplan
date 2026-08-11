package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 支出记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("expense_record")
public class ExpenseRecord extends BaseEntity {
    
    private Long memberId;
    
    private BigDecimal amount;
    
    private String category;
    
    private String type;
    
    private LocalDate expenseDate;
    
    private String remark;
}
