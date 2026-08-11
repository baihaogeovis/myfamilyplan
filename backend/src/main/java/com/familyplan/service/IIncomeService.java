package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.IncomeRecord;

import java.time.LocalDate;

/**
 * 收入记录服务接口
 */
public interface IIncomeService extends IService<IncomeRecord> {

    /**
     * 分页查询收入记录
     */
    IPage<IncomeRecord> pageQuery(Long memberId, Integer page, Integer size,
                                   String category, LocalDate startDate, LocalDate endDate);

    /**
     * 添加收入记录
     */
    void add(IncomeRecord record);

    /**
     * 更新收入记录
     */
    void update(IncomeRecord record);

    /**
     * 删除收入记录
     */
    void delete(Long id);
}
