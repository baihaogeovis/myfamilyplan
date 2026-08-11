package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 固定支出实体(物业费/车位费/车位管理费/话费网费等)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fixed_expense")
public class FixedExpense extends BaseEntity {

    /** 成员ID */
    private Long memberId;

    /** 支出名称(如物业费) */
    private String expenseName;

    /** 分类(PROPERTY-物业费,PARKING-车位费,PARKING_MANAGE-车位管理费,PHONE-话费,INTERNET-网费,UTILITY-水电气,OTHER-其他) */
    private String category;

    /** 月均金额 */
    private BigDecimal amount;

    /** 每月缴费日(1-28) */
    private Integer dueDay;

    /** 是否启用(0-停用,1-启用) */
    private Integer active;

    /** 是否自动记账(0-否,1-是) */
    private Integer autoRecord;

    /** 备注 */
    private String remark;
}
