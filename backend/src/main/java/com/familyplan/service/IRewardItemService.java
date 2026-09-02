package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.RewardItem;

import java.util.List;

/**
 * 奖励活动方案服务接口
 */
public interface IRewardItemService extends IService<RewardItem> {

    /**
     * 查询当前家庭的活动方案列表(首次访问自动种入内置模板)
     */
    List<RewardItem> listByFamily(Long familyId, String itemType);

    /**
     * 新增活动方案(自动归属当前家庭)
     */
    void addItem(Long familyId, RewardItem item);

    /**
     * 更新活动方案(仅限当前家庭的方案)
     */
    void updateItem(Long familyId, RewardItem item);

    /**
     * 删除活动方案(仅限当前家庭的方案)
     */
    void deleteItem(Long familyId, Long id);

    /**
     * 取一个可用于记账的方案(校验归属/启用/类型, 不满足则抛出业务异常)
     */
    RewardItem getUsableItem(Long familyId, Long itemId, String expectedType);
}
