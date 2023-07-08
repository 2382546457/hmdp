package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.ApplicationContextUtil;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIDWorker idWorker;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    public Result secondKill(SeckillVoucher voucher) {
        // 判断库存是否充足
        if (voucher.getStock() < 1) {
            throw new RuntimeException("库存不足!");
        }

        Long userId = UserHolder.getUser().getId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            return Result.fail("不许重复下单");
        }
        try {
            IVoucherOrderService bean = (IVoucherOrderService) ApplicationContextUtil.getBean("voucherOrderServiceImpl");
            return bean.createVoucherOrder(voucher);
        } finally {
            lock.unlock();
        }

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
