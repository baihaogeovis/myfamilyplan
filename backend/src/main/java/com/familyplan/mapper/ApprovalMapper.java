package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金审批Mapper
 */
@Mapper
public interface ApprovalMapper extends BaseMapper<ApprovalRecord> {
}
