package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 儿童成长计划实体(短期/长期目标)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("growth_plan")
public class GrowthPlan extends BaseEntity {

    /** 成员ID(孩子) */
    private Long memberId;

    /** 目标标题 */
    private String title;

    /** 类型(SHORT-短期目标,LONG-长期目标) */
    private String goalType;

    /** 分类(学习/运动/艺术/习惯/社交/其他) */
    private String category;

    /** 目标描述 */
    private String description;

    /** 目标达成日期 */
    private LocalDate targetDate;

    /** 状态(IN_PROGRESS-进行中,ACHIEVED-已达成,GIVEN_UP-已放弃) */
    private String status;

    /** 完成进度(0-100) */
    private Integer progress;

    /** 备注 */
    private String remark;
}
