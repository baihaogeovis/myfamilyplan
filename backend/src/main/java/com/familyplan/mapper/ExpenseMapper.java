package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.ExpenseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支出记录Mapper
 */
@Mapper
public interface ExpenseMapper extends BaseMapper<ExpenseRecord> {
}
