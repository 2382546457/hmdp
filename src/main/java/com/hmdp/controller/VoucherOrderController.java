package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author 何
 * 优惠券下单
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private IVoucherOrderService voucherOrderService;
    @Autowired
    private IVoucherService voucherService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 前端传来的ID不存在
        if (Objects.isNull(seckillVoucher)) {
            return Result.fail("没有这个优惠券!");
        }
        // 时间不对劲
        LocalDateTime now = LocalDateTime.now();
        if (seckillVoucher.getBeginTime().isAfter(now)) {
            return Result.fail("活动还未开始，活动开始时间：" + seckillVoucher.getBeginTime());
        }
        if (seckillVoucher.getEndTime().isBefore(now)) {
            return Result.fail("活动已经结束！活动结束时间：" + seckillVoucher.getEndTime());
        }

        // 开始秒杀
        String orderID = voucherOrderService.secondKill(seckillVoucher);

        return Result.ok(orderID);
    }
}
