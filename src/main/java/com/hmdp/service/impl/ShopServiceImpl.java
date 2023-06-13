package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Transactional
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate redisTemplate;



    @Override
    public Result queryById(Long id) {
        // 1. 从Redis查询缓存
        String s = redisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);

        if (!Objects.isNull(s) && !Strings.isEmpty(s)) {
            Shop shop = JSONUtil.toBean(s, Shop.class);
            return Result.ok(shop);
        }
        if (!Objects.isNull(s) && s.length() == 0) {
            return Result.fail("没有这个店铺");
        }
        // 2. 查询数据库库
        Shop shop = getById(id);
        if (Objects.isNull(shop)) {
            // 缓存空值,时间两分钟
            redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("未找到id为" + id + "的店铺");
        }
        String json = JSONUtil.toJsonStr(shop);
        redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, json, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
    }

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (Objects.isNull(id)) {
            return Result.fail("shop的id不能为空");
        }
        // 先更新数据库，再删缓存
        boolean b = updateById(shop);
        if (b) {
            redisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        }
        return null;
    }
}
