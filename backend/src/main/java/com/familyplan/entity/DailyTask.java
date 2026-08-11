package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 每日任务实体(家庭/工作打勾工作台)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_task")
public class DailyTask extends BaseEntity {

    /** 成员ID */
    private Long memberId;

    /** 任务名称 */
    private String taskName;

    /** 类型(FAMILY-家庭,WORK-工作) */
    private String taskType;

    /** 任务日期 */
    private LocalDate taskDate;

    /** 是否完成(0-未完成,1-已完成) */
    private Integer completed;

    /** 优先级(HIGH-高,MEDIUM-中,LOW-低) */
    private String priority;

    /** 备注 */
    private String remark;
}
