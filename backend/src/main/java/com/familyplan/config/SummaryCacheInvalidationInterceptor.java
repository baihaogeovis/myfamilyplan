package com.familyplan.config;

import com.familyplan.security.SecurityUtil;
import com.familyplan.support.FamilySummaryCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 收支、预算、审批、成员、固定支出写成功后，丢掉这一户的汇总缓存。
 * 读接口和登录、导出、奖励不触发失效。
 */
@Component
public class SummaryCacheInvalidationInterceptor implements HandlerInterceptor {

    static final Pattern SUMMARY_WRITE = Pattern.compile(
            "^(?:/api)?/(?:income|expense|budget|approval|member|fixed-expense)(?:/.*)?$");

    private final FamilySummaryCache cache;

    public SummaryCacheInvalidationInterceptor(FamilySummaryCache cache) {
        this.cache = cache;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (shouldEvict(request.getMethod(), request.getRequestURI(), response.getStatus(), ex)) {
            cache.evictFamily(SecurityUtil.getCurrentFamilyId());
        }
    }

    static boolean shouldEvict(String method, String requestUri, int status, Exception error) {
        if (error != null || status >= 400 || method == null || requestUri == null) {
            return false;
        }
        String normalized = method.toUpperCase(Locale.ROOT);
        if (!"POST".equals(normalized) && !"PUT".equals(normalized) && !"DELETE".equals(normalized)) {
            return false;
        }
        String path = requestUri;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return SUMMARY_WRITE.matcher(path).matches();
    }
}
