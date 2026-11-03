package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.dto.FundAllocationSummary;
import com.familyplan.entity.FundAllocation;
import com.familyplan.service.IFundAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 资金分配：按成员、按月或按年维护分类比例。
 */
@Slf4j
@RestController
@RequestMapping("/fund-allocation")
public class FundAllocationController {

    @Autowired
    private IFundAllocationService fundAllocationService;

    /**
     * 查询某一周期的分配及合计
     */
    @GetMapping("/list")
    public Result<FundAllocationSummary> list(@RequestParam Long memberId,
                                              @RequestParam(defaultValue = "MONTH") String period,
                                              @RequestParam(required = false) Integer year,
                                              @RequestParam(required = false) Integer month) {
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        Integer resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();
        return Result.success(fundAllocationService.summary(memberId, period, resolvedYear, resolvedMonth));
    }

    @PostMapping
    public Result<Void> add(@RequestBody FundAllocation item) {
        fundAllocationService.add(item);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody FundAllocation item) {
        fundAllocationService.update(item);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fundAllocationService.delete(id);
        return Result.success();
    }
}
