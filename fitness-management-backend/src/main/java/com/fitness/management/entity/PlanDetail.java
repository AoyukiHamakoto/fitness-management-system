package com.fitness.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计划详情表
 */
@Data
@TableName("plan_detail")
public class PlanDetail {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属计划ID */
    private Long planId;

    /** 计划内第几天（从1开始） */
    private Integer dayIndex;

    /** 训练项目名称 */
    private String exerciseName;

    /** 建议时长（分钟） */
    private Integer durationMinutes;

    /** 当日动作排序，升序 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;
}
