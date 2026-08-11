package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.common.BusinessException;
import com.familyplan.entity.Family;
import com.familyplan.entity.FamilyMember;
import com.familyplan.mapper.FamilyMapper;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IFamilyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 家庭服务实现
 */
@Slf4j
@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements IFamilyService {

    @Autowired
    private FamilyMapper familyMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IFamilyMemberService memberService;

    @Override
    public Family getByUsername(String username) {
        LambdaQueryWrapper<Family> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Family::getUsername, username);
        return familyMapper.selectOne(wrapper);
    }

    @Override
    public Family authenticate(String username, String password) {
        Family family = getByUsername(username);
        if (family == null) {
            throw new BusinessException(401, "家庭账号或密码错误");
        }
        if (!passwordEncoder.matches(password, family.getPassword())) {
            throw new BusinessException(401, "家庭账号或密码错误");
        }
        if (family.getStatus() != null && family.getStatus() == 0) {
            throw new BusinessException(403, "家庭账号已被禁用");
        }
        return family;
    }

    @Override
    public Family register(String familyName, String username, String password, String ownerName) {
        // 1. 参数校验
        if (!StringUtils.hasText(familyName)) {
            throw new BusinessException("家庭名称不能为空");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("家庭账号不能为空");
        }
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }

        // 2. 账号唯一性校验
        if (getByUsername(username) != null) {
            throw new BusinessException("家庭账号已存在, 请更换账号");
        }

        // 3. 创建家庭(密码加密, 默认启用)
        Family family = new Family();
        family.setFamilyName(familyName.trim());
        family.setUsername(username.trim());
        family.setPassword(passwordEncoder.encode(password));
        family.setStatus(1);
        familyMapper.insert(family);
        log.info("注册新家庭成功: {} (familyId={})", username, family.getId());

        // 4. 自动创建户主成员, 便于注册后直接进入系统
        FamilyMember owner = new FamilyMember();
        owner.setFamilyId(family.getId());
        owner.setMemberName(StringUtils.hasText(ownerName) ? ownerName.trim() : familyName.trim());
        owner.setRelation("OWNER");
        owner.setRole("ADMIN");
        owner.setStatus(1);
        memberService.addMember(owner);

        return family;
    }
}
