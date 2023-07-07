package com.hmdp.service.impl;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService voucherService;
    @Autowired
    private RedisIDWorker idWorker;
    @Override
    public String secondKill(SeckillVoucher voucher) {
        if (voucher.getStock() < 1) {
            throw new RuntimeException("库存不足!");
        }
        // 修改优惠券库存
        boolean success = voucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucher.getVoucherId())
                .update();
        if (!success) {
            throw new RuntimeException("库存不足!");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(idWorker.nextID("order"));
        voucherOrder.setVoucherId(voucher.getVoucherId());
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        save(voucherOrder);
        return String.valueOf(voucherOrder.getId());
    }
}
