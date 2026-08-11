package com.familyplan.security;

import com.familyplan.service.IFamilyMemberService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 解析 token 中的家庭ID, 并统一校验请求头 X-Member-Id 的成员归属
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private IFamilyMemberService memberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String username;

        try {
            username = jwtUtil.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    // 解析家庭ID
                    Claims claims = jwtUtil.extractAllClaims(jwt);
                    Long familyId = claims.get("familyId", Long.class);

                    // 解析并校验当前操作成员归属
                    Long memberId = null;
                    String memberIdHeader = request.getHeader("X-Member-Id");
                    if (StringUtils.hasText(memberIdHeader)) {
                        try {
                            memberId = Long.valueOf(memberIdHeader);
                        } catch (NumberFormatException ignored) {
                            memberId = null;
                        }
                    }
                    if (familyId != null && memberId != null) {
                        try {
                            memberService.checkMemberBelongsToFamily(familyId, memberId);
                        } catch (RuntimeException e) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"成员不存在或不属于当前家庭\"}");
                            return;
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new FamilyContext(familyId, memberId));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("JWT认证失败: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
