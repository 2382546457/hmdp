package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;


public interface IFollowService extends IService<Follow> {

    /**
     * 当前用户 关注/取关 followUserId
     * @param followUserId 被关注/取关的用户id
     * @param isFollow 关注/取关
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 当前用户是否已经关注 followUserId
     * @param followUserId
     * @return
     */
    Result isFollow(Long followUserId);


    /**
     * 求共同关注 当前登录用户与id的共同关注
     * @param id
     * @return
     */
    Result common(Long id);
}
