package com.familyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familyplan.common.Result;
import com.familyplan.dto.DashboardOverview;
import com.familyplan.dto.RecentTransaction;
import com.familyplan.dto.TrendPoint;
import com.familyplan.entity.*;
import com.familyplan.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private IFamilyMemberService familyMemberService;
    @Autowired
    private IIncomeService incomeService;
    @Autowired
    private IExpenseService expenseService;
    @Autowired
    private IBudgetService budgetService;
    @Autowired
    private IApprovalService approvalService;

    private List<Long> memberIdsOf(Long userId) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getUserId, userId);
        return familyMemberService.list(wrapper).stream()
                .map(FamilyMember::getId).collect(Collectors.toList());
    }

    /**
     * 概览数据
     */
    @GetMapping("/overview")
    public Result<DashboardOverview> overview(@RequestParam Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate first = now.withDayOfMonth(1);
        LocalDate last = now.withDayOfMonth(now.lengthOfMonth());
        List<Long> memberIds = memberIdsOf(userId);

        DashboardOverview overview = new DashboardOverview();

        if (memberIds.isEmpty()) {
            overview.setMonthIncome(BigDecimal.ZERO);
            overview.setMonthExpense(BigDecimal.ZERO);
            overview.setBudgetRemaining(BigDecimal.ZERO);
            overview.setPendingCount(0L);
            overview.setMemberCount(0L);
            return Result.success(overview);
        }

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);
        BigDecimal monthIncome = incomes.stream().map(IncomeRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);
        BigDecimal monthExpense = expenses.stream().map(ExpenseRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<BudgetPlan> bw = new LambdaQueryWrapper<>();
        bw.in(BudgetPlan::getMemberId, memberIds).eq(BudgetPlan::getPeriodType, "MONTH")
                .eq(BudgetPlan::getYear, now.getYear()).eq(BudgetPlan::getMonth, now.getMonthValue());
        BigDecimal budgetTotal = budgetService.list(bw).stream().map(BudgetPlan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<ApprovalRecord> aw = new LambdaQueryWrapper<>();
        aw.in(ApprovalRecord::getApproverId, memberIds).eq(ApprovalRecord::getStatus, "PENDING");
        long pending = approvalService.count(aw);

        overview.setMonthIncome(monthIncome);
        overview.setMonthExpense(monthExpense);
        overview.setBudgetRemaining(budgetTotal.subtract(monthExpense));
        overview.setPendingCount(pending);
        overview.setMemberCount((long) memberIds.size());
        return Result.success(overview);
    }

    /**
     * 当月收支趋势
     */
    @GetMapping("/trend")
    public Result<List<TrendPoint>> trend(@RequestParam Long userId,
                                           @RequestParam(required = false) String month) {
        LocalDate now;
        if (month != null && !month.trim().isEmpty()) {
            String m = month.trim();
            // 支持 yyyy-MM 与 yyyy-MM-dd 两种格式
            now = m.length() > 7 ? LocalDate.parse(m) : YearMonth.parse(m).atDay(1);
        } else {
            now = LocalDate.now();
        }
        LocalDate first = now.withDayOfMonth(1);
        LocalDate last = now.withDayOfMonth(now.lengthOfMonth());
        List<Long> memberIds = memberIdsOf(userId);
        if (memberIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);

        List<TrendPoint> points = new ArrayList<>();
        for (int d = 1; d <= now.lengthOfMonth(); d++) {
            LocalDate day = now.withDayOfMonth(d);
            BigDecimal inc = incomes.stream().filter(r -> day.equals(r.getIncomeDate()))
                    .map(IncomeRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal exp = expenses.stream().filter(r -> day.equals(r.getExpenseDate()))
                    .map(ExpenseRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(new TrendPoint(day.toString(), inc, exp));
        }
        return Result.success(points);
    }

    /**
     * 最近流水
     */
    @GetMapping("/recent")
    public Result<List<RecentTransaction>> recent(@RequestParam Long userId,
                                                  @RequestParam(defaultValue = "10") Integer limit) {
        LocalDate now = LocalDate.now();
        LocalDate first = now.withDayOfMonth(1);
        LocalDate last = now.withDayOfMonth(now.lengthOfMonth());
        List<Long> memberIds = memberIdsOf(userId);
        if (memberIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);

        List<RecentTransaction> list = new ArrayList<>();
        incomes.forEach(r -> list.add(new RecentTransaction(r.getId(), "INCOME", r.getCategory(),
                r.getAmount(), r.getIncomeDate(), r.getRemark())));
        expenses.forEach(r -> list.add(new RecentTransaction(r.getId(), "EXPENSE", r.getCategory(),
                r.getAmount(), r.getExpenseDate(), r.getRemark())));
        list.sort(Comparator.comparing(RecentTransaction::getDate, Comparator.reverseOrder()));
        List<RecentTransaction> result = list.size() > limit ? list.subList(0, limit) : list;
        return Result.success(result);
    }
}
