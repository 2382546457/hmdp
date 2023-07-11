package com.hmdp.config;

import com.hmdp.interception.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName MVCConfig
 * @Description MVC配置
 * @Author 何
 * @Date 2023-06-13 12:12
 * @Version 1.0
 */
@Configuration
public class MVCConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 白名单
        List<String> writeList = new ArrayList<>();
        writeList.add("/user/code");
        writeList.add("/user/login");
        writeList.add("/blog/hot");
        writeList.add("/shop/**");
        writeList.add("/shop-type/**");
        writeList.add("/voucher/**");
        writeList.add("/upload/**");
        // 配置拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(redisTemplate)).addPathPatterns("/**").order(0);

        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(writeList)
                .order(1);
    }
}
