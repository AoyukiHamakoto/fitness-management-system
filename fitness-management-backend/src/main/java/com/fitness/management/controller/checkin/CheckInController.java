package com.fitness.management.controller.checkin;

import com.fitness.management.common.Result;
import com.fitness.management.dto.checkin.CheckInDto;
import com.fitness.management.security.CurrentUserId;
import com.fitness.management.security.RequireAuth;
import com.fitness.management.service.CheckInService;
import com.fitness.management.vo.checkin.CheckInResultVO;
import com.fitness.management.vo.checkin.TodayTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 每日打卡：今日任务、提交打卡、连续天数（JWT 统一鉴权）。
 */
@RestController
@RequestMapping("/check-ins")
@RequireAuth
@RequiredArgsConstructor
@Validated
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 获取当前登录用户「今日」训练任务列表（基于进行中计划与计划开始日推算）。
     */
    @GetMapping("/today-tasks")
    public Result<List<TodayTaskVO>> todayTasks(@CurrentUserId Long userId) {
        return Result.success(checkInService.getTodayTasks(userId));
    }

    /**
     * 提交打卡（仅允许当日对应计划日的明细，防重复与越权）。
     */
    @PostMapping
    public Result<CheckInResultVO> checkIn(@CurrentUserId Long userId, @Valid @RequestBody CheckInDto dto) {
        return Result.success(checkInService.doCheckIn(userId, dto));
    }

    /**
     * 当前连续打卡天数（Redis）。
     */
    @GetMapping("/streak")
    public Result<Integer> streak(@CurrentUserId Long userId) {
        return Result.success(checkInService.getCurrentStreak(userId));
    }
}
