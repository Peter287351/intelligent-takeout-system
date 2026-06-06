package com.sky.aspect;

import com.sky.annotation.RateLimit;
import com.sky.enumeration.KeyType;
import com.sky.context.BaseContext;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 接口限流切面，基于Redis滑动窗口 + Lua脚本实现原子限流
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private HttpServletRequest request;

    private final DefaultRedisScript<Long> redisScript;

    public RateLimitAspect() {
        String luaScript =
                "local key = KEYS[1]\n" +
                "local limit = tonumber(ARGV[1])\n" +
                "local window = tonumber(ARGV[2])\n" +
                "local now = tonumber(ARGV[3])\n" +
                "local member = ARGV[4]\n" +
                "redis.call('ZREMRANGEBYSCORE', key, 0, now - window)\n" +
                "local count = redis.call('ZCARD', key)\n" +
                "if count < limit then\n" +
                "    redis.call('ZADD', key, now, member)\n" +
                "    redis.call('EXPIRE', key, window)\n" +
                "    return 1\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        redisScript = new DefaultRedisScript<>(luaScript, Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构建限流key
        String identifier;
        if (rateLimit.keyType() == KeyType.IP) {
            identifier = getClientIP();
        } else {
            identifier = BaseContext.getCurrentId().toString();
        }

        String redisKey = "rate_limit:" + rateLimit.key() + ":" + identifier;
        long now = System.currentTimeMillis();
        String member = now + "_" + ThreadLocalRandom.current().nextInt(10000);

        Long result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(redisKey),
                String.valueOf(rateLimit.limit()),
                String.valueOf(rateLimit.window() * 1000L),
                String.valueOf(now),
                member
        );

        if (result != null && result == 0) {
            log.warn("接口限流触发：key={}, limit={}/{}/{}s", redisKey, rateLimit.limit(), rateLimit.limit(), rateLimit.window());
            return Result.error(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 获取客户端真实IP，优先从代理头获取
     */
    private String getClientIP() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
