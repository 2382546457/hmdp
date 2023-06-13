package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public interface IUserService extends IService<User> {

    /**
     * 发送验证码
     * @param phone 目标手机号
     * @param session 验证码存入session
     * @return 是否成功
     */
    public Result sendCode(String phone, HttpSession session);

    /**
     * 用户登录
     * @param phone 手机号
     * @param password 密码
     * @param code 验证码
     * @param session session
     * @return 是否陈成功
     */
    Result login(String phone, String password, String code, HttpSession session);


}
