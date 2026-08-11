package com.familyplan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求DTO
 * 家庭账号登录: username=家庭账号, password=密码, memberId=当前操作成员(可选,指定后校验是否属于该家庭)
 */
@Data
public class LoginRequest implements Serializable {

    /** 家庭账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 当前操作成员ID(可选: 登录时校验该成员属于该家庭) */
    private Long memberId;
}
