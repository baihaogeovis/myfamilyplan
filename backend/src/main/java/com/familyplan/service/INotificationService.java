package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.Notification;

/**
 * 提醒通知服务接口
 */
public interface INotificationService extends IService<Notification> {

    /**
     * 分页查询提醒(按家庭)
     */
    IPage<Notification> pageQuery(Long familyId, Integer page, Integer size, Integer isRead);

    /**
     * 添加提醒
     */
    void add(Notification notification);

    /**
     * 标记已读
     */
    void markRead(Long id);

    /**
     * 全部已读(按家庭)
     */
    void markAllRead(Long familyId);

    /**
     * 删除提醒
     */
    void delete(Long id);

    /**
     * 未读数量(按家庭)
     */
    long unreadCount(Long familyId);
}
