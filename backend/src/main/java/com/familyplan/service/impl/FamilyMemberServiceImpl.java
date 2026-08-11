package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.common.BusinessException;
import com.familyplan.entity.FamilyMember;
import com.familyplan.mapper.FamilyMemberMapper;
import com.familyplan.service.IFamilyMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 家庭成员服务实现
 */
@Slf4j
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService {

    @Override
    public List<FamilyMember> getListByFamilyId(Long familyId) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getFamilyId, familyId)
                .orderByAsc(FamilyMember::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void addMember(FamilyMember member) {
        member.setStatus(1);
        // 老库 user_id 为 NOT NULL 且无默认值, 未传时补默认 0 避免插入失败(兼容保留字段)
        if (member.getUserId() == null) {
            member.setUserId(0L);
        }
        baseMapper.insert(member);
        log.info("添加家庭成员成功: {}", member.getMemberName());
    }

    @Override
    public void updateMember(FamilyMember member) {
        baseMapper.updateById(member);
        log.info("更新家庭成员成功: {}", member.getMemberName());
    }

    @Override
    public void deleteMember(Long id) {
        baseMapper.deleteById(id);
        log.info("删除家庭成员成功, ID: {}", id);
    }

    @Override
    public void checkMemberBelongsToFamily(Long familyId, Long memberId) {
        FamilyMember member = getById(memberId);
        if (member == null || !familyId.equals(member.getFamilyId())) {
            throw new BusinessException(403, "成员不存在或不属于当前家庭");
        }
    }
}
