package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * @ClassName LoginInterceptor
 * @Description 登录拦截器
 * @Author 何
 * @Date 2023-06-13 12:07
 * @Version 1.0
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            // 未授权
            response.setStatus(401);
            return false;
        }
        UserHolder.saveUser(new UserDTO(user.getId(), user.getNickName(), user.getIcon()));
        return true;
    }

    /**
     * 请求结束的时候将user移出ThreadLocal
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
