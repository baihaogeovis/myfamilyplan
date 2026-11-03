package com.familyplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familyplan.common.BusinessException;
import com.familyplan.dto.CategoryItem;
import com.familyplan.dto.MonthlyStat;
import com.familyplan.entity.BudgetPlan;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.entity.FamilyMember;
import com.familyplan.entity.IncomeRecord;
import com.familyplan.security.SecurityUtil;
import com.familyplan.support.ExcelWorkbooks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按当前筛选条件导出 Excel，不改动原有查询接口。
 */
@Slf4j
@Service
public class ExcelExportService {

    @Autowired
    private IIncomeService incomeService;

    @Autowired
    private IExpenseService expenseService;

    @Autowired
    private IBudgetService budgetService;

    @Autowired
    private IFamilyMemberService familyMemberService;

    public byte[] exportIncome(Long memberId, String category, LocalDate startDate, LocalDate endDate) {
        checkMember(memberId);
        LambdaQueryWrapper<IncomeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IncomeRecord::getMemberId, memberId);
        if (category != null && !category.isBlank()) {
            wrapper.eq(IncomeRecord::getCategory, category);
        }
        if (startDate != null) {
            wrapper.ge(IncomeRecord::getIncomeDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(IncomeRecord::getIncomeDate, endDate);
        }
        wrapper.orderByDesc(IncomeRecord::getIncomeDate);
        wrapper.last("LIMIT " + (ExcelWorkbooks.MAX_ROWS + 1));
        List<IncomeRecord> rows = incomeService.list(wrapper);
        boolean truncated = trim(rows);
        log.info("导出收入 Excel: memberId={}, rows={}", memberId, rows.size());
        return ExcelWorkbooks.income(rows, truncated);
    }

