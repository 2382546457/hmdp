package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIDWorker idWorker;
    @Override
    public Result secondKill(SeckillVoucher voucher) {
        if (voucher.getStock() < 1) {
            throw new RuntimeException("库存不足!");
        }
        // 修改优惠券库存
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucher.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足!");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(idWorker.nextID("order"));
        voucherOrder.setVoucherId(voucher.getVoucherId());
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        save(voucherOrder);
        return Result.ok(voucherOrder.getId());
    }
}
