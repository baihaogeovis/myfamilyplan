package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.Notification;
import com.familyplan.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 提醒通知控制器
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
    public Result<IPage<Notification>> list(@RequestParam Long userId,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) Integer isRead) {
        return Result.success(notificationService.pageQuery(userId, page, size, isRead));
    }

    /**
     * 添加提醒
     */
    @PostMapping
    public Result<Void> add(@RequestBody Notification notification) {
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
    public Result<Void> markAllRead(@RequestParam Long userId) {
        notificationService.markAllRead(userId);
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
    public Result<Map<String, Object>> unreadCount(@RequestParam Long userId) {
        long count = notificationService.unreadCount(userId);
        return Result.success(Map.of("count", count));
    }
}
