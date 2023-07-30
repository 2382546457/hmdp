package com.hmdp.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Transactional
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public Result sendCode(String phone, HttpSession session) {
        // 生成验证码
        String code = RandomUtil.randomString(6);
        // 存入session
        //  session.setAttribute("code", code);
        // 存入Redis。
        redisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("验证码: {}", code);
        return Result.ok();
    }

    @Override
    public Result login(String phone, String password, String code, HttpSession session) throws Exception {
        // 1. 校验验证码
        String code1 = (String) redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (Strings.isEmpty(code1) || !code1.equals(code)) {
            return Result.fail("验证码错误");
        }
        // 2.如果数据库中没有该用户，注册
        User user = query().eq("phone", phone).one();
        if (Objects.isNull(user)) {
            // 注册
            user = createUserWithPhone(phone);
        }

        UserDTO userDTO = new UserDTO(user.getId(), user.getNickName(), user.getIcon());
        Map<String, Object> userDtoMap = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue)-> {return fieldValue.toString();})
        );
        String token = JWTUtils.generateToken(userDtoMap);
        // key: token
        // value: Map
        // 存入Redis后设置有效期
        redisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userDtoMap);
        redisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    @Override
    public boolean sign() {
        // 拼接key
        // userId:year:month
        Long userId = UserHolder.getUser().getId();

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonth().getValue();
        int day = now.getDayOfMonth();

        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + year + ":" + month;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setBit(key, day - 1, true));


    }

    @Override
    public Integer maxSignCount() {
        // 1. 获取本月截止到今天的签到记录
        Long userId = UserHolder.getUser().getId();
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonth().getValue();
        int day = now.getDayOfMonth();

        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + year + ":" + month;
        List<Long> bits = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if (CollectionUtil.isEmpty(bits)) {
            return 0;
        }
        Long num = bits.get(0);
        if (Objects.isNull(num) || num.longValue() == 0) {
            return 0;
        }
        // 2. 计算连续1最多的。 011110111 -> 最多4个连续的1
        int maxCount = 0;
        int count = 0;
        for (int i = 0; i < 32; i++) {
            if (((num >> i) & 1) == 0) {
                count = 0;
                continue;
            } else {
                count++;
            }
            maxCount = Math.max(maxCount, count);
        }
        return maxCount;
    }

    /**
     * 根据手机号创建用户
     *
     * @param phone 手机号
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 数据库保存用户
        save(user);
        return user;
    }

}
