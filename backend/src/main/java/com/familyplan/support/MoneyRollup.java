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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把已经查出来的流水收成仪表盘、月报和固定支出总览要用的数字。
 * 单次遍历分桶，金额或日期缺失的行跳过，不让一条脏数据打断整月汇总。
 */
public final class MoneyRollup {

    private static final String UNCATEGORIZED = "未分类";

    private MoneyRollup() {
    }

    public static DashboardOverview overview(List<IncomeRecord> incomes,
                                             List<ExpenseRecord> expenses,
                                             List<BudgetPlan> budgets,
                                             long pendingCount,
                                             long memberCount) {
        BigDecimal income = sumIncome(incomes);
        BigDecimal expense = sumExpense(expenses);
        DashboardOverview overview = new DashboardOverview();
        overview.setMonthIncome(income);
        overview.setMonthExpense(expense);
        overview.setBudgetRemaining(sumBudget(budgets).subtract(expense));
        overview.setPendingCount(pendingCount);
        overview.setMemberCount(memberCount);
        return overview;
    }

    /**
     * 当月每一天一个点。不在这个月、或没有日期的流水不计入。
     */
    public static List<TrendPoint> dailyTrend(YearMonth month, List<IncomeRecord> incomes, List<ExpenseRecord> expenses) {
        int days = month.lengthOfMonth();
        BigDecimal[] incomeByDay = zeroDays(days);
        BigDecimal[] expenseByDay = zeroDays(days);
        if (incomes != null) {
            for (IncomeRecord record : incomes) {
                if (record == null || !inMonth(record.getIncomeDate(), month)) {
                    continue;
                }
                int day = record.getIncomeDate().getDayOfMonth();
                incomeByDay[day] = incomeByDay[day].add(amount(record.getAmount()));
            }
        }
        if (expenses != null) {
            for (ExpenseRecord record : expenses) {
                if (record == null || !inMonth(record.getExpenseDate(), month)) {
                    continue;
                }
                int day = record.getExpenseDate().getDayOfMonth();
                expenseByDay[day] = expenseByDay[day].add(amount(record.getAmount()));
            }
        }
        List<TrendPoint> points = new ArrayList<>(days);
        for (int day = 1; day <= days; day++) {
            points.add(new TrendPoint(month.atDay(day).toString(), incomeByDay[day], expenseByDay[day]));
        }
        return points;
    }

