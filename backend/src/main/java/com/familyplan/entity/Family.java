package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家庭实体(家庭账号:一个家庭一个登录账号,家庭成员共用该账号登录)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family")
public class Family extends BaseEntity {

    /** 家庭名称 */
    private String familyName;

    /** 家庭登录账号 */
    private String username;

    /** 密码(加密) */
    private String password;

    /** 家庭头像 */
    private String avatar;

    /** 备注 */
    private String remark;

    /** 状态(0-禁用,1-启用) */
    private Integer status;
}
