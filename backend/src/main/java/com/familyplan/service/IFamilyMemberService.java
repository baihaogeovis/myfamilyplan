package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.FamilyMember;

import java.util.List;

/**
 * 家庭成员服务接口
 */
public interface IFamilyMemberService extends IService<FamilyMember> {

    /**
     * 根据家庭ID查询成员列表
     */
    List<FamilyMember> getListByFamilyId(Long familyId);

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

    /**
     * 校验成员是否属于指定家庭(不属于则抛出业务异常)
     */
    void checkMemberBelongsToFamily(Long familyId, Long memberId);
}
