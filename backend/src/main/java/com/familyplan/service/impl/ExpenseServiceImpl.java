package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.mapper.ExpenseMapper;
import com.familyplan.service.IExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 支出记录服务实现
 */
@Slf4j
@Service
public class ExpenseServiceImpl extends ServiceImpl<ExpenseMapper, ExpenseRecord> implements IExpenseService {

    @Override
    public IPage<ExpenseRecord> pageQuery(Long memberId, Integer page, Integer size,
                                          String category, String type,
                                          LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ExpenseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecord::getMemberId, memberId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(ExpenseRecord::getCategory, category);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(ExpenseRecord::getType, type);
        }
        if (startDate != null) {
            wrapper.ge(ExpenseRecord::getExpenseDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ExpenseRecord::getExpenseDate, endDate);
        }
        wrapper.orderByDesc(ExpenseRecord::getExpenseDate);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void add(ExpenseRecord record) {
        baseMapper.insert(record);
        log.info("添加支出记录成功: 金额={}", record.getAmount());
    }

    @Override
    public void update(ExpenseRecord record) {
        baseMapper.updateById(record);
        log.info("更新支出记录成功: ID={}", record.getId());
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除支出记录成功: ID={}", id);
    }
}
