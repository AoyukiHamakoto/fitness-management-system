package com.fitness.management.service.impl;

import com.fitness.management.dto.checkin.CheckInDto;
import com.fitness.management.entity.FitnessPlan;
import com.fitness.management.entity.PlanDetail;
import com.fitness.management.entity.PunchRecord;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.service.CheckInService;
import com.fitness.management.service.FitnessPlanService;
import com.fitness.management.service.PlanDetailService;
import com.fitness.management.service.PunchRecordService;
import com.fitness.management.vo.checkin.CheckInResultVO;
import com.fitness.management.vo.checkin.TodayTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 论文用例：每日打卡 — 重复校验、合理性校验、落库、进度、Redis 连续打卡天数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private static final int STATUS_IN_PROGRESS = 1;

    /** Redis：连续打卡天数（字符串整数） */
    private static final String REDIS_STREAK_COUNT = "checkin:streak:count:";
    /** Redis：最后一次计入连续打卡的日历日期（ISO yyyy-MM-dd） */
    private static final String REDIS_STREAK_LAST_DATE = "checkin:streak:lastDate:";

    /** 连续打卡统计窗口与键过期：论文常设 60 天无活动则清理，避免脏数据常驻 */
    private static final long STREAK_TTL_DAYS = 60;

    /** 业务上限：防异常大数 */
    private static final int MAX_SETS = 500;
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("500");

    private final FitnessPlanService fitnessPlanService;
    private final PlanDetailService planDetailService;
    private final PunchRecordService punchRecordService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<TodayTaskVO> getTodayTasks(Long userId) {
        FitnessPlan plan = requireActivePlan(userId);
        LocalDate today = LocalDate.now();
        int dayIndex = resolveTodayPlanDayIndex(plan, today);
        if (dayIndex <= 0) {
            return List.of();
        }
        PlanDetail lastDayRow = planDetailService.lambdaQuery()
                .eq(PlanDetail::getPlanId, plan.getId())
                .orderByDesc(PlanDetail::getDayIndex)
                .last("LIMIT 1")
                .one();
        int maxDay = lastDayRow == null ? 0 : lastDayRow.getDayIndex();
        if (maxDay > 0 && dayIndex > maxDay) {
            return List.of();
        }
        List<PlanDetail> rows = planDetailService.lambdaQuery()
                .eq(PlanDetail::getPlanId, plan.getId())
                .eq(PlanDetail::getDayIndex, dayIndex)
                .orderByAsc(PlanDetail::getSortOrder)
                .list();
        List<TodayTaskVO> list = new ArrayList<>();
        for (PlanDetail row : rows) {
            TodayTaskVO vo = new TodayTaskVO();
            vo.setPlanDetailId(row.getId());
            vo.setDayIndex(row.getDayIndex());
            vo.setExerciseName(row.getExerciseName());
            vo.setDurationMinutes(row.getDurationMinutes());
            vo.setSortOrder(row.getSortOrder());
            boolean punched = punchRecordService.lambdaQuery()
                    .eq(PunchRecord::getUserId, userId)
                    .eq(PunchRecord::getPlanDetailId, row.getId())
                    .eq(PunchRecord::getPunchDate, today)
                    .exists();
            vo.setCheckedIn(punched);
            list.add(vo);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInResultVO doCheckIn(Long userId, CheckInDto dto) {
        FitnessPlan plan = requireActivePlan(userId);
        LocalDate today = LocalDate.now();
        int todayDayIndex = resolveTodayPlanDayIndex(plan, today);
        if (todayDayIndex <= 0) {
            throw new BusinessException("计划尚未开始，无法打卡");
        }

        PlanDetail detail = planDetailService.getById(dto.getPlanDetailId());
        if (detail == null) {
            throw new BusinessException("训练项不存在");
        }
        if (!detail.getPlanId().equals(plan.getId())) {
            throw new BusinessException("该训练项不属于当前进行中的计划");
        }
        if (!detail.getDayIndex().equals(todayDayIndex)) {
            throw new BusinessException("该训练项不是今日任务，无法打卡");
        }

        validateCheckInPayload(dto);

        boolean duplicated = punchRecordService.lambdaQuery()
                .eq(PunchRecord::getUserId, userId)
                .eq(PunchRecord::getPlanDetailId, dto.getPlanDetailId())
                .eq(PunchRecord::getPunchDate, today)
                .exists();
        if (duplicated) {
            throw new BusinessException("今日该训练项已打卡，请勿重复提交");
        }

        PunchRecord record = new PunchRecord();
        record.setUserId(userId);
        record.setPlanId(plan.getId());
        record.setPlanDetailId(dto.getPlanDetailId());
        record.setPunchDate(today);
        record.setActualSets(dto.getActualSets());
        record.setActualWeight(dto.getActualWeight().setScale(2, RoundingMode.HALF_UP));
        record.setRemark("");
        record.setCreateTime(LocalDateTime.now());

        try {
            if (!punchRecordService.save(record)) {
                throw new BusinessException("打卡失败，请稍后重试");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("今日该训练项已打卡，请勿重复提交");
        }

        int progress = computePlanProgressPercent(userId, plan.getId());
        updateStreak(userId, today);

        CheckInResultVO vo = new CheckInResultVO();
        vo.setPunchRecordId(record.getId());
        vo.setCurrentStreakDays(getCurrentStreak(userId));
        vo.setProgressPercent(progress);
        vo.setMessage("打卡成功");
        return vo;
    }

    @Override
    public int getCurrentStreak(Long userId) {
        String raw = stringRedisTemplate.opsForValue().get(REDIS_STREAK_COUNT + userId);
        if (!StringUtils.hasText(raw)) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(raw.trim()), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 论文逻辑：以「日历日」为粒度维护连续打卡。
     * <ul>
     *     <li>若上次计入 streak 的日期就是今天：不再递增（同一天多次打卡只算一次）</li>
     *     <li>若上次为昨天：streak + 1</li>
     *     <li>若上次为空或其它：streak 置为 1（今日首次有效打卡）</li>
     * </ul>
     */
    protected void updateStreak(Long userId, LocalDate today) {
        String countKey = REDIS_STREAK_COUNT + userId;
        String lastKey = REDIS_STREAK_LAST_DATE + userId;
        String lastStr = stringRedisTemplate.opsForValue().get(lastKey);

        if (!StringUtils.hasText(lastStr)) {
            stringRedisTemplate.opsForValue().set(countKey, "1", STREAK_TTL_DAYS, TimeUnit.DAYS);
            stringRedisTemplate.opsForValue().set(lastKey, today.toString(), STREAK_TTL_DAYS, TimeUnit.DAYS);
            return;
        }
        LocalDate last = LocalDate.parse(lastStr);
        if (last.equals(today)) {
            touchTtl(countKey, lastKey);
            return;
        }
        long streak = parseStreakCount(countKey);
        if (last.equals(today.minusDays(1))) {
            streak++;
        } else {
            streak = 1;
        }
        stringRedisTemplate.opsForValue().set(countKey, String.valueOf(streak), STREAK_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(lastKey, today.toString(), STREAK_TTL_DAYS, TimeUnit.DAYS);
    }

    private void touchTtl(String countKey, String lastKey) {
        stringRedisTemplate.expire(countKey, STREAK_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.expire(lastKey, STREAK_TTL_DAYS, TimeUnit.DAYS);
    }

    private long parseStreakCount(String countKey) {
        String v = stringRedisTemplate.opsForValue().get(countKey);
        if (!StringUtils.hasText(v)) {
            return 1;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * 今日在计划中的日序号：计划开始日为第 1 天；未开始返回 0。
     */
    protected int resolveTodayPlanDayIndex(FitnessPlan plan, LocalDate today) {
        if (today.isBefore(plan.getStartDate())) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(plan.getStartDate(), today) + 1;
    }

    private FitnessPlan requireActivePlan(Long userId) {
        FitnessPlan plan = fitnessPlanService.lambdaQuery()
                .eq(FitnessPlan::getUserId, userId)
                .eq(FitnessPlan::getStatus, STATUS_IN_PROGRESS)
                .orderByDesc(FitnessPlan::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (plan == null) {
            throw new BusinessException("当前没有进行中的健身计划，无法打卡");
        }
        return plan;
    }

    private void validateCheckInPayload(CheckInDto dto) {
        if (dto.getActualSets() > MAX_SETS) {
            throw new BusinessException("完成组数超出合理范围");
        }
        if (dto.getActualWeight().compareTo(MAX_WEIGHT_KG) > 0) {
            throw new BusinessException("重量超出合理范围");
        }
    }

    /**
     * 计划完成进度：已产生打卡记录的去重明细数 / 计划总明细条数（上限 100）。
     */
    protected int computePlanProgressPercent(Long userId, Long planId) {
        long total = planDetailService.lambdaQuery().eq(PlanDetail::getPlanId, planId).count();
        if (total <= 0) {
            return 0;
        }
        long punchedDetails = punchRecordService.lambdaQuery()
                .eq(PunchRecord::getUserId, userId)
                .eq(PunchRecord::getPlanId, planId)
                .isNotNull(PunchRecord::getPlanDetailId)
                .count();
        int pct = BigDecimal.valueOf(punchedDetails)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.min(Math.max(pct, 0), 100);
    }
}
