package com.fitness.management.controller.dashboard;

import com.fitness.management.common.Result;
import com.fitness.management.entity.BodyDataRecord;
import com.fitness.management.entity.FitnessPlan;
import com.fitness.management.entity.PlanDetail;
import com.fitness.management.entity.PunchRecord;
import com.fitness.management.entity.User;
import com.fitness.management.security.CurrentUserId;
import com.fitness.management.security.RequireAuth;
import com.fitness.management.service.BodyDataRecordService;
import com.fitness.management.service.FitnessPlanService;
import com.fitness.management.service.PlanDetailService;
import com.fitness.management.service.PunchRecordService;
import com.fitness.management.service.UserService;
import com.fitness.management.vo.dashboard.BodyTrendPointVO;
import com.fitness.management.vo.dashboard.CheckInHeatPointVO;
import com.fitness.management.vo.dashboard.CompletionRateVO;
import com.fitness.management.vo.dashboard.ExerciseLibraryItemVO;
import com.fitness.management.vo.dashboard.LeaderboardItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequireAuth
@RequiredArgsConstructor
public class DashboardController {

    private static final BigDecimal DEFAULT_HEIGHT_M = new BigDecimal("1.75");
    private static final int ACTIVE_PLAN_STATUS = 1;

    private final BodyDataRecordService bodyDataRecordService;
    private final PunchRecordService punchRecordService;
    private final FitnessPlanService fitnessPlanService;
    private final PlanDetailService planDetailService;
    private final UserService userService;