    public byte[] exportExpense(Long memberId, String category, String type,
                                LocalDate startDate, LocalDate endDate) {
        checkMember(memberId);
        LambdaQueryWrapper<ExpenseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecord::getMemberId, memberId);
        if (category != null && !category.isBlank()) {
            wrapper.eq(ExpenseRecord::getCategory, category);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(ExpenseRecord::getType, type);
        }
        if (startDate != null) {
            wrapper.ge(ExpenseRecord::getExpenseDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ExpenseRecord::getExpenseDate, endDate);
        }
        wrapper.orderByDesc(ExpenseRecord::getExpenseDate);
        wrapper.last("LIMIT " + (ExcelWorkbooks.MAX_ROWS + 1));
        List<ExpenseRecord> rows = expenseService.list(wrapper);
        boolean truncated = trim(rows);
        log.info("导出支出 Excel: memberId={}, rows={}", memberId, rows.size());
        return ExcelWorkbooks.expense(rows, truncated);
    }

    public byte[] exportBudget(Long memberId, Integer year, Integer month, String periodType) {
        checkMember(memberId);
        LambdaQueryWrapper<BudgetPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetPlan::getMemberId, memberId);
        if (year != null) {
            wrapper.eq(BudgetPlan::getYear, year);
        }
        if (month != null) {
            wrapper.eq(BudgetPlan::getMonth, month);
        }
        if (periodType != null && !periodType.isBlank()) {
            wrapper.eq(BudgetPlan::getPeriodType, periodType);
        }
        wrapper.orderByDesc(BudgetPlan::getYear).orderByDesc(BudgetPlan::getMonth);
        wrapper.last("LIMIT " + (ExcelWorkbooks.MAX_ROWS + 1));
        List<BudgetPlan> rows = budgetService.list(wrapper);
        boolean truncated = trim(rows);
        log.info("导出预算 Excel: memberId={}, rows={}", memberId, rows.size());
        return ExcelWorkbooks.budget(rows, truncated);
    }

    public byte[] exportStatistics(Integer year, YearMonth month) {
        int y = year != null ? year : LocalDate.now().getYear();
        YearMonth ym = month != null ? month : YearMonth.now();
        List<Long> memberIds = memberIdsOfFamily();
        List<MonthlyStat> monthly = monthly(y, memberIds);
        List<CategoryItem> income = categoryIncome(ym, memberIds);
        List<CategoryItem> expense = categoryExpense(ym, memberIds);
        log.info("导出统计 Excel: year={}, month={}", y, ym);
        return ExcelWorkbooks.statistics(y, ym.toString(), monthly, income, expense);
    }

    private void checkMember(Long memberId) {
        if (memberId == null) {
            throw new BusinessException("请选择家庭成员");
        }
        familyMemberService.checkMemberBelongsToFamily(SecurityUtil.getRequiredFamilyId(), memberId);
    }

    private List<Long> memberIdsOfFamily() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getFamilyId, familyId);
        return familyMemberService.list(wrapper).stream().map(FamilyMember::getId).collect(Collectors.toList());
    }

    private List<MonthlyStat> monthly(int year, List<Long> memberIds) {
        List<MonthlyStat> stats = new ArrayList<>();
        if (memberIds.isEmpty()) {
            for (int m = 1; m <= 12; m++) {
                stats.add(new MonthlyStat(m, BigDecimal.ZERO, BigDecimal.ZERO));
            }
            return stats;
        }
        LocalDate first = LocalDate.of(year, 1, 1);
        LocalDate last = LocalDate.of(year, 12, 31);
        LambdaQueryWrapper<IncomeRecord> incomeWrapper = new LambdaQueryWrapper<>();
        incomeWrapper.in(IncomeRecord::getMemberId, memberIds).between(IncomeRecord::getIncomeDate, first, last);
        Map<Integer, BigDecimal> incomeMap = incomeService.list(incomeWrapper).stream()
                .filter(row -> row.getIncomeDate() != null)
                .collect(Collectors.groupingBy(row -> row.getIncomeDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO,
                                row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount(),
                                BigDecimal::add)));
        LambdaQueryWrapper<ExpenseRecord> expenseWrapper = new LambdaQueryWrapper<>();
        expenseWrapper.in(ExpenseRecord::getMemberId, memberIds).between(ExpenseRecord::getExpenseDate, first, last);
        Map<Integer, BigDecimal> expenseMap = expenseService.list(expenseWrapper).stream()
                .filter(row -> row.getExpenseDate() != null)
                .collect(Collectors.groupingBy(row -> row.getExpenseDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO,
                                row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount(),
                                BigDecimal::add)));
        for (int m = 1; m <= 12; m++) {
            stats.add(new MonthlyStat(m, incomeMap.get(m), expenseMap.get(m)));
        }
        return stats;
    }

    private List<CategoryItem> categoryIncome(YearMonth ym, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<IncomeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(IncomeRecord::getMemberId, memberIds)
                .between(IncomeRecord::getIncomeDate, ym.atDay(1), ym.atEndOfMonth());
        Map<String, BigDecimal> grouped = incomeService.list(wrapper).stream()
                .collect(Collectors.groupingBy(row -> row.getCategory() == null ? "" : row.getCategory(),
                        Collectors.reducing(BigDecimal.ZERO,
                                row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount(),
                                BigDecimal::add)));
        return grouped.entrySet().stream()
                .map(entry -> new CategoryItem(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    private List<CategoryItem> categoryExpense(YearMonth ym, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ExpenseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ExpenseRecord::getMemberId, memberIds)
                .between(ExpenseRecord::getExpenseDate, ym.atDay(1), ym.atEndOfMonth());
        Map<String, BigDecimal> grouped = expenseService.list(wrapper).stream()
                .collect(Collectors.groupingBy(row -> row.getCategory() == null ? "" : row.getCategory(),
                        Collectors.reducing(BigDecimal.ZERO,
                                row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount(),
                                BigDecimal::add)));
        return grouped.entrySet().stream()
                .map(entry -> new CategoryItem(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    private boolean trim(List<?> rows) {
        if (rows.size() > ExcelWorkbooks.MAX_ROWS) {
            rows.subList(ExcelWorkbooks.MAX_ROWS, rows.size()).clear();
            return true;
        }
        return false;
    }
}
