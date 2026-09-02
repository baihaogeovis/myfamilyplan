package com.familyplan.dto;

import lombok.Data;

import java.util.List;

/**
 * 奖励额度概览(余额 + 本周期收支 + 消费分类构成 + 待确认笔数)
 */
@Data
public class RewardSummary {

    /** 成员ID */
    private Long memberId;

    /** 成员姓名 */
    private String memberName;

    /** 当前可用额度 */
    private Integer balance;

    /** 每周期发放额度 */
    private Integer weeklyQuota;

    /** 发放周期(WEEK/MONTH) */
    private String periodType;

    /** 是否跨周期结转(0-清零,1-累积) */
    private Integer carryOver;

    /** 统计周期标识 */
    private String periodKey;

    /** 本周期已发放 */
    private Integer granted;

    /** 本周期已赚取 */
    private Integer earned;

    /** 本周期已花(正数) */
    private Integer spent;

    /** 本周期过期清零(正数) */
    private Integer expired;

    /** 本周期手动调整净值 */
    private Integer adjusted;

    /** 待确认笔数 */
    private Long pendingCount;

    /** 本周期消费按分类构成 */
    private List<RewardCategoryItem> spendByCategory;
}
