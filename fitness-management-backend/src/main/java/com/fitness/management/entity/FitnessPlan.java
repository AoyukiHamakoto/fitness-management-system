package com.fitness.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健身计划表
 */
@Data
@TableName("fitness_plan")
public class FitnessPlan {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 计划名称 */
    private String planName;

    /** 计划说明 */
    private String planDesc;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 状态：0草稿 1进行中 2已完成 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
