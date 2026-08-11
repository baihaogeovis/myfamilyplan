package com.familyplan.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前请求的家庭上下文(由JWT过滤器解析后放入认证信息details中)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyContext implements Serializable {

    /** 当前家庭ID */
    private Long familyId;

    /** 当前操作成员ID(通过请求头 X-Member-Id 传入, 可为空) */
    private Long memberId;
}
