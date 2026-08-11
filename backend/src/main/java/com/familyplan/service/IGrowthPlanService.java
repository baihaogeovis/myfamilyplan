package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.GrowthPlan;

import java.util.List;

/**
 * 儿童成长计划服务接口
 */
public interface IGrowthPlanService extends IService<GrowthPlan> {

    /**
     * 查询目标列表(短期/长期)
     */
    List<GrowthPlan> listByType(Long memberId, String goalType, String status);

    /**
     * 添加目标
     */
    void add(GrowthPlan plan);

    /**
     * 更新目标
     */
    void update(GrowthPlan plan);

    /**
     * 更新进度(0-100)
     */
    void updateProgress(Long id, Integer progress);

    /**
     * 更新状态(达成/放弃)
     */
    void changeStatus(Long id, String status);

    /**
     * 删除目标
     */
    void delete(Long id);
}
