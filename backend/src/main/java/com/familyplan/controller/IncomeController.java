package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.IncomeRecord;
import com.familyplan.service.IIncomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 收入管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/income")
public class IncomeController {

    @Autowired
    private IIncomeService incomeService;

    /**
     * 分页查询收入记录
     */
    @GetMapping("/page")
    public Result<IPage<IncomeRecord>> page(@RequestParam Long memberId,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(incomeService.pageQuery(memberId, page, size, category, startDate, endDate));
    }

    /**
     * 添加收入记录
     */
    @PostMapping
    public Result<Void> add(@RequestBody IncomeRecord record) {
        incomeService.add(record);
        return Result.success();
    }

    /**
     * 更新收入记录
     */
    @PutMapping
    public Result<Void> update(@RequestBody IncomeRecord record) {
        incomeService.update(record);
        return Result.success();
    }

    /**
     * 删除收入记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        incomeService.delete(id);
        return Result.success();
    }
}
