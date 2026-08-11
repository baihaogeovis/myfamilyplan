package com.familyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familyplan.common.Result;
import com.familyplan.dto.CategoryItem;
import com.familyplan.dto.CategoryStatResponse;
import com.familyplan.dto.MonthlyStat;
import com.familyplan.entity.*;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IIncomeService;
import com.familyplan.service.IExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
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

    /**
     * 当前家庭下的全部成员ID(统计展示整个家庭的数据)
     */
    private List<Long> memberIdsOfFamily() {
        Long familyId = com.familyplan.security.SecurityUtil.getRequiredFamilyId();
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
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        List<Long> memberIds = memberIdsOfFamily();

        CategoryStatResponse response = new CategoryStatResponse();
        if (memberIds.isEmpty()) {
            response.setIncome(Collections.emptyList());
            response.setExpense(Collections.emptyList());
            return Result.success(response);
        }

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        Map<String, BigDecimal> incomeMap = incomeService.list(iw).stream()
                .collect(Collectors.groupingBy(IncomeRecord::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, IncomeRecord::getAmount, BigDecimal::add)));

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        Map<String, BigDecimal> expenseMap = expenseService.list(ew).stream()
                .collect(Collectors.groupingBy(ExpenseRecord::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseRecord::getAmount, BigDecimal::add)));

        List<CategoryItem> income = incomeMap.entrySet().stream()
                .map(e -> new CategoryItem(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
        List<CategoryItem> expense = expenseMap.entrySet().stream()
                .map(e -> new CategoryItem(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());

        response.setIncome(income);
        response.setExpense(expense);
        return Result.success(response);
    }

    /**
     * 年度月度趋势：当年每月的收入/支出合计
     */
    @GetMapping("/monthly")
    public Result<List<MonthlyStat>> monthly(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        LocalDate first = LocalDate.of(y, 1, 1);
        LocalDate last = LocalDate.of(y, 12, 31);
        List<Long> memberIds = memberIdsOfFamily();

        List<MonthlyStat> stats = new ArrayList<>();
        if (memberIds.isEmpty()) {
            for (int m = 1; m <= 12; m++) {
                stats.add(new MonthlyStat(m, BigDecimal.ZERO, BigDecimal.ZERO));
            }
            return Result.success(stats);
        }

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        Map<Integer, BigDecimal> incomeMap = incomeService.list(iw).stream()
                .collect(Collectors.groupingBy(r -> r.getIncomeDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO, IncomeRecord::getAmount, BigDecimal::add)));

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        Map<Integer, BigDecimal> expenseMap = expenseService.list(ew).stream()
                .collect(Collectors.groupingBy(r -> r.getExpenseDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO, ExpenseRecord::getAmount, BigDecimal::add)));

        for (int m = 1; m <= 12; m++) {
            stats.add(new MonthlyStat(m, incomeMap.get(m), expenseMap.get(m)));
        }
        return Result.success(stats);
    }
}
