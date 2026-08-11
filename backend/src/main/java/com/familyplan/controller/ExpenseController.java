package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.service.IExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 支出管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/expense")
public class ExpenseController {

    @Autowired
    private IExpenseService expenseService;

    /**
     * 分页查询支出记录
     */
    @GetMapping("/page")
    public Result<IPage<ExpenseRecord>> page(@RequestParam Long memberId,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(expenseService.pageQuery(memberId, page, size, category, type, startDate, endDate));
    }

    /**
     * 添加支出记录
     */
    @PostMapping
    public Result<Void> add(@RequestBody ExpenseRecord record) {
        expenseService.add(record);
        return Result.success();
    }

    /**
     * 更新支出记录
     */
    @PutMapping
    public Result<Void> update(@RequestBody ExpenseRecord record) {
        expenseService.update(record);
        return Result.success();
    }

    /**
     * 删除支出记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return Result.success();
    }
}
