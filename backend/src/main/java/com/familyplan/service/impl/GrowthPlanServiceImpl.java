package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.GrowthPlan;
import com.familyplan.mapper.GrowthPlanMapper;
import com.familyplan.service.IGrowthPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 儿童成长计划服务实现
 */
@Slf4j
@Service
public class GrowthPlanServiceImpl extends ServiceImpl<GrowthPlanMapper, GrowthPlan> implements IGrowthPlanService {

    @Override
    public List<GrowthPlan> listByType(Long memberId, String goalType, String status) {
        LambdaQueryWrapper<GrowthPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthPlan::getMemberId, memberId);
        if (goalType != null && !goalType.isEmpty()) {
            wrapper.eq(GrowthPlan::getGoalType, goalType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GrowthPlan::getStatus, status);
        }
        wrapper.orderByDesc(GrowthPlan::getStatus)
                .orderByAsc(GrowthPlan::getTargetDate)
                .orderByDesc(GrowthPlan::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void add(GrowthPlan plan) {
        if (plan.getStatus() == null || plan.getStatus().isEmpty()) {
            plan.setStatus("IN_PROGRESS");
        }
        if (plan.getProgress() == null) {
            plan.setProgress(0);
        }
        baseMapper.insert(plan);
        log.info("添加成长目标成功: {}", plan.getTitle());
    }

    @Override
    public void update(GrowthPlan plan) {
        baseMapper.updateById(plan);
        log.info("更新成长目标成功: ID={}", plan.getId());
    }

    @Override
    public void updateProgress(Long id, Integer progress) {
        if (progress == null || progress < 0) {
            progress = 0;
        }
        if (progress > 100) {
            progress = 100;
        }
        GrowthPlan plan = new GrowthPlan();
        plan.setId(id);
        plan.setProgress(progress);
        baseMapper.updateById(plan);
        log.info("成长目标进度更新成功: ID={}, progress={}", id, progress);
    }

    @Override
    public void changeStatus(Long id, String status) {
        GrowthPlan plan = new GrowthPlan();
        plan.setId(id);
        plan.setStatus(status);
        baseMapper.updateById(plan);
        log.info("成长目标状态更新成功: ID={}, status={}", id, status);
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除成长目标成功: ID={}", id);
    }
}
