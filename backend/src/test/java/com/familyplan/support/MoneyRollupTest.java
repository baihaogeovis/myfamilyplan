package com.familyplan.support;

import com.familyplan.dto.CategoryItem;
import com.familyplan.dto.CategoryStatResponse;
import com.familyplan.dto.DashboardOverview;
import com.familyplan.dto.MonthlyStat;
import com.familyplan.dto.RecentTransaction;
import com.familyplan.dto.TrendPoint;
import com.familyplan.entity.BudgetPlan;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.entity.FixedExpense;
import com.familyplan.entity.IncomeRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyRollupTest {

    @Test
    void sumsMonthIncomeExpenseAndBudgetRemaining() {
        IncomeRecord salary = income("SALARY", "12000.50", LocalDate.of(2026, 11, 1));
        IncomeRecord blank = income("BONUS", null, LocalDate.of(2026, 11, 2));
        ExpenseRecord food = expense("FOOD", "800.25", LocalDate.of(2026, 11, 3));
        ExpenseRecord rent = expense("RENT", "4500.00", LocalDate.of(2026, 11, 4));
        BudgetPlan budget = budget("6000.00");
        BudgetPlan missing = budget(null);

        DashboardOverview overview = MoneyRollup.overview(
                List.of(salary, blank), List.of(food, rent), List.of(budget, missing), 2L, 3L);

        assertMoney("12000.50", overview.getMonthIncome());
        assertMoney("5300.25", overview.getMonthExpense());
        assertMoney("699.75", overview.getBudgetRemaining());
        assertEquals(2L, overview.getPendingCount());
        assertEquals(3L, overview.getMemberCount());
    }

    @Test
    void budgetRemainingGoesNegativeWhenSpendingExceedsThePlan() {
        DashboardOverview overview = MoneyRollup.overview(
                List.of(), List.of(expense("FOOD", "80.00", LocalDate.of(2026, 11, 1))),
                List.of(budget("50.00")), 0L, 1L);
        assertMoney("-30.00", overview.getBudgetRemaining());
    }

    @Test
    void emptyBooksStayAtZero() {
        DashboardOverview overview = MoneyRollup.overview(null, null, null, 0L, 0L);
        assertMoney("0", overview.getMonthIncome());
        assertMoney("0", overview.getMonthExpense());
        assertMoney("0", overview.getBudgetRemaining());

        List<TrendPoint> trend = MoneyRollup.dailyTrend(YearMonth.of(2026, 2), null, null);
        assertEquals(28, trend.size());
        assertEquals("2026-02-01", trend.get(0).getDate());
        assertMoney("0", trend.get(0).getIncome());
        assertEquals(12, MoneyRollup.byMonth(2026, null, null).size());
        assertTrue(MoneyRollup.recent(null, null, 10).isEmpty());
    }

    @Test
    void dailyTrendBucketsOnlyTheRequestedMonth() {
        List<IncomeRecord> incomes = List.of(
                income("SALARY", "100.00", LocalDate.of(2026, 11, 1)),
                income("SALARY", "20.00", LocalDate.of(2026, 11, 1)),
                income("SALARY", "7.00", LocalDate.of(2026, 10, 31)),
                income("SALARY", null, null));
        List<ExpenseRecord> expenses = List.of(
                expense("FOOD", "5.50", LocalDate.of(2026, 11, 30)),
                expense("FOOD", "9.00", LocalDate.of(2026, 12, 1)));

        List<TrendPoint> trend = MoneyRollup.dailyTrend(YearMonth.of(2026, 11), incomes, expenses);

        assertEquals(30, trend.size());
        assertMoney("120.00", trend.get(0).getIncome());
        assertMoney("0", trend.get(0).getExpense());
        assertMoney("5.50", trend.get(29).getExpense());
        assertMoney("0", trend.get(14).getIncome());
    }

    @Test
    void recentOrdersNewestFirstAndKeepsUndatedRowsLast() {
        IncomeRecord older = income("SALARY", "10", LocalDate.of(2026, 11, 1));
        older.setId(1L);
        ExpenseRecord newer = expense("FOOD", "4", LocalDate.of(2026, 11, 3));
        newer.setId(2L);
        IncomeRecord undated = income("OTHER", "1", null);
        undated.setId(3L);

        List<RecentTransaction> rows = MoneyRollup.recent(List.of(older, undated), List.of(newer), 2);

        assertEquals(2, rows.size());
        assertEquals("EXPENSE", rows.get(0).getType());
        assertEquals(LocalDate.of(2026, 11, 3), rows.get(0).getDate());
        assertEquals("INCOME", rows.get(1).getType());
        assertEquals(List.of(), MoneyRollup.recent(List.of(older), List.of(), 0));
    }

    @Test
    void categoryAndMonthRollupsIgnoreBlankAndForeignRows() {
        CategoryStatResponse categories = MoneyRollup.byCategory(
                List.of(income("SALARY", "10", LocalDate.of(2026, 1, 1)),
                        income("  ", "3", LocalDate.of(2026, 1, 2)),
                        income(null, "2", LocalDate.of(2026, 1, 3))),
                List.of(expense("FOOD", "8", LocalDate.of(2026, 1, 4)),
                        expense("FOOD", "1", LocalDate.of(2026, 1, 5)),
                        expense("RENT", "8", LocalDate.of(2026, 1, 6))));

        assertEquals(List.of("SALARY", "未分类"), categories.getIncome().stream().map(CategoryItem::getCategory).toList());
        assertMoney("5", categories.getIncome().get(1).getAmount());
        assertEquals("FOOD", categories.getExpense().get(0).getCategory());
        assertEquals("RENT", categories.getExpense().get(1).getCategory());

        List<MonthlyStat> months = MoneyRollup.byMonth(2026, List.of(
                income("SALARY", "10", LocalDate.of(2026, 3, 1)),
                income("SALARY", "99", LocalDate.of(2025, 3, 1)),
                income("SALARY", "1", null)), List.of(
                expense("FOOD", "4", LocalDate.of(2026, 3, 2))));

        assertEquals(12, months.size());
        assertMoney("10", months.get(2).getIncome());
        assertMoney("4", months.get(2).getExpense());
        assertMoney("0", months.get(0).getIncome());
    }

    @Test
    void activeFixedTotalSkipsDisabledAndBlankAmounts() {
        FixedExpense rent = fixed(1, "2000");
        FixedExpense paused = fixed(0, "300");
        FixedExpense blank = fixed(1, null);
        assertMoney("2000", MoneyRollup.activeFixedTotal(List.of(rent, paused, blank)));
        assertMoney("0", MoneyRollup.activeFixedTotal(null));
    }

    private static IncomeRecord income(String category, String amount, LocalDate date) {
        IncomeRecord record = new IncomeRecord();
        record.setCategory(category);
        record.setAmount(amount == null ? null : new BigDecimal(amount));
        record.setIncomeDate(date);
        return record;
    }

    private static ExpenseRecord expense(String category, String amount, LocalDate date) {
        ExpenseRecord record = new ExpenseRecord();
        record.setCategory(category);
        record.setAmount(amount == null ? null : new BigDecimal(amount));
        record.setExpenseDate(date);
        return record;
    }

    private static BudgetPlan budget(String amount) {
        BudgetPlan plan = new BudgetPlan();
        plan.setAmount(amount == null ? null : new BigDecimal(amount));
        return plan;
    }

    private static FixedExpense fixed(int active, String amount) {
        FixedExpense expense = new FixedExpense();
        expense.setActive(active);
        expense.setAmount(amount == null ? null : new BigDecimal(amount));
        return expense;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), String.valueOf(actual));
    }
}
