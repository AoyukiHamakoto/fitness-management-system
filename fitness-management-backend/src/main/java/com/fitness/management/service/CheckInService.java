package com.fitness.management.service;

import com.fitness.management.dto.checkin.CheckInDto;
import com.fitness.management.vo.checkin.CheckInResultVO;
import com.fitness.management.vo.checkin.TodayTaskVO;

import java.util.List;

/**
 * 计划每日打卡：今日任务、提交打卡、连续打卡统计。
 */
public interface CheckInService {

    /**
     * 当前进行中计划下，按「今日」对应的计划日序号列出应做任务，并标记是否已打卡。
     */
    List<TodayTaskVO> getTodayTasks(Long userId);

    /**
     * 提交打卡：防重复、数据合理性、写库、进度与连续天数更新。
     */
    CheckInResultVO doCheckIn(Long userId, CheckInDto dto);

    /**
     * 从 Redis 读取当前连续打卡天数（与最后打卡日期键配合维护）。
     */
    int getCurrentStreak(Long userId);
}
