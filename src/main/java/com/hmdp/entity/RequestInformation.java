package com.hmdp.entity;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 请求信息
 * @author 大忽悠
 * @create 2023/2/6 10:55
 */
@Data
public class RequestInformation {
    /**
     * 限流key
     */
    private String key;
    /**
     * 限流时间
     */
    private int time;
    /**
     * time时间内最大请求资源次数
     */
    private int count;
    /**
     * 限流类型
     */
    private int limitType;
    /**
     * 请求的方法信息
     */
    private Method method;
    /**
     * 方法参数信息
     */
    private Object[] arguments;
    /**
     * 客户端IP地址
     */
    private String ip;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;

    public RequestInformation() {
    }
}
