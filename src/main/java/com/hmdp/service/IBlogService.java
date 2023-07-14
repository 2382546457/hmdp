package com.hmdp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IBlogService extends IService<Blog> {

    Result queryBolgById(Long id);

    /**
     * 点赞功能
     * @param bolgId 博客id
     * @param userId 用户id
     */
    void likeBlog(Long bolgId, Long userId) throws JsonProcessingException;

    /**
     * 查询当前登录用户是否点赞该博客
     * @return
     */
    public boolean isLike(Long userId, Long blogId);
}
