package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 奖励额度流水实体(唯一账本, 仅 CONFIRMED 参与余额与统计)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reward_txn")
public class RewardTxn extends BaseEntity {

    /** 所属家庭ID */
    private Long familyId;

    /** 成员ID(孩子) */
    private Long memberId;

    /** 活动方案ID(手动调整为空) */
    private Long itemId;

    /** 方案名称快照 */
    private String itemName;

    /** 方案分类快照(用于流水分类统计) */
    private String category;

    /** 方向(GRANT-周期发放,EARN-行为赚取,SPEND-消费扣减,EXPIRE-周期清零,ADJUST-手动调整) */
    private String direction;

    /** 状态(PENDING-待确认,CONFIRMED-已生效,REJECTED-已驳回) */
    private String status;

    /** 本次变动额度(带符号, 消费与清零为负) */
    private Integer points;

    /** 次数 */
    private Integer quantity;

    /** 时长分钟数(按时长方案) */
    private Integer minutes;

    /** 归属周期(如2026-W36 / 2026-08) */
    private String periodKey;

    /** 操作/确认的父母成员ID */
    private Long operatorMemberId;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 备注 */
    private String remark;
}
