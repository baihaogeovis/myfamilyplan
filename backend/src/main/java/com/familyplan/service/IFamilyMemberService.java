package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.FamilyMember;

import java.util.List;

/**
 * 家庭成员服务接口
 */
public interface IFamilyMemberService extends IService<FamilyMember> {
    
    /**
     * 根据用户ID查询成员列表
     */
    List<FamilyMember> getListByUserId(Long userId);
    
    /**
     * 添加家庭成员
     */
    void addMember(FamilyMember member);
    
    /**
     * 更新家庭成员
     */
    void updateMember(FamilyMember member);
    
    /**
     * 删除家庭成员
     */
    void deleteMember(Long id);
}
