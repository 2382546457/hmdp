package com.hmdp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIDWorker idWorker;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 加载lua脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("sec.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result secondKill(SeckillVoucher voucher) throws JsonProcessingException {
//        // 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            throw new RuntimeException("库存不足!");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock();
//        if (!isLock) {
//            return Result.fail("不许重复下单");
//        }
//        try {
//            IVoucherOrderService bean = (IVoucherOrderService) ApplicationContextUtil.getBean("voucherOrderServiceImpl");
//            return bean.createVoucherOrder(voucher);
//        } finally {
//            lock.unlock();
//        }
        Long userId = UserHolder.getUser().getId();
        Long voucherId = voucher.getVoucherId();

        // 执行lua脚本, 判断结果是否为0
        int result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        ).intValue();


        // 结果不为0，不能下单
        if (result == 1) {
            return Result.fail("库存不足!");
        }
        if (result == 2) {
            return Result.fail("不能重复下单!");
        }
        // 结果为0，可以下单
        long orderId = idWorker.nextID(RedisConstants.ORDER);
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(orderId);

       rocketMQTemplate.asyncSend("hmdp:SecondKill",
               new ObjectMapper().writeValueAsBytes(voucherOrder),
               new SendCallback() {
                   @Override
                   public void onSuccess(SendResult sendResult) {
                       log.info("订单id: {}, 购买人: {} 消息发送成功.", orderId, userId);
                   }
                   @Override
                   public void onException(Throwable throwable) {
                       log.info("订单id: {}, 购买人: {} 消息发送失败或者库存不足.", orderId, userId);
                       Long increment = stringRedisTemplate.opsForValue().increment(RedisConstants.SECKILL_STOCK + voucherId, 1L);
                       Long remove = stringRedisTemplate.opsForSet().remove(RedisConstants.SECKILL_ORDER + voucherId, userId);
                       log.info("将下单信息从Redis删掉...");
                       log.info("增加库存是否成功:{}", increment);
                       log.info("删除用户购买信息是否成功:{}", remove);
                   }
               });

       return Result.ok(orderId);
    }

    @Transactional
    public Result createVoucherOrder(SeckillVoucher voucher) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("voucher_id", voucher.getVoucherId()).eq("user_id", userId).count();
        if (count > 0) {
            return Result.fail("您已经购买过一次，不能重复购买!");
        }

        // 修改优惠券库存
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucher.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足!");
        }
        // 下单完成
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(idWorker.nextID("order"));
        voucherOrder.setVoucherId(voucher.getVoucherId());
        voucherOrder.setUserId(userId);
        save(voucherOrder);
        return Result.ok(voucherOrder.getId());
    }
}
