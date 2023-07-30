package com.hmdp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName ScrollResult
 * @Description 滚动分页
 * @Author 何
 * @Date 2023-07-30 15:45
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScrollResult {
    /**
     * 结果集
     */
    private List<?> list;

    /**
     * 最小时间
     */
    private Long minTime;

    /**
     * 偏移量
     */
    private Integer offset;
}
