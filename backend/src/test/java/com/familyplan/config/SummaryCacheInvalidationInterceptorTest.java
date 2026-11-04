package com.familyplan.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryCacheInvalidationInterceptorTest {

    @Test
    void evictsAfterSuccessfulMoneyWrites() {
        assertTrue(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/api/income", 200, null));
        assertTrue(SummaryCacheInvalidationInterceptor.shouldEvict("put", "/api/expense/12", 200, null));
        assertTrue(SummaryCacheInvalidationInterceptor.shouldEvict("DELETE", "/api/budget/3/", 204, null));
        assertTrue(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/api/approval/9/approve", 200, null));
        assertTrue(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/member", 200, null));
        assertTrue(SummaryCacheInvalidationInterceptor.shouldEvict("PUT", "/api/fixed-expense", 200, null));
    }

    @Test
    void leavesCacheAloneForReadsExportsAndFailedWrites() {
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("GET", "/api/dashboard/overview", 200, null));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("GET", "/api/statistics/monthly", 200, null));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("GET", "/api/export/income", 200, null));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/api/auth/login", 200, null));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/api/reward/earn", 200, null));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/api/income", 500, null));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict("POST", "/api/income", 200, new RuntimeException("rolled back")));
        assertFalse(SummaryCacheInvalidationInterceptor.shouldEvict(null, "/api/income", 200, null));
    }
}
