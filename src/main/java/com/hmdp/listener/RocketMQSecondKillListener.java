package com.hmdp.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * @ClassName RocketMQSecondKillListener
 * @Description 消费者: 秒杀业务
 * @Author 何
 * @Date 2023-07-12 12:02
 * @Version 1.0
 */
@Slf4j
@Component
@RocketMQMessageListener(
        // 消费者组
        consumerGroup = "SecondKillConsume",
        // topic
        topic = "hmdp",
        // tag
        selectorExpression = "SecondKill"
)
public class RocketMQSecondKillListener implements RocketMQListener<MessageExt> {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private IVoucherOrderService iVoucherOrderService;
    @Override
    public void onMessage(MessageExt messageExt) {
        log.info("接收到消息");
        log.info("message id : {}", messageExt.getMsgId());
        VoucherOrder voucherOrder = null;
        try {
            voucherOrder = new ObjectMapper().readValue(messageExt.getBody(), VoucherOrder.class);
            log.info("message Body : {}", voucherOrder);
        } catch (IOException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }

        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            // 抛出异常，生产者的回调函数会把Redis里的库存加回来
            throw new RuntimeException("库存不足");
        }
        voucherOrder.setCreateTime(LocalDateTime.now());
        iVoucherOrderService.save(voucherOrder);
    }
}
