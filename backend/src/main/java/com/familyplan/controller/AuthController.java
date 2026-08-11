package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.dto.LoginRequest;
import com.familyplan.dto.LoginResponse;
import com.familyplan.dto.RegisterRequest;
import com.familyplan.entity.Family;
import com.familyplan.entity.FamilyMember;
import com.familyplan.security.JwtUtil;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IFamilyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 多家庭改造: 家庭账号登录, 登录时校验"当前操作成员是否属于该家庭"
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private IFamilyService familyService;

    @Autowired
    private IFamilyMemberService memberService;

    /**
     * 家庭账号登录
     * 支持两种模式:
     * 1. 仅账号+密码: 校验家庭账号, 返回家庭信息+成员列表(前端展示成员选择)
     * 2. 账号+密码+memberId: 额外校验该成员属于该家庭, 校验通过后可直接进入系统
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. 校验家庭账号与密码
        Family family = familyService.authenticate(request.getUsername(), request.getPassword());

        // 2. 登录时校验: 指定的操作成员必须属于该家庭
        if (request.getMemberId() != null) {
            memberService.checkMemberBelongsToFamily(family.getId(), request.getMemberId());
        }

        // 3. 生成token(携带 familyId)
        UserDetails userDetails = userDetailsService.loadUserByUsername(family.getUsername());
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("familyId", family.getId());
        String token = jwtUtil.generateToken(extraClaims, userDetails);

        // 4. 查询该家庭成员列表(前端用于选择当前操作成员)
        List<FamilyMember> members = memberService.getListByFamilyId(family.getId());

        // 5. 组装响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setFamilyId(family.getId());
        response.setFamilyName(family.getFamilyName());
        response.setUsername(family.getUsername());
        response.setAvatar(family.getAvatar() != null ? family.getAvatar() : "");
        response.setMembers(members);
        if (request.getMemberId() != null) {
            FamilyMember current = memberService.getById(request.getMemberId());
            response.setCurrentMemberId(current.getId());
            response.setCurrentMemberName(current.getMemberName());
        }

        log.info("家庭账号登录成功: {} (familyId={})", request.getUsername(), family.getId());
        return Result.success(response);
    }

    /**
     * 获取当前家庭信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getFamilyInfo(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7);
            Long familyId = jwtUtil.extractFamilyId(token);

            Family family = familyService.getById(familyId);
            if (family == null) {
                return Result.error(401, "家庭不存在或登录已过期");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("familyId", family.getId());
            data.put("familyName", family.getFamilyName());
            data.put("username", family.getUsername());
            data.put("avatar", family.getAvatar() != null ? family.getAvatar() : "");

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取家庭信息失败: {}", e.getMessage());
            return Result.error("获取家庭信息失败");
        }
    }

    /**
     * 注册新家庭(新增家庭账号)
     * 注册成功自动创建户主成员并直接登录
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        // 1. 注册家庭(自动创建户主成员)
        Family family = familyService.register(
                request.getFamilyName(), request.getUsername(), request.getPassword(), request.getOwnerName());

        // 2. 注册即登录: 生成token
        UserDetails userDetails = userDetailsService.loadUserByUsername(family.getUsername());
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("familyId", family.getId());
        String token = jwtUtil.generateToken(extraClaims, userDetails);

        // 3. 查询该家庭成员列表
        List<FamilyMember> members = memberService.getListByFamilyId(family.getId());

        // 4. 组装响应(自动选中户主成员)
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setFamilyId(family.getId());
        response.setFamilyName(family.getFamilyName());
        response.setUsername(family.getUsername());
        response.setAvatar(family.getAvatar() != null ? family.getAvatar() : "");
        response.setMembers(members);
        if (!members.isEmpty()) {
            response.setCurrentMemberId(members.get(0).getId());
            response.setCurrentMemberName(members.get(0).getMemberName());
        }

        log.info("注册家庭成功并自动登录: {} (familyId={})", family.getUsername(), family.getId());
        return Result.success("注册成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("用户登出");
        return Result.success();
    }
}
