package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.entity.RewardItem;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IRewardItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 奖励活动方案控制器(家庭维度的价目表)
 */
@Slf4j
@RestController
@RequestMapping("/reward/item")
public class RewardItemController {

    @Autowired
    private IRewardItemService rewardItemService;

    /**
     * 活动方案列表(itemType 传 EARN/SPEND 过滤, 缺省返回全部)
     */
    @GetMapping("/list")
    public Result<List<RewardItem>> list(@RequestParam(required = false) String itemType) {
        return Result.success(rewardItemService.listByFamily(SecurityUtil.getRequiredFamilyId(), itemType));
    }

    /**
     * 新增活动方案
     */
    @PostMapping
    public Result<Void> add(@RequestBody RewardItem item) {
        rewardItemService.addItem(SecurityUtil.getRequiredFamilyId(), item);
        return Result.success();
    }

    /**
     * 更新活动方案
     */
    @PutMapping
    public Result<Void> update(@RequestBody RewardItem item) {
        rewardItemService.updateItem(SecurityUtil.getRequiredFamilyId(), item);
        return Result.success();
    }

    /**
     * 删除活动方案(不影响历史流水)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rewardItemService.deleteItem(SecurityUtil.getRequiredFamilyId(), id);
        return Result.success();
    }
}
