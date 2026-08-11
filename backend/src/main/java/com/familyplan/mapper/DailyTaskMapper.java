package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.DailyTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日任务Mapper
 */
@Mapper
public interface DailyTaskMapper extends BaseMapper<DailyTask> {
}
