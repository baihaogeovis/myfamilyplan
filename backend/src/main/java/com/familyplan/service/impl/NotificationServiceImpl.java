package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.Notification;
import com.familyplan.mapper.NotificationMapper;
import com.familyplan.service.FixedExpenseReminderService;
import com.familyplan.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 提醒通知服务实现
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Autowired
    private FixedExpenseReminderService fixedExpenseReminderService;

    @Override
    public IPage<Notification> pageQuery(Long familyId, Integer page, Integer size, Integer isRead) {
        refreshFixedExpenseReminders(familyId);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getFamilyId, familyId);
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void add(Notification notification) {
        // 手工提醒不占用固定支出的幂等键
        notification.setBizKey(null);
        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(LocalDateTime.now());
        }
        baseMapper.insert(notification);
        log.info("添加提醒成功: 家庭={}", notification.getFamilyId());
    }

    @Override
    public void markRead(Long id) {
        Notification notification = baseMapper.selectById(id);
        if (notification != null) {
            notification.setIsRead(1);
            baseMapper.updateById(notification);
        }
    }

    @Override
    public void markAllRead(Long familyId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getFamilyId, familyId).eq(Notification::getIsRead, 0);
        Notification entity = new Notification();
        entity.setIsRead(1);
        baseMapper.update(entity, wrapper);
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除提醒成功: ID={}", id);
    }

    @Override
    public long unreadCount(Long familyId) {
        refreshFixedExpenseReminders(familyId);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getFamilyId, familyId).eq(Notification::getIsRead, 0);
        return baseMapper.selectCount(wrapper);
    }

    /**
     * 打开提醒或未读数时补齐即将到期/已逾期的固定支出提醒。失败不影响列表本身。
     */
    private void refreshFixedExpenseReminders(Long familyId) {
        try {
            fixedExpenseReminderService.generateForFamily(familyId);
        } catch (Exception ex) {
            log.warn("生成固定支出提醒失败: {}", ex.getMessage());
        }
    }
}
