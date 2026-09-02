package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.common.BusinessException;
import com.familyplan.entity.RewardItem;
import com.familyplan.mapper.RewardItemMapper;
import com.familyplan.service.IRewardItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 奖励活动方案服务实现
 */
@Slf4j
@Service
public class RewardItemServiceImpl extends ServiceImpl<RewardItemMapper, RewardItem> implements IRewardItemService {

    private static final String TYPE_EARN = "EARN";
    private static final String TYPE_SPEND = "SPEND";
    private static final String MODE_TIMES = "TIMES";
    private static final String MODE_DURATION = "DURATION";

    /** 内置模板: {名称, 类型, 分类, 计价方式, 额度, 时长档位分钟, 图标, 颜色, 排序} */
    private static final String[][] TEMPLATES = {
            {"看电视", TYPE_SPEND, "SCREEN", MODE_DURATION, "10", "30", "影", "#35a7d7", "10"},
            {"打游戏", TYPE_SPEND, "SCREEN", MODE_DURATION, "15", "30", "游", "#8b6be0", "20"},
            {"买零食", TYPE_SPEND, "SNACK", MODE_TIMES, "10", "30", "零", "#f7b733", "30"},
            {"买玩具", TYPE_SPEND, "TOY", MODE_TIMES, "100", "30", "玩", "#e8604c", "40"},
            {"出游加餐", TYPE_SPEND, "ACTIVITY", MODE_TIMES, "50", "30", "乐", "#a3d93f", "50"},
            {"好好吃饭", TYPE_EARN, "HABIT", MODE_TIMES, "5", "30", "饭", "#2fae52", "10"},
            {"认真学习", TYPE_EARN, "STUDY", MODE_TIMES, "10", "30", "学", "#188043", "20"},
            {"活动锻炼", TYPE_EARN, "SPORTS", MODE_TIMES, "8", "30", "动", "#5fc673", "30"},
            {"主动做家务", TYPE_EARN, "HOUSEWORK", MODE_TIMES, "10", "30", "做", "#a3d93f", "40"},
            {"早睡早起", TYPE_EARN, "HABIT", MODE_TIMES, "5", "30", "睡", "#35a7d7", "50"},
            {"达成成长目标", TYPE_EARN, "STUDY", MODE_TIMES, "30", "30", "标", "#8b6be0", "60"}
    };

    @Override
    public List<RewardItem> listByFamily(Long familyId, String itemType) {
        ensureTemplateItems(familyId);
        LambdaQueryWrapper<RewardItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardItem::getFamilyId, familyId);
        if (itemType != null && !itemType.isEmpty()) {
            wrapper.eq(RewardItem::getItemType, itemType);
        }
        wrapper.orderByAsc(RewardItem::getItemType)
                .orderByAsc(RewardItem::getSortOrder)
                .orderByAsc(RewardItem::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void addItem(Long familyId, RewardItem item) {
        item.setId(null);
        item.setFamilyId(familyId);
        normalize(item);
        baseMapper.insert(item);
        log.info("添加奖励方案成功: {}", item.getItemName());
    }

    @Override
    public void updateItem(Long familyId, RewardItem item) {
        RewardItem exists = checkAndGet(familyId, item.getId());
        item.setFamilyId(exists.getFamilyId());
        normalize(item);
        baseMapper.updateById(item);
        log.info("更新奖励方案成功: ID={}", item.getId());
    }

    @Override
    public void deleteItem(Long familyId, Long id) {
        checkAndGet(familyId, id);
        baseMapper.deleteById(id);
        log.info("删除奖励方案成功: ID={}", id);
    }

    @Override
    public RewardItem getUsableItem(Long familyId, Long itemId, String expectedType) {
        if (itemId == null) {
            throw new BusinessException("请选择活动方案");
        }
        RewardItem item = baseMapper.selectById(itemId);
        if (item == null || !familyId.equals(item.getFamilyId())) {
            throw new BusinessException(403, "活动方案不存在或不属于当前家庭");
        }
        if (!expectedType.equals(item.getItemType())) {
            throw new BusinessException(TYPE_SPEND.equals(expectedType) ? "该方案不是消费项目" : "该方案不是赚取项目");
        }
        if (item.getActive() != null && item.getActive() == 0) {
            throw new BusinessException("该方案已停用，请先在活动方案中启用");
        }
        return item;
    }

    /**
     * 家庭首次使用奖励额度时种入内置活动方案(幂等: 已有任意方案则跳过)
     */
    private void ensureTemplateItems(Long familyId) {
        LambdaQueryWrapper<RewardItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardItem::getFamilyId, familyId);
        if (baseMapper.selectCount(wrapper) > 0) {
            return;
        }
        List<RewardItem> items = new ArrayList<>();
        for (String[] t : TEMPLATES) {
            RewardItem item = new RewardItem();
            item.setFamilyId(familyId);
            item.setItemName(t[0]);
            item.setItemType(t[1]);
            item.setCategory(t[2]);
            item.setPricingMode(t[3]);
            item.setPoints(Integer.parseInt(t[4]));
            item.setUnitMinutes(Integer.parseInt(t[5]));
            item.setIcon(t[6]);
            item.setColor(t[7]);
            item.setSortOrder(Integer.parseInt(t[8]));
            item.setActive(1);
            items.add(item);
        }
        saveBatch(items);
        log.info("已为家庭 {} 种入内置奖励活动方案 {} 条", familyId, items.size());
    }

    /**
     * 补全缺省值, 避免前端漏传导致库里出现脏枚举
     */
    private void normalize(RewardItem item) {
        if (item.getItemType() == null || item.getItemType().isEmpty()) {
            item.setItemType(TYPE_SPEND);
        }
        if (!TYPE_EARN.equals(item.getItemType()) && !TYPE_SPEND.equals(item.getItemType())) {
            throw new BusinessException("活动方案类型只能是 EARN 或 SPEND");
        }
        if (item.getPricingMode() == null || item.getPricingMode().isEmpty()) {
            item.setPricingMode(MODE_TIMES);
        }
        if (!MODE_TIMES.equals(item.getPricingMode()) && !MODE_DURATION.equals(item.getPricingMode())) {
            throw new BusinessException("计价方式只能是 TIMES(按次) 或 DURATION(按时长)");
        }
        if (item.getPoints() == null || item.getPoints() <= 0) {
            throw new BusinessException("单价额度必须大于 0");
        }
        if (item.getUnitMinutes() == null || item.getUnitMinutes() <= 0) {
            item.setUnitMinutes(30);
        }
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        if (item.getActive() == null) {
            item.setActive(1);
        }
    }

    private RewardItem checkAndGet(Long familyId, Long id) {
        RewardItem item = id == null ? null : baseMapper.selectById(id);
        if (item == null || !familyId.equals(item.getFamilyId())) {
            throw new BusinessException(403, "活动方案不存在或不属于当前家庭");
        }
        return item;
    }
}
