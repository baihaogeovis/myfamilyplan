package com.familyplan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse implements Serializable {
    
    private Long userId;
    
    private String token;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String role;
    
    public LoginResponse(Long userId, String token, String username, String nickname, String avatar, String role) {
        this.userId = userId;
        this.token = token;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
    }
}
