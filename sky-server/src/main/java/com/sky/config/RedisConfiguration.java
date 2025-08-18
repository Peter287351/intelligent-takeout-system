package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置redis的连接工厂对象，然后这里工厂对象不需要创建，因为springboot-starter已经创建了,只需要通过Bean声明获取即可
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置redis key的序列化器,这里是字符串的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //设置redis Value的序列化器，使用GenericJackson2JsonRedisSerializer()来序列化
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }
}
