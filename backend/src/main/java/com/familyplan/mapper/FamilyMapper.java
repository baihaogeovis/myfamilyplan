package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.Family;
import org.apache.ibatis.annotations.Mapper;

/**
 * 家庭Mapper
 */
@Mapper
public interface FamilyMapper extends BaseMapper<Family> {
}
