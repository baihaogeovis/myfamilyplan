package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.RewardTxn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 奖励额度流水Mapper
 */
@Mapper
public interface RewardTxnMapper extends BaseMapper<RewardTxn> {

    /**
     * 按方向汇总指定周期的已生效额度(返回 direction / total)
     */
    @Select("SELECT direction, COALESCE(SUM(points), 0) AS total FROM reward_txn " +
            "WHERE member_id = #{memberId} AND status = 'CONFIRMED' AND deleted = 0 " +
            "AND period_key = #{periodKey} GROUP BY direction")
    List<Map<String, Object>> sumByDirection(@Param("memberId") Long memberId,
                                             @Param("periodKey") String periodKey);

    /**
     * 按分类汇总指定周期的已生效消费额度(返回 category / total, 取绝对值便于展示)
     */
    @Select("SELECT category, COALESCE(SUM(ABS(points)), 0) AS total FROM reward_txn " +
            "WHERE member_id = #{memberId} AND status = 'CONFIRMED' AND deleted = 0 " +
            "AND direction = 'SPEND' AND period_key = #{periodKey} " +
            "GROUP BY category ORDER BY total DESC")
    List<Map<String, Object>> sumSpendByCategory(@Param("memberId") Long memberId,
                                                 @Param("periodKey") String periodKey);
}
