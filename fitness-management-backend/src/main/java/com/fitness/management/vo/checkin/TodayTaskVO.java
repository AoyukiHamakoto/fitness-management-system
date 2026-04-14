package com.fitness.management.vo.checkin;

import lombok.Data;

/**
 * 当前计划「今日」应完成的一条训练任务（按计划开始日推算的第 N 天）。
 */
@Data
public class TodayTaskVO {

    private Long planDetailId;

    /** 计划内第几天 */
    private Integer dayIndex;

    private String exerciseName;

    private Integer durationMinutes;

    private Integer sortOrder;

    /** 当日该任务是否已打卡 */
    private Boolean checkedIn;
}
