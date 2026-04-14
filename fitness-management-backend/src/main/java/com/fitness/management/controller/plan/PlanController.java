package com.fitness.management.controller.plan;

import com.fitness.management.common.Result;
import com.fitness.management.dto.plan.PlanGenerateDto;
import com.fitness.management.security.CurrentUserId;
import com.fitness.management.security.RequireAuth;
import com.fitness.management.service.PlanGenerateService;
import com.fitness.management.vo.plan.PlanFullVO;
import com.fitness.management.vo.plan.PlanSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健身计划：AI 生成、当前进行中计划、计划详情（JWT 由 {@link RequireAuth} 与拦截器统一校验）。
 */
@RestController
@RequestMapping("/plans")
@RequireAuth
@RequiredArgsConstructor
@Validated
public class PlanController {

    private final PlanGenerateService planGenerateService;

    /**
     * 根据体征与偏好生成个性化计划并落库。
     */
    @PostMapping("/generate")
    public Result<Long> generate(@CurrentUserId Long userId, @Valid @RequestBody PlanGenerateDto dto) {
        Long planId = planGenerateService.generatePlan(userId, dto);
        return Result.success(planId);
    }

    /**
     * 获取当前登录用户进行中的计划摘要。
     */
    @GetMapping("/current")
    public Result<PlanSummaryVO> current(@CurrentUserId Long userId) {
        return Result.success(planGenerateService.getCurrentActivePlan(userId));
    }

    /**
     * 获取指定计划详情（含每日训练条目），仅允许本人访问。
     */
    @GetMapping("/{planId}")
    public Result<PlanFullVO> detail(@CurrentUserId Long userId, @PathVariable("planId") Long planId) {
        return Result.success(planGenerateService.getPlanDetail(userId, planId));
    }
}
