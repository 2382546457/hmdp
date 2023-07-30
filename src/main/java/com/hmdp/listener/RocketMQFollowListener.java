package com.hmdp.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.FollowFeed;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName RocketMQFollowListener
 * @Description 将blog信息推送到用户收件箱中
 * @Author 何
 * @Date 2023-07-30 13:58
 * @Version 1.0
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "followConsume",
        topic = "hmdp",
        selectorExpression = "follow"
)
public class RocketMQFollowListener implements RocketMQListener<MessageExt> {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void onMessage(MessageExt messageExt) {
        FollowFeed followFeed = null;
        try {
            followFeed = new ObjectMapper().readValue(messageExt.getBody(), FollowFeed.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Long blogId = followFeed.getBlogId();
        List<Long> followIDs = followFeed.getFollowID();
        followIDs.stream().forEach(followID -> {
            // 往粉丝的收件箱中存储消息，当然要获取粉丝的id形成key
            String key = RedisConstants.FOLLOW_FEED + followID;
            stringRedisTemplate.opsForZSet().add(key, blogId.toString(), System.currentTimeMillis());
        });
    }
}
