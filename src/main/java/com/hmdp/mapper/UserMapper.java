package com.hmdp.mapper;

import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<User> selectUserByLikeTime(@Param("ids") List<Long> userIds);


}
