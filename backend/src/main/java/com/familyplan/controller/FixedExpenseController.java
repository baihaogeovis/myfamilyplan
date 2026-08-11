package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.dto.FinanceOverview;
import com.familyplan.entity.FixedExpense;
import com.familyplan.service.IFixedExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 固定支出控制器(物业费/车位费/话费网费等 + 资金总览)
 */
@Slf4j
@RestController
@RequestMapping("/fixed-expense")
public class FixedExpenseController {

    @Autowired
    private IFixedExpenseService fixedExpenseService;

    /**
     * 查询固定支出配置列表
     */
    @GetMapping("/list")
    public Result<List<FixedExpense>> list(@RequestParam Long memberId) {
        return Result.success(fixedExpenseService.listAll(memberId));
    }

    /**
     * 资金总览统计(存了多少/花了多少/固定支出)
     */
    @GetMapping("/overview")
    public Result<FinanceOverview> overview(@RequestParam Long memberId,
                                            @RequestParam Integer year,
                                            @RequestParam Integer month) {
        return Result.success(fixedExpenseService.overview(memberId, year, month));
    }

    /**
     * 添加固定支出
     */
    @PostMapping
    public Result<Void> add(@RequestBody FixedExpense expense) {
        fixedExpenseService.add(expense);
        return Result.success();
    }

    /**
     * 更新固定支出
     */
    @PutMapping
    public Result<Void> update(@RequestBody FixedExpense expense) {
        fixedExpenseService.update(expense);
        return Result.success();
    }

    /**
     * 删除固定支出
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fixedExpenseService.delete(id);
        return Result.success();
    }
}
