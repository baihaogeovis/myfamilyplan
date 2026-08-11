package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.BudgetPlan;
import com.familyplan.mapper.BudgetMapper;
import com.familyplan.service.IBudgetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 预算规划服务实现
 */
@Slf4j
@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, BudgetPlan> implements IBudgetService {

    @Override
    public IPage<BudgetPlan> pageQuery(Long memberId, Integer page, Integer size,
                                       Integer year, Integer month, String period) {
        LambdaQueryWrapper<BudgetPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetPlan::getMemberId, memberId);
        if (year != null) {
            wrapper.eq(BudgetPlan::getYear, year);
        }
        if (month != null) {
            wrapper.eq(BudgetPlan::getMonth, month);
        }
        if (period != null && !period.isEmpty()) {
            wrapper.eq(BudgetPlan::getPeriodType, period);
        }
        wrapper.orderByDesc(BudgetPlan::getYear).orderByDesc(BudgetPlan::getMonth);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void add(BudgetPlan plan) {
        baseMapper.insert(plan);
        log.info("添加预算成功: 金额={}", plan.getAmount());
    }

    @Override
    public void update(BudgetPlan plan) {
        baseMapper.updateById(plan);
        log.info("更新预算成功: ID={}", plan.getId());
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除预算成功: ID={}", id);
    }
}
