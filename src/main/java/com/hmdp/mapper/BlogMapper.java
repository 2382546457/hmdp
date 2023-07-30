package com.hmdp.mapper;

import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

    public Integer updateLikeCount(@Param("id") Long id, @Param("count") int count);

    List<Blog> selectByIdsOrdered(@Param("ids") List<Long> blogIds);
}
