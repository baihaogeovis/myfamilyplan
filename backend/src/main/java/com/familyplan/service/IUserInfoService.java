package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.UserInfo;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface IUserInfoService extends IService<UserInfo> {
    
    /**
     * 用户登录
     */
    Map<String, Object> login(String username, String password);
    
    /**
     * 根据用户名查询用户
     */
    UserInfo getUserByUsername(String username);
    
    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(String username);
}
