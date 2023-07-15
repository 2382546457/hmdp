package com.hmdp.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.LikeDto;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * @ClassName RocketMQLikeListener
 * @Description 点赞消息监听器
 * @Author 何
 * @Date 2023-07-14 19:50
 * @Version 1.0
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "likeConsume",
        topic = "hmdp",
        selectorExpression = "like"
)
public class RocketMQLikeListener implements RocketMQListener<MessageExt> {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BlogMapper blogMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        LikeDto likeDto = null;
        try {
            likeDto = new ObjectMapper().readValue(messageExt.getBody(), LikeDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 去Redis看当前用户是否已经点赞过
        Long blogId = likeDto.getBlogId();
        Long userId = likeDto.getUserId();
        Double score = stringRedisTemplate.opsForZSet().score(RedisConstants.BLOG_LIKED_KEY + blogId, userId.toString());
        // 存在，说明已经被点赞了
        if (!Objects.isNull(score)) {
            // 从Redis删掉
            Integer count = blogMapper.updateLikeCount(blogId, 1);
            if (count > 0) {
                stringRedisTemplate.opsForZSet().remove(RedisConstants.BLOG_LIKED_KEY + blogId, userId.toString());
            }
        } else {
            // 加到Redi 的 zset中
            Integer count = blogMapper.updateLikeCount(blogId, -1);
            if (count < 1) {
                stringRedisTemplate.opsForZSet().add(RedisConstants.BLOG_LIKED_KEY + blogId, String.valueOf(userId), System.currentTimeMillis());
            }
        }
    }
}