    @GetMapping("/body-trends")
    public Result<List<BodyTrendPointVO>> bodyTrends(
            @CurrentUserId Long userId,
            @RequestParam(value = "days", required = false, defaultValue = "90") Integer days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(days, 7) - 1L);
        List<BodyDataRecord> records = bodyDataRecordService.lambdaQuery()
                .eq(BodyDataRecord::getUserId, userId)
                .ge(BodyDataRecord::getRecordDate, start)
                .le(BodyDataRecord::getRecordDate, end)
                .orderByAsc(BodyDataRecord::getRecordDate)
                .list();
        List<BodyTrendPointVO> list = new ArrayList<>();
        for (BodyDataRecord r : records) {
            BodyTrendPointVO point = new BodyTrendPointVO();
            point.setDate(r.getRecordDate());
            point.setWeight(r.getWeight());
            point.setBmi(calculateBmi(r.getWeight()));
            point.setBodyFatRate(estimateBodyFatRate(point.getBmi()));
            list.add(point);
        }
        return Result.success(list);
    }

    @GetMapping("/checkin-heatmap")
    public Result<List<CheckInHeatPointVO>> checkInHeatmap(
            @CurrentUserId Long userId,
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {
        YearMonth ym = parseYearMonth(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<PunchRecord> records = punchRecordService.lambdaQuery()
                .eq(PunchRecord::getUserId, userId)
                .ge(PunchRecord::getPunchDate, start)
                .le(PunchRecord::getPunchDate, end)
                .list();
        Map<LocalDate, Long> byDay = records.stream()
                .collect(Collectors.groupingBy(PunchRecord::getPunchDate, Collectors.counting()));
        List<CheckInHeatPointVO> list = new ArrayList<>();
        LocalDate date = start;
        while (!date.isAfter(end)) {
            CheckInHeatPointVO vo = new CheckInHeatPointVO();
            vo.setDate(date);
            vo.setCount(byDay.getOrDefault(date, 0L).intValue());
            list.add(vo);
            date = date.plusDays(1);
        }
        return Result.success(list);
    }

    @GetMapping("/completion-rate")
    public Result<CompletionRateVO> completionRate(
            @CurrentUserId Long userId,
            @RequestParam(value = "range", required = false, defaultValue = "week") String range) {
        LocalDate end = LocalDate.now();
        LocalDate start = "month".equalsIgnoreCase(range) ? end.withDayOfMonth(1) : end.minusDays(6);
        CompletionRateVO vo = new CompletionRateVO();
        FitnessPlan current = fitnessPlanService.lambdaQuery()
                .eq(FitnessPlan::getUserId, userId)
                .eq(FitnessPlan::getStatus, ACTIVE_PLAN_STATUS)
                .orderByDesc(FitnessPlan::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (current == null) {
            vo.setCompleted(0);
            vo.setPending(0);
            vo.setOverCompleted(0);
            return Result.success(vo);
        }

        List<PlanDetail> details = planDetailService.lambdaQuery()
                .eq(PlanDetail::getPlanId, current.getId())
                .list();
        int planned = 0;
        for (PlanDetail d : details) {
            LocalDate day = current.getStartDate().plusDays(Math.max(d.getDayIndex(), 1) - 1L);
            if (!day.isBefore(start) && !day.isAfter(end)) {
                planned++;
            }
        }

        List<PunchRecord> doneRecords = punchRecordService.lambdaQuery()
                .eq(PunchRecord::getUserId, userId)
                .eq(PunchRecord::getPlanId, current.getId())
                .ge(PunchRecord::getPunchDate, start)
                .le(PunchRecord::getPunchDate, end)
                .isNotNull(PunchRecord::getPlanDetailId)
                .list();
        Set<Long> doneDetailIds = doneRecords.stream()
                .map(PunchRecord::getPlanDetailId)
                .collect(Collectors.toSet());
        int completed = doneDetailIds.size();
        int overCompleted = Math.max(completed - planned, 0);
        int pending = Math.max(planned - completed, 0);

        vo.setCompleted(completed);
        vo.setPending(pending);
        vo.setOverCompleted(overCompleted);
        return Result.success(vo);
    }

    @GetMapping("/leaderboard")
    public Result<List<LeaderboardItemVO>> leaderboard(
            @RequestParam(value = "range", required = false, defaultValue = "week") String range) {
        LocalDate end = LocalDate.now();
        LocalDate start = "month".equalsIgnoreCase(range) ? end.withDayOfMonth(1) : end.minusDays(6);
        List<PunchRecord> records = punchRecordService.lambdaQuery()
                .ge(PunchRecord::getPunchDate, start)
                .le(PunchRecord::getPunchDate, end)
                .list();

        Map<Long, Long> grouped = records.stream()
                .collect(Collectors.groupingBy(PunchRecord::getUserId, Collectors.counting()));

        List<User> users = userService.lambdaQuery()
                .in(!grouped.isEmpty(), User::getId, grouped.keySet())
                .list();
        Map<Long, String> nickMap = new HashMap<>();
        for (User user : users) {
            String nick = user.getNickname();
            if (nick == null || nick.isBlank()) {
                nick = user.getUsername();
            }
            nickMap.put(user.getId(), maskNickname(nick));
        }

        List<Map.Entry<Long, Long>> sorted = grouped.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .toList();
        List<LeaderboardItemVO> list = new ArrayList<>();
        int idx = 1;
        for (Map.Entry<Long, Long> it : sorted) {
            LeaderboardItemVO row = new LeaderboardItemVO();
            row.setRank(idx++);
            row.setUserId(it.getKey());
            row.setNickname(nickMap.getOrDefault(it.getKey(), "用户" + it.getKey()));
            row.setValue(it.getValue().intValue());
            list.add(row);
        }
        return Result.success(list);
    }

    @GetMapping("/exercise-library")
    public Result<List<ExerciseLibraryItemVO>> exerciseLibrary() {
        List<ExerciseLibraryItemVO> list = List.of(
                buildExercise("深蹲", "腿部/臀部", "中级", "保持核心收紧，下蹲至大腿与地面平行后起身。", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b"),
                buildExercise("卧推", "胸部/三头", "中级", "肩胛后收，下放至胸中部，稳定推起，避免耸肩。", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48"),
                buildExercise("硬拉", "背部/臀腿", "高级", "全程保持脊柱中立，杠铃贴腿轨迹，上拉至髋关节完全伸展。", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438"),
                buildExercise("平板支撑", "核心", "初级", "前臂撑地，肩髋踝保持一条直线，避免塌腰。", "https://images.unsplash.com/photo-1518611012118-696072aa579a"),
                buildExercise("哑铃划船", "背阔肌/后链", "中级", "上身前倾稳定核心，肘沿身体两侧向后拉。", "https://images.unsplash.com/photo-1599058917212-d750089bc07e")
        );
        return Result.success(list);
    }

    private YearMonth parseYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(yearMonth.trim());
        } catch (Exception ignored) {
            return YearMonth.now();
        }
    }

    private BigDecimal calculateBmi(BigDecimal weight) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal heightSq = DEFAULT_HEIGHT_M.multiply(DEFAULT_HEIGHT_M);
        return weight.divide(heightSq, 2, RoundingMode.HALF_UP);
    }

    /**
     * 简化估算：用于看板趋势展示，不作为医学建议。
     */
    private BigDecimal estimateBodyFatRate(BigDecimal bmi) {
        if (bmi == null || bmi.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = bmi.multiply(new BigDecimal("1.15")).add(new BigDecimal("6.00"));
        return value.min(new BigDecimal("45.00")).max(new BigDecimal("5.00")).setScale(2, RoundingMode.HALF_UP);
    }

    private String maskNickname(String nick) {
        if (nick == null || nick.isBlank()) {
            return "匿名用户";
        }
        if (nick.length() <= 1) {
            return nick + "*";
        }
        if (nick.length() == 2) {
            return nick.charAt(0) + "*";
        }
        return nick.charAt(0) + "***" + nick.charAt(nick.length() - 1);
    }

    private ExerciseLibraryItemVO buildExercise(
            String name, String muscleGroup, String difficulty, String description, String mediaUrl) {
        ExerciseLibraryItemVO vo = new ExerciseLibraryItemVO();
        vo.setName(name);
        vo.setMuscleGroup(muscleGroup);
        vo.setDifficulty(difficulty);
        vo.setDescription(description);
        vo.setMediaUrl(mediaUrl);
        return vo;
    }
}
