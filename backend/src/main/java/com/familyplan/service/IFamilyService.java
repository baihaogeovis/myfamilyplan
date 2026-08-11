package com.familyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.Family;

/**
 * 家庭服务接口
 */
public interface IFamilyService extends IService<Family> {

    /**
     * 根据家庭账号查询家庭
     */
    Family getByUsername(String username);

    /**
     * 校验家庭账号密码
     *
     * @param username 家庭账号
     * @param password 明文密码
     * @return 校验通过的家庭
     */
    Family authenticate(String username, String password);

    /**
     * 注册新家庭(新增家庭账号)
     *
     * @param familyName 家庭名称
     * @param username   家庭登录账号
     * @param password   明文密码
     * @param ownerName  户主姓名(可为空,默认取家庭名称)
     * @return 注册成功的家庭
     */
    Family register(String familyName, String username, String password, String ownerName);
}
