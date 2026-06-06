package com.sky.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类——统一封装常用操作，替代散落的RedisTemplate直接调用
 */
@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 写入缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        String json = JSON.toJSONString(value);
        redisTemplate.opsForValue().set(key, json, timeout, unit);
    }

    /**
     * 写入缓存（永不过期）
     */
    public void set(String key, Object value) {
        String json = JSON.toJSONString(value);
        redisTemplate.opsForValue().set(key, json);
    }

    /**
     * 读取缓存（自动JSON反序列化）
     */
    public <T> T get(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.warn("Redis反序列化失败: key={}, json={}", key, json);
            return null;
        }
    }

    /**
     * 读取缓存列表（自动JSON反序列化）
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.warn("Redis列表反序列化失败: key={}, json={}", key, json);
            return null;
        }
    }

    /**
     * 删除key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * SCAN游标模糊删除——替代KEYS命令，避免阻塞Redis
     */
    public void deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();
        redisTemplate.executeWithStickyConnection((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                }
            }
            return null;
        });
    }

    /**
     * 分布式锁——尝试加锁
     */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    /**
     * 分布式锁——释放锁
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
