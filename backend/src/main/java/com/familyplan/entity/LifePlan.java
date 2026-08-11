package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 美好生活计划实体(家电购买/年度旅游)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_plan")
public class LifePlan extends BaseEntity {

    /** 成员ID */
    private Long memberId;

    /** 类型(APPLIANCE-家电,TRAVEL-旅游) */
    private String planType;

    /** 计划名称(家电名称/旅游目的地) */
    private String planName;

    /** 分类(家电:电视/冰箱/空调/洗衣机等; 旅游:国内/国外) */
    private String category;

    /** 预算金额 */
    private BigDecimal budget;

    /** 计划时间/出行日期 */
    private LocalDate planDate;

    /** 状态(PLANNED-计划中,COMPLETED-已完成,CANCELLED-已取消) */
    private String status;

    /** 优先级(HIGH-高,MEDIUM-中,LOW-低) */
    private String priority;

    /** 备注 */
    private String remark;
}
