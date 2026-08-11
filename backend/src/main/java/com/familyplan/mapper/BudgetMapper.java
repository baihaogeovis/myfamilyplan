package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.BudgetPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算规划Mapper
 */
@Mapper
public interface BudgetMapper extends BaseMapper<BudgetPlan> {
}
