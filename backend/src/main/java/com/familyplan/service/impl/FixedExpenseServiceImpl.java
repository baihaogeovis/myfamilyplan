package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.dto.FinanceOverview;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.entity.FixedExpense;
import com.familyplan.entity.IncomeRecord;
import com.familyplan.mapper.ExpenseMapper;
import com.familyplan.mapper.FixedExpenseMapper;
import com.familyplan.mapper.IncomeMapper;
import com.familyplan.service.IFixedExpenseService;
import com.familyplan.support.MoneyRollup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 固定支出服务实现
 */
@Slf4j
@Service
public class FixedExpenseServiceImpl extends ServiceImpl<FixedExpenseMapper, FixedExpense> implements IFixedExpenseService {

    @Autowired
    private IncomeMapper incomeMapper;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Override
    public List<FixedExpense> listAll(Long memberId) {
        LambdaQueryWrapper<FixedExpense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FixedExpense::getMemberId, memberId)
                .orderByDesc(FixedExpense::getActive)
                .orderByAsc(FixedExpense::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public FinanceOverview overview(Long memberId, Integer year, Integer month) {
        FinanceOverview vo = new FinanceOverview();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // 累计统计
        BigDecimal totalIncome = sumIncome(memberId, null, null);
        BigDecimal totalExpense = sumExpense(memberId, null, null);
        vo.setTotalIncome(totalIncome);
        vo.setTotalExpense(totalExpense);
        vo.setTotalBalance(totalIncome.subtract(totalExpense));

        // 本月统计
        BigDecimal monthIncome = sumIncome(memberId, monthStart, monthEnd);
        BigDecimal monthExpense = sumExpense(memberId, monthStart, monthEnd);
        vo.setMonthIncome(monthIncome);
        vo.setMonthExpense(monthExpense);
        vo.setMonthBalance(monthIncome.subtract(monthExpense));

        // 启用中的固定支出
        List<FixedExpense> fixedExpenses = listAll(memberId);
        vo.setFixedExpenses(fixedExpenses);
        vo.setMonthFixedTotal(MoneyRollup.activeFixedTotal(fixedExpenses));

        log.info("资金总览统计成功: memberId={}, 累计结余={}", memberId, vo.getTotalBalance());
        return vo;
    }

    @Override
    public void add(FixedExpense expense) {
        if (expense.getActive() == null) {
            expense.setActive(1);
        }
        if (expense.getAutoRecord() == null) {
            expense.setAutoRecord(0);
        }
        baseMapper.insert(expense);
        log.info("添加固定支出成功: {}", expense.getExpenseName());
    }

    @Override
    public void update(FixedExpense expense) {
        baseMapper.updateById(expense);
        log.info("更新固定支出成功: ID={}", expense.getId());
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除固定支出成功: ID={}", id);
    }

    /**
     * 统计收入合计
     */
    private BigDecimal sumIncome(Long memberId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<IncomeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IncomeRecord::getMemberId, memberId);
        if (start != null) {
            wrapper.ge(IncomeRecord::getIncomeDate, start);
        }
        if (end != null) {
            wrapper.le(IncomeRecord::getIncomeDate, end);
        }
        wrapper.select(IncomeRecord::getAmount);
        return incomeMapper.selectList(wrapper).stream()
                .map(IncomeRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 统计支出合计
     */
    private BigDecimal sumExpense(Long memberId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<ExpenseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecord::getMemberId, memberId);
        if (start != null) {
            wrapper.ge(ExpenseRecord::getExpenseDate, start);
        }
        if (end != null) {
            wrapper.le(ExpenseRecord::getExpenseDate, end);
        }
        wrapper.select(ExpenseRecord::getAmount);
        return expenseMapper.selectList(wrapper).stream()
                .map(ExpenseRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
