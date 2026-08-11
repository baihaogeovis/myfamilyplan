package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    public List<FamilyMember> getListByUserId(Long userId) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getUserId, userId);
        return baseMapper.selectList(wrapper);
    }
    
    @Override
    public void addMember(FamilyMember member) {
        member.setStatus(1);
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
}
