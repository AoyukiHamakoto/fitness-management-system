package com.fitness.management.service;

import com.fitness.management.dto.plan.PlanGenerateDto;
import com.fitness.management.vo.plan.PlanFullVO;
import com.fitness.management.vo.plan.PlanSummaryVO;

/**
 * AI 个性化健身计划：生成、查询当前进行中计划、计划详情。
 */
public interface PlanGenerateService {

    /**
     * 根据体征与偏好调用大模型生成计划并落库，返回新计划主键。
     */
    Long generatePlan(Long userId, PlanGenerateDto dto);

    /**
     * 当前用户进行中的计划（status=1，按创建时间倒序取一条）。
     */
    PlanSummaryVO getCurrentActivePlan(Long userId);

    /**
     * 计划主表 + 明细（校验归属当前用户）。
     */
    PlanFullVO getPlanDetail(Long userId, Long planId);
}
