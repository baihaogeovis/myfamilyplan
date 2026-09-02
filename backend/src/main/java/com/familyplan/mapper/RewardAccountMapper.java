package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.RewardAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 奖励额度账户Mapper
 */
@Mapper
public interface RewardAccountMapper extends BaseMapper<RewardAccount> {

    /**
     * 锁住账户行, 串行化同一孩子的发放/扣减, 避免并发下余额与流水不一致
     */
    @Select("SELECT * FROM reward_account WHERE member_id = #{memberId} AND deleted = 0 FOR UPDATE")
    RewardAccount selectForUpdate(@Param("memberId") Long memberId);

    /**
     * 条件扣减额度: 余额不足时影响行数为 0, 由调用方判定为超额
     */
    @Update("UPDATE reward_account SET balance = balance - #{points} " +
            "WHERE member_id = #{memberId} AND deleted = 0 AND balance >= #{points}")
    int deductBalance(@Param("memberId") Long memberId, @Param("points") Integer points);

    /**
     * 增加额度
     */
    @Update("UPDATE reward_account SET balance = balance + #{points} " +
            "WHERE member_id = #{memberId} AND deleted = 0")
    int increaseBalance(@Param("memberId") Long memberId, @Param("points") Integer points);
}
