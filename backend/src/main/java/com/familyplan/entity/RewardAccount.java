package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 奖励额度账户实体(一个孩子一条, balance 为余额唯一真相)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reward_account")
public class RewardAccount extends BaseEntity {

    /** 所属家庭ID */
    private Long familyId;

    /** 成员ID(孩子) */
    private Long memberId;

    /** 当前可用额度 */
    private Integer balance;

    /** 每周期发放额度(0-不自动发放) */
    private Integer weeklyQuota;

    /** 发放周期(WEEK-周,MONTH-月) */
    private String periodType;

    /** 余额是否跨周期结转(0-清零,1-累积) */
    private Integer carryOver;

    /** 备注 */
    private String remark;

    /** 状态(0-禁用,1-启用) */
    private Integer status;
}
