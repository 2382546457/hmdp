package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long nowUserId = UserHolder.getUser().getId();

        // 关注
        if (isFollow) {
            Follow follow = new Follow().setUserId(nowUserId).setFollowUserId(followUserId);
            save(follow);
        } else {
            // 取关
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getUserId, nowUserId).eq(Follow::getFollowUserId, followUserId);
            remove(wrapper);
        }

        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long nowUserId = UserHolder.getUser().getId();
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, nowUserId).eq(Follow::getFollowUserId, followUserId);
        Follow one = getOne(wrapper);
        return Result.ok(!Objects.isNull(one));
    }
}
