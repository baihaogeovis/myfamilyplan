package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.BudgetPlan;
import com.familyplan.service.IBudgetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 预算规划控制器
 */
@Slf4j
@RestController
@RequestMapping("/budget")
public class BudgetController {

    @Autowired
    private IBudgetService budgetService;

    /**
     * 分页查询预算
     */
    @GetMapping("/page")
    public Result<IPage<BudgetPlan>> page(@RequestParam Long memberId,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) Integer month,
                                          @RequestParam(required = false) String periodType) {
        return Result.success(budgetService.pageQuery(memberId, page, size, year, month, periodType));
    }

    /**
     * 添加预算
     */
    @PostMapping
    public Result<Void> add(@RequestBody BudgetPlan plan) {
        budgetService.add(plan);
        return Result.success();
    }

    /**
     * 更新预算
     */
    @PutMapping
    public Result<Void> update(@RequestBody BudgetPlan plan) {
        budgetService.update(plan);
        return Result.success();
    }

    /**
     * 删除预算
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return Result.success();
    }
}
