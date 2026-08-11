package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.IncomeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收入记录Mapper
 */
@Mapper
public interface IncomeMapper extends BaseMapper<IncomeRecord> {
}
