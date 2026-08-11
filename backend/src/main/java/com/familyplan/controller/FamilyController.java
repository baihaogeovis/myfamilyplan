package com.familyplan.controller;

import com.familyplan.common.BusinessException;
import com.familyplan.common.Result;
import com.familyplan.entity.Family;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IFamilyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 家庭管理控制器(管理当前登录家庭的信息)
 */
@Slf4j
@RestController
@RequestMapping("/family")
public class FamilyController {

    @Autowired
    private IFamilyService familyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取当前家庭信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Family family = getCurrentFamily();
        Map<String, Object> data = new HashMap<>();
        data.put("familyId", family.getId());
        data.put("familyName", family.getFamilyName());
        data.put("username", family.getUsername());
        data.put("avatar", family.getAvatar() != null ? family.getAvatar() : "");
        data.put("remark", family.getRemark() != null ? family.getRemark() : "");
        return Result.success(data);
    }

    /**
     * 更新当前家庭信息(家庭名称/头像/备注)
     */
    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody Family req) {
        Family family = getCurrentFamily();
        if (StringUtils.hasText(req.getFamilyName())) {
            family.setFamilyName(req.getFamilyName().trim());
        }
        family.setAvatar(req.getAvatar());
        family.setRemark(req.getRemark());
        familyService.updateById(family);
        log.info("更新家庭信息成功: familyId={}", family.getId());
        return Result.success();
    }

    /**
     * 修改家庭登录密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        Family family = getCurrentFamily();
        if (!StringUtils.hasText(oldPassword) || !passwordEncoder.matches(oldPassword, family.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }

        family.setPassword(passwordEncoder.encode(newPassword));
        familyService.updateById(family);
        log.info("修改家庭密码成功: familyId={}", family.getId());
        return Result.success();
    }

    private Family getCurrentFamily() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        Family family = familyService.getById(familyId);
        if (family == null) {
            throw new BusinessException(401, "家庭不存在或登录已过期");
        }
        return family;
    }
}
