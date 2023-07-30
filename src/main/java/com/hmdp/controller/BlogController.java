package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;


@RestController
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    private IBlogService blogService;
    @Autowired
    private IUserService userService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) throws JsonProcessingException {
        return blogService.saveBlog(blog);
    }

    /**
     * 用户点赞
     * @param id 博客id
     * @return 结果集
     * @throws JsonProcessingException json序列化异常
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) throws JsonProcessingException {
        // 修改点赞数量
        Long userId = UserHolder.getUser().getId();
        blogService.likeBlog(id, userId);
        return Result.ok();
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 获取当前登录用户是否点赞这个blog
        records.forEach(blog ->{
            Long nowUser = UserHolder.getUser().getId();
            if (!Objects.isNull(nowUser)) {
                blogService.isLike(nowUser, blog.getId());
            }

        });
        return Result.ok(records);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBolgById(id);
    }

    /**
     * 给某一博客点赞的用户(取前五个)
     * @param blogId
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result likes(@PathVariable("id") Long blogId) {
        return blogService.likes(blogId, 5);

    }
}
