package com.fitness.management.vo.plan;

import lombok.Data;

/**
 * 单日训练条目（与 plan_detail 表及大模型 JSON 条目对齐）。
 */
@Data
public class PlanDetailVO {

    /** 计划内第几天（从1开始，全周期连续编号） */
    private Integer dayIndex;

    /** 训练动作或项目名称 */
    private String exerciseName;

    /** 建议时长（分钟） */
    private Integer durationMinutes;

    /** 同一天内动作排序，升序 */
    private Integer sortOrder;
}
