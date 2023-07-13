package com.hmdp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;


public interface IBlogService extends IService<Blog> {

    Result queryBolgById(Long id);

    /**
     * 点赞功能
     * @param bolgId 博客id
     * @param userId 用户id
     * @return 是否成功
     */
    boolean like(Long bolgId, Long userId) throws JsonProcessingException;
}
