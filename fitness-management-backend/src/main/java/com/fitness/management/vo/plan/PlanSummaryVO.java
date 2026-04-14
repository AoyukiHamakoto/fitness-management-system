package com.fitness.management.vo.plan;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进行中计划的摘要信息（列表/当前计划接口使用，不含每日明细）。
 */
@Data
public class PlanSummaryVO {

    private Long id;

    private String planName;

    private String planDesc;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 状态：0草稿 1进行中 2已完成 */
    private Integer status;

    private LocalDateTime createTime;
}
