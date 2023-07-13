package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName LikeDto
 * @Description 消息队列发送的消息
 * @Author 何
 * @Date 2023-07-13 13:41
 * @Version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeDto {
    private Long blogId;
    private Long userId;
}
