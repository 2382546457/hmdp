package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;
    /**
     * 关注
     * @param followUserId
     * @param isFollow
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 当前登录用户是否已经关注 followUserId
     * @param followUserId 被关注人的id
     * @return 统一返回结果类
     */
    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }


    /**
     * 求共同关注 当前登录用户与id的共同关注
     * @param id
     * @return
     */
    @GetMapping("/follow/common/{id}")
    public Result common(@PathVariable("id") Long id) {
        return followService.common(id);
    }
}
