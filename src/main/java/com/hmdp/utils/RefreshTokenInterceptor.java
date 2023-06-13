package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.hmdp.dto.UserDTO;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName RefreshTokenInterceptor
 * @Description 刷新token的拦截器,不拦截任何请求
 * @Author 何
 * @Date 2023-06-13 14:54
 * @Version 1.0
 */

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate redisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("authorization");
        // 没toke直接拦截
        if (Strings.isEmpty(token)) {
            return true;
        }
        // Request Header里有token，看看Redis里有没有
        Map<Object, Object> userMap = redisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);

        if (CollectionUtil.isEmpty(userMap)) {
            return true;
        }
        redisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        return true;


    }
}
