package com.fitness.management.vo.plan;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 大模型返回并解析后的完整计划内容（根对象）。
 */
@Data
public class PlanContentVO {

    /** 计划名称 */
    private String planName;

    /** 计划总体说明 */
    private String planDesc;

    /** 按周划分的训练安排 */
    private List<WeeklyScheduleVO> weeks = new ArrayList<>();
}
