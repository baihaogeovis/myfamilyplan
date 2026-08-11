package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
public class UserInfo extends BaseEntity {
    
    private String username;
    
    private String password;
    
    private String nickname;
    
    private String avatar;
    
    private String role;
    
    private Integer status;
    
    @TableField(exist = false)
    private LocalDateTime lastLoginTime;
}
