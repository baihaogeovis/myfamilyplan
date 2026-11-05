package com.familyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familyplan.common.Result;
import com.familyplan.dto.CategoryStatResponse;
import com.familyplan.dto.MonthlyStat;
import com.familyplan.entity.*;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IIncomeService;
import com.familyplan.service.IExpenseService;
import com.familyplan.support.FamilySummaryCache;
import com.familyplan.support.MoneyRollup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统计报表控制器
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private IFamilyMemberService familyMemberService;
    @Autowired
    private IIncomeService incomeService;
    @Autowired
    private IExpenseService expenseService;
    @Autowired
    private FamilySummaryCache summaryCache;

    /**
     * 当前家庭下的全部成员ID(统计展示整个家庭的数据)
     */
    private List<Long> memberIdsOfFamily() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getFamilyId, familyId);
        return familyMemberService.list(wrapper).stream()
                .map(FamilyMember::getId).collect(Collectors.toList());
    }

    /**
     * 分类统计：当月收入/支出按分类汇总
     */
    @GetMapping("/category")
    public Result<CategoryStatResponse> category(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth ym = month != null ? month : YearMonth.now();
        Long familyId = SecurityUtil.getRequiredFamilyId();
        CategoryStatResponse response = summaryCache.get(familyId, "category:" + ym, () -> computeCategory(ym));
        return Result.success(response);
    }

    /**
     * 年度月度趋势：当年每月的收入/支出合计
     */
    @GetMapping("/monthly")
    public Result<List<MonthlyStat>> monthly(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        Long familyId = SecurityUtil.getRequiredFamilyId();
        List<MonthlyStat> stats = summaryCache.get(familyId, "monthly:" + y, () -> computeMonthly(y));
        return Result.success(stats);
    }

    private CategoryStatResponse computeCategory(YearMonth ym) {
        List<Long> memberIds = memberIdsOfFamily();
        if (memberIds.isEmpty()) {
            return MoneyRollup.byCategory(List.of(), List.of());
        }
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);
        return MoneyRollup.byCategory(incomes, expenses);
    }

    private List<MonthlyStat> computeMonthly(int year) {
        List<Long> memberIds = memberIdsOfFamily();
        if (memberIds.isEmpty()) {
            return MoneyRollup.byMonth(year, List.of(), List.of());
        }
        LocalDate first = LocalDate.of(year, 1, 1);
        LocalDate last = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);
        return MoneyRollup.byMonth(year, incomes, expenses);
    }
}
