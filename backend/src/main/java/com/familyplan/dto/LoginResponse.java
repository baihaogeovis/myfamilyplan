package com.familyplan.dto;

import com.familyplan.entity.FamilyMember;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应DTO
 * 返回家庭信息 + 家庭成员列表(前端用于选择当前操作成员)
 */
@Data
public class LoginResponse implements Serializable {

    /** 访问令牌 */
    private String token;

    /** 家庭ID */
    private Long familyId;

    /** 家庭名称 */
    private String familyName;

    /** 家庭账号 */
    private String username;

    /** 家庭头像 */
    private String avatar;

    /** 家庭成员列表(登录后需选择当前操作成员) */
    private List<FamilyMember> members;

    /** 当前操作成员ID(登录时指定 memberId 时返回) */
    private Long currentMemberId;

    /** 当前操作成员姓名 */
    private String currentMemberName;
}
