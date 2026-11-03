package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.dto.FundAllocationSummary;
import com.familyplan.entity.FundAllocation;

/**
 * 资金分配服务
 */
public interface IFundAllocationService extends IService<FundAllocation> {

    /**
     * 查询某一成员某一周期的分配，并给出合计
     */
    FundAllocationSummary summary(Long memberId, String period, Integer year, Integer month);

    void add(FundAllocation item);

    void update(FundAllocation item);

    void delete(Long id);
}
