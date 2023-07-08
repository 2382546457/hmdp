package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName RedissonConfig
 * @Description Redisson配置
 * @Author 何
 * @Date 2023-07-08 13:04
 * @Version 1.0
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://47.109.60.208:6379").setPassword("@HWF20020622456@");
        return Redisson.create(config);
    }
}
