package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.entity.LifePlan;
import com.familyplan.service.ILifePlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 美好生活计划控制器(家电购买/年度旅游)
 */
@Slf4j
@RestController
@RequestMapping("/life-plan")
public class LifePlanController {

    @Autowired
    private ILifePlanService lifePlanService;

    /**
     * 查询计划列表
     */
    @GetMapping("/list")
    public Result<List<LifePlan>> list(@RequestParam Long memberId,
                                       @RequestParam(required = false) String planType,
                                       @RequestParam(required = false) String status) {
        return Result.success(lifePlanService.listByType(memberId, planType, status));
    }

    /**
     * 添加计划
     */
    @PostMapping
    public Result<Void> add(@RequestBody LifePlan plan) {
        lifePlanService.add(plan);
        return Result.success();
    }

    /**
     * 更新计划
     */
    @PutMapping
    public Result<Void> update(@RequestBody LifePlan plan) {
        lifePlanService.update(plan);
        return Result.success();
    }

    /**
     * 更新计划状态(COMPLETED-完成/CANCELLED-取消)
     */
    @PutMapping("/status/{id}")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        lifePlanService.changeStatus(id, status);
        return Result.success();
    }

    /**
     * 删除计划
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        lifePlanService.delete(id);
        return Result.success();
    }
}
