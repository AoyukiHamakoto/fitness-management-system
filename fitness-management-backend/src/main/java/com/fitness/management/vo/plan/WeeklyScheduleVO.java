package com.fitness.management.vo.plan;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 按周组织的训练安排（论文计划解析结构中的周维度）。
 */
@Data
public class WeeklyScheduleVO {

    /** 第几周（从1开始） */
    private Integer weekIndex;

    /** 本周内的训练明细列表 */
    private List<PlanDetailVO> details = new ArrayList<>();
}
