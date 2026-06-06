package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import org.springframework.stereotype.Component;

/**
 * 管理端JWT拦截器——只需覆写3个配置项
 */
@Component
public class JwtTokenAdminInterceptor extends JwtTokenInterceptor {

    @Override
    protected String getTokenName() {
        return jwtProperties.getAdminTokenName();
    }

    @Override
    protected String getSecretKey() {
        return jwtProperties.getAdminSecretKey();
    }

    @Override
    protected String getUserIdClaim() {
        return JwtClaimsConstant.EMP_ID;
    }
}
