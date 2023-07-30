package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hmdp.dto.FollowFeed;
import com.hmdp.dto.LikeDto;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.ScrollResult;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBolgById(Long id) {
        // 查询blog
        Blog blog = blogMapper.selectById(id);
        if (Objects.isNull(blog)) {
            return Result.fail("对应的blog不存在");
        }
        // 查询blog对应的用户
        User user = userMapper.selectById(blog.getUserId());
        boolean isLike = isLike(user.getId(), id);
        // 设置博客对应的用户的属性
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
        blog.setIsLike(isLike);
        return Result.ok(blog);
    }


    public boolean isLike(Long userId, Long blogId) {
        return !Objects.isNull(stringRedisTemplate.opsForZSet().score(RedisConstants.BLOG_LIKED_KEY + blogId, userId.toString())) ;
    }

    @Override
    public Result likes(Long blogId, int count) {
        // 查出来的是 userid
        Set<String> range = stringRedisTemplate.opsForZSet().range(RedisConstants.BLOG_LIKED_KEY + blogId, 0, (count - 1));
        if (range.isEmpty()) {
            return Result.ok(Lists.newArrayList());
        }
        List<Long> userIds = range.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userMapper.selectUserByLikeTime(userIds);
        List<UserDTO> userDTOS = users.stream().map(user -> {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            return userDTO;
        }).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) throws JsonProcessingException {
        Long id = UserHolder.getUser().getId();
        blog.setUserId(id);
        // 保存博客
        boolean save = save(blog);
        if (!save) {
            return Result.fail("保存失败!");
        }
        // 开始推送
        // 获取当前用户的所有粉丝
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowUserId, id);
        List<Long> followsID = followMapper.selectList(wrapper).stream().map(Follow::getId).collect(Collectors.toList());
        // 如果没有粉丝可以不用推送
        if (CollectionUtil.isEmpty(followsID)) {
            return Result.ok();
        }
        // 异步发送消息
        FollowFeed followFeed = new FollowFeed(followsID, blog.getId());
        rocketMQTemplate.asyncSend("hmdp:follow",
                new ObjectMapper().writeValueAsBytes(followFeed),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("推送信息发送成功! {}", sendResult);
                        log.info("user id : {}", id);
                        log.info("follows id : {}", followsID);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("点赞消息发送失败! {}", throwable.toString());
                        log.info("user id : {}", id);
                        log.info("follows id : {}", followsID);
                        throw new RuntimeException("点赞失败!");
                    }
                }
        );
        return Result.ok();


    }

    @Override
    public Result queryBlogOfFollow(Long maxTime, Integer offset) {
        // 1. 获取当前用户
        Long id = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + id;
        // 2. 查询收件箱，ZREVRANGEBYSCORE key max min limit offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, maxTime, offset, 5);
        if (CollectionUtils.isEmpty(typedTuples)) {
            return Result.ok();
        }

        // 3. 解析数据: blogId、minTime、offset
        List<Long> blogIds = new ArrayList<>(typedTuples.size());
        Long minTime = 0L;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            Long blogId = Long.valueOf(Objects.requireNonNull(typedTuple.getValue()));
            blogIds.add(blogId);
            minTime = Objects.requireNonNull(typedTuple.getScore()).longValue();
        }
        // 4. 根据id查询blog
        // 不能直接使用 listByIds, 因为现在的blog是有顺序的，为了避免顺序被破坏应该使用 order by field (在UserMapper.xml中例子)
        // 博客查出来了，还要查博客对应的用户信息、当前登录用户是否点赞
        List<Blog> blogs = blogMapper.selectByIdsOrdered(blogIds);
        blogs.forEach(blog -> {
            blog.setIsLike(isLike(id, blog.getId()));
        });
        // 5. 封装返回
        offset = 1;
        ScrollResult scrollResult = new ScrollResult(blogs, minTime, offset);
        return Result.ok(scrollResult);
    }

    @Override
    public void likeBlog(Long blogId, Long userId) throws JsonProcessingException {
        LikeDto likeDto = new LikeDto(blogId, userId);
        // 发消息队列
        rocketMQTemplate.asyncSend("hmdp:like",
                new ObjectMapper().writeValueAsBytes(likeDto),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("点赞信息发送成功! {}", sendResult);
                        log.info("user id : {}", userId);
                        log.info("blog id : {}", blogId);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("点赞消息发送失败! {}", throwable.toString());
                        log.info("user id : {}", userId);
                        log.info("blog id : {}", blogId);
                        throw new RuntimeException("点赞失败!");
                    }
                }
        );

    }

}
