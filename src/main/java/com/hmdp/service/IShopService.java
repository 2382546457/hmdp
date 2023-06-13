package com.hmdp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

@Service
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查找对应的店铺
     * @param id id
     * @return
     */
    Result queryById(Long id) throws JsonProcessingException;

    /**
     * 更新shop
     * @param shop
     * @return
     */
    Result update(Shop shop);
}
