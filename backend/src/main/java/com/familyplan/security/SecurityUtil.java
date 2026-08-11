package com.familyplan.security;

import com.familyplan.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类: 获取当前登录家庭信息
 */
public class SecurityUtil {

    /**
     * 获取当前家庭ID(未登录返回null)
     */
    public static Long getCurrentFamilyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof FamilyContext context) {
            return context.getFamilyId();
        }
        return null;
    }

    /**
     * 获取当前家庭ID(未登录时抛出异常)
     */
    public static Long getRequiredFamilyId() {
        Long familyId = getCurrentFamilyId();
        if (familyId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return familyId;
    }

    /**
     * 获取当前操作成员ID(请求头 X-Member-Id, 未选择成员返回null)
     */
    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof FamilyContext context) {
            return context.getMemberId();
        }
        return null;
    }
}
