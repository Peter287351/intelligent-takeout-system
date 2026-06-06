package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import org.springframework.stereotype.Component;

/**
 * 用户端JWT拦截器——只需覆写3个配置项
 */
@Component
public class JwtTokenUserInterceptor extends JwtTokenInterceptor {

    @Override
    protected String getTokenName() {
        return jwtProperties.getUserTokenName();
    }

    @Override
    protected String getSecretKey() {
        return jwtProperties.getUserSecretKey();
    }

    @Override
    protected String getUserIdClaim() {
        return JwtClaimsConstant.USER_ID;
    }
}
