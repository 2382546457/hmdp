package com.hmdp.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.PasswordEncoder;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    public Result sendCode(String phone, HttpSession session) {
        // 生成验证码
        String code = RandomUtil.randomString(6);
        // 存入session
        session.setAttribute("code", code);
        log.debug("验证码: {}", code);
        // 返回
        return Result.ok();
    }

    @Override
    public Result login(String phone, String password, String code, HttpSession session) {
        // 1. 校验验证码
        String code1 = (String) session.getAttribute("code");
        if (Strings.isEmpty(code1) || !code1.equals(code)) {
            return Result.fail("验证码错误");
        }
        // 2.如果数据库中没有该用户，注册
        User user = query().eq("phone", phone).one();
        if (Objects.isNull(user)) {
            // 注册
            user = createUserWithPhone(phone);
        }
        session.setAttribute("user", user);

        return Result.ok();
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
