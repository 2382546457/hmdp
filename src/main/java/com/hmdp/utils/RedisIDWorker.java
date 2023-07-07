package com.hmdp.utils;

import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

/**
 * @ClassName RedisIDWorker
 * @Description 全局ID生成器
 * @Author 何
 * @Date 2023-06-14 10:53
 * @Version 1.0
 */
@Component
public class RedisIDWorker {
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final long BEGIN_TIMESTAMP = 1672531200;
    private static final long COUNT_BITS = 32L;

    /**
     * 生成全局id
     *
     * @param prefix 业务前缀
     * @return long类型的全局id
     */
    public long nextID(String prefix) {
        assert !StringUtil.isNullOrEmpty(prefix);
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
        // 同一个业务不要使用同一个key.尽量减小范围，将id生成缩小到"天"
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = redisTemplate.opsForValue().increment("icrID:" + prefix + ":" + date);
        // 现在将时间戳与count拼接
        return timeStamp << COUNT_BITS | count;
    }



}
