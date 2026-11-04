package com.familyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familyplan.common.Result;
import com.familyplan.dto.DashboardOverview;
import com.familyplan.dto.RecentTransaction;
import com.familyplan.dto.TrendPoint;
import com.familyplan.entity.ApprovalRecord;
import com.familyplan.entity.BudgetPlan;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.entity.FamilyMember;
import com.familyplan.entity.IncomeRecord;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IApprovalService;
import com.familyplan.service.IBudgetService;
import com.familyplan.service.IExpenseService;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IIncomeService;
import com.familyplan.support.FamilySummaryCache;
import com.familyplan.support.MoneyRollup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘数据控制器。
 * 汇总结果按家庭短时缓存，收支写入后整户失效。
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
    @Autowired
    private FamilySummaryCache summaryCache;

    /**
     * 当前家庭下的全部成员ID(仪表盘展示整个家庭的数据)
     */
    private List<Long> memberIdsOfFamily() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getFamilyId, familyId);
        return familyMemberService.list(wrapper).stream()
                .map(FamilyMember::getId).collect(Collectors.toList());
    }

    /**
     * 概览数据
     */
    @GetMapping("/overview")
    public Result<DashboardOverview> overview() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        YearMonth month = YearMonth.now();
        DashboardOverview overview = summaryCache.get(familyId, "overview:" + month,
                () -> computeOverview(month));
        return Result.success(overview);
    }

    /**
     * 当月收支趋势
     */
    @GetMapping("/trend")
    public Result<List<TrendPoint>> trend(@RequestParam(required = false) String month) {
        YearMonth ym = resolveMonth(month);
        Long familyId = SecurityUtil.getRequiredFamilyId();
        List<TrendPoint> points = summaryCache.get(familyId, "trend:" + ym, () -> computeTrend(ym));
        return Result.success(points);
    }

    /**
     * 最近流水
     */
    @GetMapping("/recent")
    public Result<List<RecentTransaction>> recent(@RequestParam(defaultValue = "10") Integer limit) {
        int size = limit == null ? 10 : limit;
        Long familyId = SecurityUtil.getRequiredFamilyId();
        YearMonth month = YearMonth.now();
        List<RecentTransaction> result = summaryCache.get(familyId, "recent:" + month + ":" + size,
                () -> computeRecent(month, size));
        return Result.success(result);
    }

    private DashboardOverview computeOverview(YearMonth month) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        List<Long> memberIds = memberIdsOfFamily();
        if (memberIds.isEmpty()) {
            return MoneyRollup.overview(List.of(), List.of(), List.of(), 0L, 0L);
        }

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);

        LambdaQueryWrapper<BudgetPlan> bw = new LambdaQueryWrapper<>();
        bw.in(BudgetPlan::getMemberId, memberIds).eq(BudgetPlan::getPeriodType, "MONTH")
                .eq(BudgetPlan::getYear, month.getYear()).eq(BudgetPlan::getMonth, month.getMonthValue());
        List<BudgetPlan> budgets = budgetService.list(bw);

        LambdaQueryWrapper<ApprovalRecord> aw = new LambdaQueryWrapper<>();
        aw.in(ApprovalRecord::getApproverId, memberIds).eq(ApprovalRecord::getStatus, "PENDING");
        long pending = approvalService.count(aw);

        return MoneyRollup.overview(incomes, expenses, budgets, pending, memberIds.size());
    }

    private List<TrendPoint> computeTrend(YearMonth month) {
        List<Long> memberIds = memberIdsOfFamily();
        if (memberIds.isEmpty()) {
            return List.of();
        }
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);
        return MoneyRollup.dailyTrend(month, incomes, expenses);
    }

    private List<RecentTransaction> computeRecent(YearMonth month, int limit) {
        List<Long> memberIds = memberIdsOfFamily();
        if (memberIds.isEmpty()) {
            return List.of();
        }
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        LambdaQueryWrapper<IncomeRecord> iw = new LambdaQueryWrapper<>();
        iw.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        List<IncomeRecord> incomes = incomeService.list(iw);

        LambdaQueryWrapper<ExpenseRecord> ew = new LambdaQueryWrapper<>();
        ew.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        List<ExpenseRecord> expenses = expenseService.list(ew);
        return MoneyRollup.recent(incomes, expenses, limit);
    }

    private YearMonth resolveMonth(String month) {
        if (month == null || month.trim().isEmpty()) {
            return YearMonth.now();
        }
        String value = month.trim();
        // 支持 yyyy-MM 与 yyyy-MM-dd 两种格式
        return value.length() > 7 ? YearMonth.from(LocalDate.parse(value)) : YearMonth.parse(value);
    }
}
