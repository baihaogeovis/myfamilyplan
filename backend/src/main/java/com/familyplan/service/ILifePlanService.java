package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.LifePlan;

import java.util.List;

/**
 * 美好生活计划服务接口
 */
public interface ILifePlanService extends IService<LifePlan> {

    /**
     * 查询计划列表
     */
    List<LifePlan> listByType(Long memberId, String planType, String status);

    /**
     * 添加计划
     */
    void add(LifePlan plan);

    /**
     * 更新计划
     */
    void update(LifePlan plan);

    /**
     * 更新状态(完成/取消)
     */
    void changeStatus(Long id, String status);

    /**
     * 删除计划
     */
    void delete(Long id);
}
