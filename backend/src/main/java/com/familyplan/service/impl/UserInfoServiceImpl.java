package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.common.BusinessException;
import com.familyplan.entity.Family;
import com.familyplan.entity.UserInfo;
import com.familyplan.mapper.UserMapper;
import com.familyplan.service.IFamilyService;
import com.familyplan.service.IUserInfoService;
import com.familyplan.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserMapper, UserInfo> implements IUserInfoService, UserService {
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IFamilyService familyService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public Map<String, Object> login(String username, String password) {
        // 查询用户
        UserInfo user = getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "用户已被禁用");
        }
        
        // 更新最后登录时间
        updateLastLoginTime(username);
        
        // 返回用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("role", user.getRole());
        result.put("lastLoginTime", user.getLastLoginTime());
        
        log.info("用户登录成功: {}", username);
        return result;
    }
    
    @Override
    public UserInfo getUserByUsername(String username) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, username);
        UserInfo user = userMapper.selectOne(wrapper);
        
        if (user != null) {
            // 设置最后登录时间
            user.setLastLoginTime(user.getLastLoginTime());
        }
        
        return user;
    }
    
    @Override
    public void updateLastLoginTime(String username) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, username);
        
        UserInfo update = new UserInfo();
        update.setLastLoginTime(LocalDateTime.now());
        
        userMapper.update(update, wrapper);
    }
    
    /**
     * Spring Security用户详情加载
     * 多家庭改造: 登录账号为"家庭账号", 从 family 表加载
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Family family = familyService.getByUsername(username);

        if (family == null) {
            log.error("家庭账号不存在: {}", username);
            throw new UsernameNotFoundException("家庭账号不存在: " + username);
        }

        if (family.getStatus() != null && family.getStatus() == 0) {
            throw new UsernameNotFoundException("家庭账号已被禁用");
        }

        return new org.springframework.security.core.userdetails.User(
                family.getUsername(),
                family.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
