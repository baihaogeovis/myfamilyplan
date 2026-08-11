package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.entity.GrowthPlan;
import com.familyplan.service.IGrowthPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 儿童成长计划控制器(短期/长期目标)
 */
@Slf4j
@RestController
@RequestMapping("/growth-plan")
public class GrowthPlanController {

    @Autowired
    private IGrowthPlanService growthPlanService;

    /**
     * 查询目标列表(短期/长期)
     */
    @GetMapping("/list")
    public Result<List<GrowthPlan>> list(@RequestParam Long memberId,
                                         @RequestParam(required = false) String goalType,
                                         @RequestParam(required = false) String status) {
        return Result.success(growthPlanService.listByType(memberId, goalType, status));
    }

    /**
     * 添加目标
     */
    @PostMapping
    public Result<Void> add(@RequestBody GrowthPlan plan) {
        growthPlanService.add(plan);
        return Result.success();
    }

    /**
     * 更新目标
     */
    @PutMapping
    public Result<Void> update(@RequestBody GrowthPlan plan) {
        growthPlanService.update(plan);
        return Result.success();
    }

    /**
     * 更新进度(0-100)
     */
    @PutMapping("/progress/{id}")
    public Result<Void> updateProgress(@PathVariable Long id, @RequestParam Integer progress) {
        growthPlanService.updateProgress(id, progress);
        return Result.success();
    }

    /**
     * 更新状态(ACHIEVED-达成/GIVEN_UP-放弃)
     */
    @PutMapping("/status/{id}")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        growthPlanService.changeStatus(id, status);
        return Result.success();
    }

    /**
     * 删除目标
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        growthPlanService.delete(id);
        return Result.success();
    }
}
