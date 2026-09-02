package com.familyplan.dto;

import lombok.Data;

/**
 * 记录良好行为赚取额度(父母操作即生效)
 */
@Data
public class RewardEarnRequest {

    /** 孩子成员ID */
    private Long memberId;

    /** 活动方案ID */
    private Long itemId;

    /** 次数(缺省 1) */
    private Integer quantity;

    /** 备注 */
    private String remark;
}
