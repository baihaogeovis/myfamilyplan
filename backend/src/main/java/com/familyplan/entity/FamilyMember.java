package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 家庭成员实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_member")
public class FamilyMember extends BaseEntity {

    /** 所属家庭ID */
    private Long familyId;

    /** 关联用户ID(兼容保留) */
    private Long userId;
    
    private String memberName;
    
    private String relation;
    
    private String gender;
    
    private LocalDate birthday;
    
    private String phone;
    
    private String idCard;
    
    private String role;
    
    private String avatar;
    
    private String remark;
    
    private Integer status;
}
