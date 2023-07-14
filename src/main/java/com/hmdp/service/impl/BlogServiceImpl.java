package com.hmdp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.LikeDto;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;


@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public Result queryBolgById(Long id) {
        // 查询blog
        Blog blog = blogMapper.selectById(id);
        if (Objects.isNull(blog)) {
            return Result.fail("对应的blog不存在");
        }
        // 查询blog对应的用户
        User user = userMapper.selectById(blog.getUserId());
        // 设置用户的属性
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());

        return Result.ok(blog);
    }

    @Override
    public boolean like(Long blogId, Long userId) throws JsonProcessingException {
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
                    }
                }
        );
        return false;
    }

}
