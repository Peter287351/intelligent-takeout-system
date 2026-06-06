package com.sky.annotation;

import com.sky.enumeration.KeyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，基于Redis滑动窗口实现
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 限流key前缀 */
    String key();
    /** 窗口内最大请求数 */
    int limit();
    /** 时间窗口（秒） */
    int window();
    /** 限流触发时的提示信息 */
    String message() default "操作过于频繁，请稍后再试";
    /** 限流key类型：IP地址或用户ID */
    KeyType keyType() default KeyType.IP;
}
