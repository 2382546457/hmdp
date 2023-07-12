package com.hmdp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀
     * @param seckillVoucher 优惠券
     * @return 生成的订单id
     */
    Result secondKill(SeckillVoucher seckillVoucher) throws JsonProcessingException;

    /**
     * 创建订单
     * @param voucher
     * @return
     */
    Result createVoucherOrder(SeckillVoucher voucher);
}
