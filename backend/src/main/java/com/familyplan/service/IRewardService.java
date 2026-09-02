package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.dto.RewardEarnRequest;
import com.familyplan.dto.RewardSpendRequest;
import com.familyplan.dto.RewardSummary;
import com.familyplan.entity.RewardAccount;
import com.familyplan.entity.RewardTxn;

import java.time.LocalDate;

/**
 * 奖励额度服务接口
 */
public interface IRewardService {

    /**
     * 取孩子额度账户, 不存在则按 0 余额建账
     */
    RewardAccount getOrCreateAccount(Long familyId, Long memberId);

    /**
     * 保存额度规则(每周额度/周期类型/是否结转/备注)
     */
    void saveAccount(Long familyId, RewardAccount account);

    /**
     * 发放本周期额度(同周期重复发放会被拒绝; 结转关闭时先把结余清零)
     */
    RewardTxn grantPeriod(Long familyId, Long memberId, Integer points, Long operatorMemberId);

    /**
     * 额度概览: 余额 + 本周期收支 + 消费分类构成 + 待确认笔数
     */
    RewardSummary summary(Long familyId, Long memberId, String periodKey);

    /**
     * 提交消费申请(待父母确认后才扣减额度)
     */
    RewardTxn spend(Long familyId, RewardSpendRequest request, Long operatorMemberId);

    /**
     * 记录良好行为赚取额度(父母操作即生效)
     */
    RewardTxn earn(Long familyId, RewardEarnRequest request, Long operatorMemberId);

    /**
     * 手动加减额度(即时生效, points 带符号)
     */
    RewardTxn adjust(Long familyId, Long memberId, Integer points, String remark, Long operatorMemberId);

    /**
     * 流水分页
     */
    IPage<RewardTxn> pageTxn(Long familyId, Long memberId, String direction, String status,
                             LocalDate startDate, LocalDate endDate, Integer page, Integer size);

    /**
     * 确认消费: 扣减余额并置为已生效
     */
    void confirm(Long familyId, Long txnId, Long operatorMemberId);

    /**
     * 驳回消费: 不扣减额度
     */
    void reject(Long familyId, Long txnId, String remark, Long operatorMemberId);

    /**
     * 删除流水: 待确认/已驳回直接删除, 已确认的消费视为撤销并回补余额
     */
    void deleteTxn(Long familyId, Long txnId);

    /**
     * 当前周期标识(如 2026-W36 / 2026-08)
     */
    String currentPeriodKey(String periodType);
}
