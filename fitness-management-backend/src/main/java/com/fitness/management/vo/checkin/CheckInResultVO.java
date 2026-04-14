package com.fitness.management.vo.checkin;

import lombok.Data;

/**
 * 打卡提交后的结果：记录主键、连续天数、计划完成进度等。
 */
@Data
public class CheckInResultVO {

    private Long punchRecordId;

    /** 当前连续打卡天数（日历日维度，Redis 维护） */
    private Integer currentStreakDays;

    /** 当前计划完成进度 0-100（已打卡明细条数 / 计划总明细条数） */
    private Integer progressPercent;

    /** 提示文案 */
    private String message;
}
