package com.sky.interceptor;

import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT拦截器抽象基类——模板方法模式消除代码重复
 */
@Slf4j
public abstract class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    protected JwtProperties jwtProperties;

    /** 获取请求头中token的key名 */
    protected abstract String getTokenName();

    /** 获取JWT签名密钥 */
    protected abstract String getSecretKey();

    /** 获取JWT Claims中用户ID字段名 */
    protected abstract String getUserIdClaim();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(getTokenName());

        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(getSecretKey(), token);
            Long userId = Long.valueOf(claims.get(getUserIdClaim()).toString());
            log.info("当前用户id：{}", userId);
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception ex) {
            response.setStatus(401);
            return false;
        }
    }
}
