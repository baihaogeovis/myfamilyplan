package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.Notification;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 提醒通知控制器(按当前家庭隔离)
 */
@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private INotificationService notificationService;

    /**
     * 分页查询提醒
     */
    @GetMapping("/list")
    public Result<IPage<Notification>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) Integer isRead) {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        return Result.success(notificationService.pageQuery(familyId, page, size, isRead));
    }

    /**
     * 添加提醒(自动归属当前家庭)
     */
    @PostMapping
    public Result<Void> add(@RequestBody Notification notification) {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        notification.setFamilyId(familyId);
        notificationService.add(notification);
        return Result.success();
    }

    /**
     * 标记已读
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    /**
     * 全部已读
     */
    @PostMapping("/read/all")
    public Result<Void> markAllRead() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        notificationService.markAllRead(familyId);
        return Result.success();
    }

    /**
     * 删除提醒
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return Result.success();
    }

    /**
     * 未读数量
     */
    @GetMapping("/unread/count")
    public Result<Map<String, Object>> unreadCount() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        long count = notificationService.unreadCount(familyId);
        return Result.success(Map.of("count", count));
    }
}
