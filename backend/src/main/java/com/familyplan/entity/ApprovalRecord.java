package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_record")
public class ApprovalRecord extends BaseEntity {
    
    private Long applicantId;
    
    private BigDecimal amount;
    
    private String purpose;
    
    private String status;
    
    private Long approverId;
    
    private String remark;
    
    private LocalDateTime approveTime;
}
