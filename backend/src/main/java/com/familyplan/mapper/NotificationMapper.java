package com.familyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyplan.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提醒通知Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
