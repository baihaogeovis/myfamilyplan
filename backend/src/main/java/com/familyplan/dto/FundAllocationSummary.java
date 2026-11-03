package com.familyplan.dto;

import com.familyplan.entity.FundAllocation;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 某一成员、某一周期的资金分配合计
 */
@Data
public class FundAllocationSummary {

    private List<FundAllocation> items;

    /** 已分配比例合计 */
    private BigDecimal totalPercentage;

    /** 距离 100% 还剩多少，超出时为 0 */
    private BigDecimal remainingPercentage;

    /** 合计是否正好 100% */
    private boolean complete;
}
