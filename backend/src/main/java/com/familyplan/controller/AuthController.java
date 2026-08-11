package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.dto.LoginRequest;
import com.familyplan.dto.LoginResponse;
import com.familyplan.entity.UserInfo;
import com.familyplan.security.JwtUtil;
import com.familyplan.service.IUserInfoService;
import com.familyplan.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private IUserInfoService userInfoService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            // Spring Security认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            // 生成token
            UserDetails userDetails = (UserDetails) userService.loadUserByUsername(request.getUsername());
            String token = jwtUtil.generateToken(userDetails);
            
            // 获取用户信息
            UserInfo user = userInfoService.getUserByUsername(request.getUsername());
            
            LoginResponse response = new LoginResponse(
                    user.getId(),
                    token,
                    user.getUsername(),
                    user.getNickname(),
                    user.getAvatar() != null ? user.getAvatar() : "",
                    user.getRole()
            );
            
            log.info("用户登录成功: {}", request.getUsername());
            return Result.success(response);
            
        } catch (AuthenticationException e) {
            log.error("登录失败: {}", e.getMessage());
            return Result.error(401, "用户名或密码错误");
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7);
            String username = jwtUtil.extractUsername(token);
            
            UserInfo user = userInfoService.getUserByUsername(username);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            data.put("role", user.getRole());
            data.put("lastLoginTime", user.getLastLoginTime());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return Result.error("获取用户信息失败");
        }
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
