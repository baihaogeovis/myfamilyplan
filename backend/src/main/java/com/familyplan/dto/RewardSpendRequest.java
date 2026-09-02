package com.familyplan.dto;

import lombok.Data;

/**
 * 提交额度消费申请(待父母确认后扣减)
 */
@Data
public class RewardSpendRequest {

    /** 孩子成员ID */
    private Long memberId;

    /** 活动方案ID */
    private Long itemId;

    /** 次数(按次方案使用, 缺省 1) */
    private Integer quantity;

    /** 时长分钟数(按时长方案使用, 必填) */
    private Integer minutes;

    /** 备注 */
    private String remark;
}