    /**
     * 收入在前、支出在后，再按日期从新到旧。没有日期的排在最后。
     */
    public static List<RecentTransaction> recent(List<IncomeRecord> incomes, List<ExpenseRecord> expenses, int limit) {
        List<RecentTransaction> list = new ArrayList<>();
        if (incomes != null) {
            for (IncomeRecord record : incomes) {
                if (record == null) {
                    continue;
                }
                list.add(new RecentTransaction(record.getId(), "INCOME", category(record.getCategory()),
                        amount(record.getAmount()), record.getIncomeDate(), record.getRemark()));
            }
        }
        if (expenses != null) {
            for (ExpenseRecord record : expenses) {
                if (record == null) {
                    continue;
                }
                list.add(new RecentTransaction(record.getId(), "EXPENSE", category(record.getCategory()),
                        amount(record.getAmount()), record.getExpenseDate(), record.getRemark()));
            }
        }
        list.sort(Comparator.comparing(RecentTransaction::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        if (limit <= 0 || list.isEmpty()) {
            return List.of();
        }
        if (list.size() <= limit) {
            return list;
        }
        return new ArrayList<>(list.subList(0, limit));
    }

    public static CategoryStatResponse byCategory(List<IncomeRecord> incomes, List<ExpenseRecord> expenses) {
        Map<String, BigDecimal> incomeMap = new HashMap<>();
        Map<String, BigDecimal> expenseMap = new HashMap<>();
        if (incomes != null) {
            for (IncomeRecord record : incomes) {
                if (record == null) {
                    continue;
                }
                add(incomeMap, category(record.getCategory()), record.getAmount());
            }
        }
        if (expenses != null) {
            for (ExpenseRecord record : expenses) {
                if (record == null) {
                    continue;
                }
                add(expenseMap, category(record.getCategory()), record.getAmount());
            }
        }
        CategoryStatResponse response = new CategoryStatResponse();
        response.setIncome(toItems(incomeMap));
        response.setExpense(toItems(expenseMap));
        return response;
    }

    /**
     * 始终返回 1 到 12 月。其他年份或没有日期的流水不计入。
     */
    public static List<MonthlyStat> byMonth(int year, List<IncomeRecord> incomes, List<ExpenseRecord> expenses) {
        BigDecimal[] incomeByMonth = zeroMonths();
        BigDecimal[] expenseByMonth = zeroMonths();
        if (incomes != null) {
            for (IncomeRecord record : incomes) {
                if (record == null || record.getIncomeDate() == null || record.getIncomeDate().getYear() != year) {
                    continue;
                }
                int month = record.getIncomeDate().getMonthValue();
                incomeByMonth[month] = incomeByMonth[month].add(amount(record.getAmount()));
            }
        }
        if (expenses != null) {
            for (ExpenseRecord record : expenses) {
                if (record == null || record.getExpenseDate() == null || record.getExpenseDate().getYear() != year) {
                    continue;
                }
                int month = record.getExpenseDate().getMonthValue();
                expenseByMonth[month] = expenseByMonth[month].add(amount(record.getAmount()));
            }
        }
        List<MonthlyStat> stats = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            stats.add(new MonthlyStat(month, incomeByMonth[month], expenseByMonth[month]));
        }
        return stats;
    }

    /**
     * 只加启用中的固定支出。停用或金额为空的不计入月合计。
     */
    public static BigDecimal activeFixedTotal(List<FixedExpense> expenses) {
        BigDecimal total = BigDecimal.ZERO;
        if (expenses == null) {
            return total;
        }
        for (FixedExpense expense : expenses) {
            if (expense == null || expense.getActive() == null || expense.getActive() != 1 || expense.getAmount() == null) {
                continue;
            }
            total = total.add(expense.getAmount());
        }
        return total;
    }

    private static BigDecimal sumIncome(List<IncomeRecord> incomes) {
        BigDecimal total = BigDecimal.ZERO;
        if (incomes == null) {
            return total;
        }
        for (IncomeRecord record : incomes) {
            if (record != null) {
                total = total.add(amount(record.getAmount()));
            }
        }
        return total;
    }

    private static BigDecimal sumExpense(List<ExpenseRecord> expenses) {
        BigDecimal total = BigDecimal.ZERO;
        if (expenses == null) {
            return total;
        }
        for (ExpenseRecord record : expenses) {
            if (record != null) {
                total = total.add(amount(record.getAmount()));
            }
        }
        return total;
    }

    private static BigDecimal sumBudget(List<BudgetPlan> budgets) {
        BigDecimal total = BigDecimal.ZERO;
        if (budgets == null) {
            return total;
        }
        for (BudgetPlan budget : budgets) {
            if (budget != null) {
                total = total.add(amount(budget.getAmount()));
            }
        }
        return total;
    }

    private static void add(Map<String, BigDecimal> bucket, String category, BigDecimal value) {
        bucket.merge(category, amount(value), BigDecimal::add);
    }

    private static List<CategoryItem> toItems(Map<String, BigDecimal> bucket) {
        List<CategoryItem> items = new ArrayList<>(bucket.size());
        for (Map.Entry<String, BigDecimal> entry : bucket.entrySet()) {
            items.add(new CategoryItem(entry.getKey(), entry.getValue()));
        }
        items.sort(Comparator.comparing(CategoryItem::getAmount).reversed()
                .thenComparing(CategoryItem::getCategory, Comparator.nullsLast(String::compareTo)));
        return items;
    }

    private static boolean inMonth(LocalDate date, YearMonth month) {
        return date != null && date.getYear() == month.getYear() && date.getMonthValue() == month.getMonthValue();
    }

    private static String category(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNCATEGORIZED;
        }
        return raw;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal[] zeroDays(int days) {
        BigDecimal[] values = new BigDecimal[days + 1];
        for (int i = 0; i < values.length; i++) {
            values[i] = BigDecimal.ZERO;
        }
        return values;
    }

    private static BigDecimal[] zeroMonths() {
        return zeroDays(12);
    }
}
