package com.familyplan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 注册新家庭请求DTO
 * 家庭账号 = 家庭登录账号, 注册成功自动创建户主成员
 */
@Data
public class RegisterRequest implements Serializable {

    /** 家庭名称 */
    private String familyName;

    /** 家庭登录账号 */
    private String username;

    /** 密码(明文, 后端加密存储) */
    private String password;

    /** 户主姓名(可选, 默认取家庭名称) */
    private String ownerName;
}
