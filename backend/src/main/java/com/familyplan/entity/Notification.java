package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知提醒实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    /** 所属家庭ID */
    private Long familyId;

    private Long userId;
    
    private String title;
    
    private String content;
    
    private String type;
    
    private Integer isRead;

    /**
     * 业务幂等键。固定支出提醒为 FE:{账单ID}:{yyyyMM}，手工提醒为空。
     * 同一家庭、同一账单、同一账期只保留一条，避免重复打扰。
     */
    private String bizKey;
}
