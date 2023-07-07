package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
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
    public Result queryById(Long id) throws InterruptedException {
        Shop shop = queryWithMutex(id);
        if (Objects.isNull(shop)) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    // 使用互斥锁解决缓存穿透
    public Shop queryWithMutex(Long id) throws InterruptedException {
        // 1. 从Redis查询缓存
        String s = redisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);

        if (!Objects.isNull(s) && !Strings.isEmpty(s)) {
            return JSONUtil.toBean(s, Shop.class);

        }
        if (!Objects.isNull(s) && s.length() == 0) {
            return null;
        }
        Shop shop = null;
        // 实现缓存重建
        // 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        // 判断获取锁是否成功
        // 失败，休眠且重试
        try {
            if (!tryLock(lockKey)) {
                Thread.sleep(500);
                return queryWithMutex(id);
            }
            // 成功，先再次查询数据库做DoubleCheck,根据id查询数据库
            String doubleCheck = redisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
            if (!Objects.isNull(s) && !Strings.isEmpty(s)) {
                return JSONUtil.toBean(s, Shop.class);
            }
            shop = getById(id);
            Thread.sleep(200);
            if (Objects.isNull(shop)) {
                // 缓存空值,时间两分钟
                redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            String json = JSONUtil.toJsonStr(shop);
            redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, json, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            // 释放互斥锁
            unlock(lockKey);
        }
        return shop;
    }

//    // 有缓存穿透问题的代码
//    public Shop queryWithPassThrough(Long id) {
//        // 1. 从Redis查询缓存
//        String s = redisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
//
//        if (!Objects.isNull(s) && !Strings.isEmpty(s)) {
//            return JSONUtil.toBean(s, Shop.class);
//
//        }
//        if (!Objects.isNull(s) && s.length() == 0) {
//            return null;
//        }
//        // 2. 查询数据库库
//        Shop shop = getById(id);
//        if (Objects.isNull(shop)) {
//            // 缓存空值,时间两分钟
//            redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        String json = JSONUtil.toJsonStr(shop);
//        redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, json, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        return shop;
//    }


    private boolean tryLock(String key) {
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // 手动拆箱，防止出现空指针.
        return BooleanUtil.isTrue(aBoolean);
    }
    private void unlock(String key) {
        redisTemplate.delete(key);
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
