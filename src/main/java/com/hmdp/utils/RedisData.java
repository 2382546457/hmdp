package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 逻辑过期时间类
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
