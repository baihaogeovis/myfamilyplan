package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.LifePlan;
import com.familyplan.mapper.LifePlanMapper;
import com.familyplan.service.ILifePlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 美好生活计划服务实现
 */
@Slf4j
@Service
public class LifePlanServiceImpl extends ServiceImpl<LifePlanMapper, LifePlan> implements ILifePlanService {

    @Override
    public List<LifePlan> listByType(Long memberId, String planType, String status) {
        LambdaQueryWrapper<LifePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifePlan::getMemberId, memberId);
        if (planType != null && !planType.isEmpty()) {
            wrapper.eq(LifePlan::getPlanType, planType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(LifePlan::getStatus, status);
        }
        wrapper.orderByDesc(LifePlan::getStatus)
                .orderByAsc(LifePlan::getPlanDate)
                .orderByDesc(LifePlan::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void add(LifePlan plan) {
        if (plan.getStatus() == null || plan.getStatus().isEmpty()) {
            plan.setStatus("PLANNED");
        }
        if (plan.getPriority() == null || plan.getPriority().isEmpty()) {
            plan.setPriority("MEDIUM");
        }
        baseMapper.insert(plan);
        log.info("添加生活计划成功: {}", plan.getPlanName());
    }

    @Override
    public void update(LifePlan plan) {
        baseMapper.updateById(plan);
        log.info("更新生活计划成功: ID={}", plan.getId());
    }

    @Override
    public void changeStatus(Long id, String status) {
        LifePlan plan = new LifePlan();
        plan.setId(id);
        plan.setStatus(status);
        baseMapper.updateById(plan);
        log.info("生活计划状态更新成功: ID={}, status={}", id, status);
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除生活计划成功: ID={}", id);
    }
}
