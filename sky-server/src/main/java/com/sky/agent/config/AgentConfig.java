package com.sky.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Agent 配置类
 * MiMoClient 通过 @Component + @Value 自动配置，无需额外 Bean
 */
@Configuration
@EnableAsync
public class AgentConfig {
}
