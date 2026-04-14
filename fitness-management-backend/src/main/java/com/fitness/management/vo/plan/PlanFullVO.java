package com.fitness.management.vo.plan;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 计划详情接口返回：计划主信息 + 明细列表。
 */
@Data
public class PlanFullVO {

    private Long id;

    private Long userId;

    private String planName;

    private String planDesc;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 状态：0草稿 1进行中 2已完成 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PlanDetailVO> details = new ArrayList<>();
}
