package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.BudgetPlan;

/**
 * 预算规划服务接口
 */
public interface IBudgetService extends IService<BudgetPlan> {

    /**
     * 分页查询预算
     */
    IPage<BudgetPlan> pageQuery(Long memberId, Integer page, Integer size,
                                Integer year, Integer month, String period);

    /**
     * 添加预算
     */
    void add(BudgetPlan plan);

    /**
     * 更新预算
     */
    void update(BudgetPlan plan);

    /**
     * 删除预算
     */
    void delete(Long id);
}
