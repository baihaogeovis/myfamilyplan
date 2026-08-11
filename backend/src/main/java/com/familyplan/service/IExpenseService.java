package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.ExpenseRecord;

import java.time.LocalDate;

/**
 * 支出记录服务接口
 */
public interface IExpenseService extends IService<ExpenseRecord> {

    /**
     * 分页查询支出记录
     */
    IPage<ExpenseRecord> pageQuery(Long memberId, Integer page, Integer size,
                                    String category, String type,
                                    LocalDate startDate, LocalDate endDate);

    /**
     * 添加支出记录
     */
    void add(ExpenseRecord record);

    /**
     * 更新支出记录
     */
    void update(ExpenseRecord record);

    /**
     * 删除支出记录
     */
    void delete(Long id);
}
