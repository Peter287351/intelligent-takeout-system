package com.sky.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
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

        //修改后的序列化器
        //使用Jackson2JsonRedisSerializer替换GenericJackson2JsonRedisSerializer
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);

        //配置ObjectMapper以支持Java 8时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //注册JavaTimeModule以支持LocalDateTime等时间类型
        objectMapper.registerModule(new JavaTimeModule());
        //禁用将日期写为时间戳的特性
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        serializer.setObjectMapper(objectMapper);

        //设置redis value的序列化器
        redisTemplate.setValueSerializer(serializer);

        //设置hash的key和value序列化器
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
