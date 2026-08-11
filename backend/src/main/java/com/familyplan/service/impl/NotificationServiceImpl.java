package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.Notification;
import com.familyplan.mapper.NotificationMapper;
import com.familyplan.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 提醒通知服务实现
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Override
    public IPage<Notification> pageQuery(Long familyId, Integer page, Integer size, Integer isRead) {
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
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getFamilyId, familyId).eq(Notification::getIsRead, 0);
        return baseMapper.selectCount(wrapper);
    }
}
