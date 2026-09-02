package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.dto.RewardEarnRequest;
import com.familyplan.dto.RewardSpendRequest;
import com.familyplan.dto.RewardSummary;
import com.familyplan.entity.RewardAccount;
import com.familyplan.entity.RewardTxn;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IRewardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 奖励额度控制器(按当前登录家庭管理孩子的额度发放、消费与流水)
 */
@Slf4j
@RestController
@RequestMapping("/reward")
public class RewardController {

    @Autowired
    private IRewardService rewardService;

    /**
     * 取孩子额度账户(不存在则建账)
     */
    @GetMapping("/account")
    public Result<RewardAccount> account(@RequestParam Long memberId) {
        return Result.success(rewardService.getOrCreateAccount(SecurityUtil.getRequiredFamilyId(), memberId));
    }

    /**
     * 保存额度规则(每周额度/周期类型/是否结转)
     */
    @PutMapping("/account")
    public Result<Void> saveAccount(@RequestBody RewardAccount account) {
        rewardService.saveAccount(SecurityUtil.getRequiredFamilyId(), account);
        return Result.success();
    }

    /**
     * 发放本周期额度(points 缺省时取账户的每周额度)
     */
    @PostMapping("/account/grant")
    public Result<RewardTxn> grant(@RequestParam Long memberId,
                                   @RequestParam(required = false) Integer points) {
        return Result.success(rewardService.grantPeriod(
                SecurityUtil.getRequiredFamilyId(), memberId, points, SecurityUtil.getCurrentMemberId()));
    }

    /**
     * 额度概览(余额 + 本周期收支 + 消费分类构成 + 待确认笔数)
     */
    @GetMapping("/summary")
    public Result<RewardSummary> summary(@RequestParam Long memberId,
                                         @RequestParam(required = false) String periodKey) {
        return Result.success(rewardService.summary(SecurityUtil.getRequiredFamilyId(), memberId, periodKey));
    }

    /**
     * 提交额度消费申请(需父母确认后扣减)
     */
    @PostMapping("/spend")
    public Result<RewardTxn> spend(@RequestBody RewardSpendRequest request) {
        return Result.success(rewardService.spend(
                SecurityUtil.getRequiredFamilyId(), request, SecurityUtil.getCurrentMemberId()));
    }

    /**
     * 记录良好行为赚取额度
     */
    @PostMapping("/earn")
    public Result<RewardTxn> earn(@RequestBody RewardEarnRequest request) {
        return Result.success(rewardService.earn(
                SecurityUtil.getRequiredFamilyId(), request, SecurityUtil.getCurrentMemberId()));
    }

    /**
     * 手动加减额度(即时生效, points 带符号)
     */
    @PostMapping("/adjust")
    public Result<RewardTxn> adjust(@RequestParam Long memberId,
                                    @RequestParam Integer points,
                                    @RequestParam(required = false) String remark) {
        return Result.success(rewardService.adjust(SecurityUtil.getRequiredFamilyId(),
                memberId, points, remark, SecurityUtil.getCurrentMemberId()));
    }

    /**
     * 分页查询额度流水(memberId 缺省为整个家庭)
     */
    @GetMapping("/txn/page")
    public Result<IPage<RewardTxn>> txnPage(@RequestParam(required = false) Long memberId,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String direction,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(rewardService.pageTxn(SecurityUtil.getRequiredFamilyId(),
                memberId, direction, status, startDate, endDate, page, size));
    }

    /**
     * 确认消费: 扣减额度并生效
     */
    @PutMapping("/txn/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        rewardService.confirm(SecurityUtil.getRequiredFamilyId(), id, SecurityUtil.getCurrentMemberId());
        return Result.success();
    }

    /**
     * 驳回消费: 不扣减额度
     */
    @PutMapping("/txn/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
        rewardService.reject(SecurityUtil.getRequiredFamilyId(), id, remark, SecurityUtil.getCurrentMemberId());
        return Result.success();
    }

    /**
     * 删除流水(已生效的消费视为撤销并回补额度)
     */
    @DeleteMapping("/txn/{id}")
    public Result<Void> deleteTxn(@PathVariable Long id) {
        rewardService.deleteTxn(SecurityUtil.getRequiredFamilyId(), id);
        return Result.success();
    }
}
