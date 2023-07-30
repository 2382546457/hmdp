package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName FollowFeed
 * @Description Feed流
 * @Author 何
 * @Date 2023-07-30 13:50
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowFeed {
    /**
     * 收件人id，即所有粉丝的id
     */
    private List<Long> followID;
    /**
     * 博客id
     */
    private Long blogId;
}
