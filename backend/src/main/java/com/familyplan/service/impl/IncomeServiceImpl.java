package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.IncomeRecord;
import com.familyplan.mapper.IncomeMapper;
import com.familyplan.service.IIncomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 收入记录服务实现
 */
@Slf4j
@Service
public class IncomeServiceImpl extends ServiceImpl<IncomeMapper, IncomeRecord> implements IIncomeService {

    @Override
    public IPage<IncomeRecord> pageQuery(Long memberId, Integer page, Integer size,
                                         String category, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<IncomeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IncomeRecord::getMemberId, memberId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(IncomeRecord::getCategory, category);
        }
        if (startDate != null) {
            wrapper.ge(IncomeRecord::getIncomeDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(IncomeRecord::getIncomeDate, endDate);
        }
        wrapper.orderByDesc(IncomeRecord::getIncomeDate);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void add(IncomeRecord record) {
        baseMapper.insert(record);
        log.info("添加收入记录成功: 金额={}", record.getAmount());
    }

    @Override
    public void update(IncomeRecord record) {
        baseMapper.updateById(record);
        log.info("更新收入记录成功: ID={}", record.getId());
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除收入记录成功: ID={}", id);
    }
}
