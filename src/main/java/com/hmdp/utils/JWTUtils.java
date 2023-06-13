package com.hmdp.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class JWTUtils {
    // 签名
    private static final String SIGN = "234as5@4123";

    // 算法
    private static final Map<String, Object> map = new HashMap<>();

    /**
     * 根据Map生成token
     * @param payload
     * @return
     */
    public static String generateToken(Map<String,Object> payload) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.HOUR, 30);

        String token = JWT.create().withHeader(map) // 头
                .withPayload(payload) // 负载
                .withExpiresAt(instance.getTime()) // 设置过期时间。
                .sign(Algorithm.HMAC256(SIGN));// 签名，使用算法生成。

        return token;
    }


    /**
     * 验证token,如果token有错，直接抛出异常了。
     * @param token token
     */
    public static void  verify(String token) {
        JWT.require(Algorithm.HMAC256(SIGN)).build().verify(token);
    }

    /**
     * 获取token信息
     * @param token token
     * @return 信息
     */
    public static Map<String, Claim> getClaims(String token) {
        Map<String, ?> map = new HashMap<>();
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(SIGN)).build().verify(token);
        Map<String, Claim> claims = decodedJWT.getClaims();
        return claims;
    }


}