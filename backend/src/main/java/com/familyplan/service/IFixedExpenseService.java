package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.dto.FinanceOverview;
import com.familyplan.entity.FixedExpense;

import java.util.List;

/**
 * 固定支出服务接口
 */
public interface IFixedExpenseService extends IService<FixedExpense> {

    /**
     * 查询全部固定支出配置
     */
    List<FixedExpense> listAll(Long memberId);

    /**
     * 资金总览统计(收入/支出/结余/固定支出)
     */
    FinanceOverview overview(Long memberId, Integer year, Integer month);

    /**
     * 添加固定支出
     */
    void add(FixedExpense expense);

    /**
     * 更新固定支出
     */
    void update(FixedExpense expense);

    /**
     * 删除固定支出
     */
    void delete(Long id);
}
