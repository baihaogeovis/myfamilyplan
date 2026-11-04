package com.familyplan.config;

import com.familyplan.support.FamilySummaryCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * 仪表盘、月报、固定支出总览共用一份进程内缓存。
 */
@Configuration
public class SummaryCacheConfig {

    @Bean
    public FamilySummaryCache familySummaryCache(
            @Value("${familyplan.summary-cache.ttl-seconds:30}") long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("familyplan.summary-cache.ttl-seconds must be positive");
        }
        return new FamilySummaryCache(Duration.ofSeconds(ttlSeconds));
    }

    @Bean
    public WebMvcConfigurer summaryCacheWebMvc(SummaryCacheInvalidationInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
