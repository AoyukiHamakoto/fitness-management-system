package com.fitness.management.service.impl;

import com.alibaba.fastjson2.JSON;
import com.fitness.management.dto.plan.PlanGenerateDto;
import com.fitness.management.entity.FitnessPlan;
import com.fitness.management.entity.PlanDetail;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.service.AIService;
import com.fitness.management.service.FitnessPlanService;
import com.fitness.management.service.PlanDetailService;
import com.fitness.management.service.PlanGenerateService;
import com.fitness.management.vo.plan.PlanContentVO;
import com.fitness.management.vo.plan.PlanDetailVO;
import com.fitness.management.vo.plan.PlanFullVO;
import com.fitness.management.vo.plan.PlanSummaryVO;
import com.fitness.management.vo.plan.WeeklyScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 论文用例：个性化计划生成 —提示词工程、JSON 解析、落库与默认计划回退。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerateServiceImpl implements PlanGenerateService {

    private static final int STATUS_IN_PROGRESS = 1;
    private static final int STATUS_COMPLETED = 2;

    /** 生成周期：固定4 周（与论文示例周期一致，可按需调整） */
    private static final int PLAN_WEEKS = 4;

    private final AIService aiService;
    private final FitnessPlanService fitnessPlanService;
    private final PlanDetailService planDetailService;

    /**
     * 系统提示词 + 用户提示词（提示词工程：仅输出标准 JSON，无解释性文字）。
     */
    protected record PromptParts(String systemPrompt, String userPrompt) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generatePlan(Long userId, PlanGenerateDto dto) {
        PromptParts parts = buildPrompt(dto);
        String raw;
        try {
            raw = aiService.callAI(parts.systemPrompt(), parts.userPrompt());
        } catch (Exception e) {
            log.warn("大模型调用失败，使用默认计划: {}", e.getMessage());
            raw = "";
        }
        PlanContentVO content = parseAIResponse(raw, dto);
        archiveActivePlans(userId);
        FitnessPlan plan = persistPlan(userId, content);
        generatePlanDetails(plan.getId(), content);
        return plan.getId();
    }

    @Override
    public PlanSummaryVO getCurrentActivePlan(Long userId) {
        FitnessPlan one = fitnessPlanService.lambdaQuery()
                .eq(FitnessPlan::getUserId, userId)
                .eq(FitnessPlan::getStatus, STATUS_IN_PROGRESS)
                .orderByDesc(FitnessPlan::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (one == null) {
            throw new BusinessException("当前没有进行中的健身计划");
        }
        return toSummary(one);
    }

    @Override
    public PlanFullVO getPlanDetail(Long userId, Long planId) {
        FitnessPlan plan = fitnessPlanService.getById(planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该计划");
        }
        List<PlanDetail> rows = planDetailService.lambdaQuery()
                .eq(PlanDetail::getPlanId, planId)
                .orderByAsc(PlanDetail::getDayIndex)
                .orderByAsc(PlanDetail::getSortOrder)
                .list();
        PlanFullVO vo = toFull(plan);
        for (PlanDetail row : rows) {
            PlanDetailVO d = new PlanDetailVO();
            d.setDayIndex(row.getDayIndex());
            d.setExerciseName(row.getExerciseName());
            d.setDurationMinutes(row.getDurationMinutes());
            d.setSortOrder(row.getSortOrder());
            vo.getDetails().add(d);
        }
        return vo;
    }

    /**
     * 论文提示词工程：约束模型输出单一 JSON 对象，字段与前端/库表解析结构严格一致。
     */
    protected PromptParts buildPrompt(PlanGenerateDto dto) {
        String system = """
                你是专业健身教练与运动处方顾问。你必须只输出一个合法 JSON 对象，不要输出 Markdown、代码围栏、解释性文字或前后缀。
                JSON 顶层结构必须为：
                {
                  "planName": "字符串，简短计划名称",
                  "planDesc": "字符串，计划总体说明与注意事项",
                  "weeks": [
                    {
                      "weekIndex": 整数，从1开始表示第几周,
                      "details": [
                        {
                          "dayIndex": 整数，全计划连续的第几天，从1开始递增,
                          "exerciseName": "字符串，训练动作或项目名",
                          "durationMinutes": 整数，建议时长分钟,
                          "sortOrder": 整数，同一天内动作顺序，从1递增
                        }
                      ]
                    }
                  ]
                }
                要求：
                1) weeks 数组长度与训练周期周数一致；每周 details 覆盖该周应练天数，且 dayIndex 全局连续不重复。
                2) exerciseName 应具体可执行（如快走、深蹲、哑铃推举），避免空泛描述。
                3) durationMinutes、sortOrder 必须为非负整数；dayIndex、weekIndex 从 1 开始。
                4) 输出必须是 UTF-8 下可被 JSON.parse 直接解析的文本。
                """;

        String user = String.format("""
                请根据以下用户体征与偏好生成个性化健身计划（仅输出 JSON）：
                - 身高: %s cm
                - 体重: %s kg
                - 年龄: %d
                - 性别: %s
                - 健身目标: %s
                - 运动经验: %s
                - 每周可训练天数: %d 天
                - 每次建议训练时长: 约 %d 分钟（单日总时长可略浮动，但需合理）
                计划总周数: %d 周。请保证训练量与经验水平匹配，有热身或放松安排时可作为独立 exerciseName。
                """,
                dto.getHeightCm().toPlainString(),
                dto.getWeightKg().toPlainString(),
                dto.getAge(),
                dto.getGender(),
                dto.getFitnessGoal(),
                dto.getExerciseExperience(),
                dto.getWeeklyTrainingDays(),
                dto.getSessionDurationMinutes(),
                PLAN_WEEKS);

        return new PromptParts(system.trim(), user.trim());
    }

    /**
     * 解析大模型文本：去 Markdown 围栏、反序列化、校验结构；异常或不合规则回退默认计划。
     */
    protected PlanContentVO parseAIResponse(String rawText, PlanGenerateDto dto) {
        if (!StringUtils.hasText(rawText)) {
            return buildDefaultPlan(dto);
        }
        try {
            String json = stripMarkdownCodeFence(rawText.trim());
            PlanContentVO vo = JSON.parseObject(json, PlanContentVO.class);
            if (vo == null || !StringUtils.hasText(vo.getPlanName())) {
                return buildDefaultPlan(dto);
            }
            if (vo.getWeeks() == null || vo.getWeeks().isEmpty()) {
                return buildDefaultPlan(dto);
            }
            for (WeeklyScheduleVO week : vo.getWeeks()) {
                if (week == null || week.getDetails() == null || week.getDetails().isEmpty()) {
                    return buildDefaultPlan(dto);
                }
                for (PlanDetailVO item : week.getDetails()) {
                    if (item == null || item.getDayIndex() == null || !StringUtils.hasText(item.getExerciseName())) {
                        return buildDefaultPlan(dto);
                    }
                }
            }
            return vo;
        } catch (Exception e) {
            log.warn("解析 AI 计划 JSON 失败，启用默认计划: {}", e.getMessage());
            return buildDefaultPlan(dto);
        }
    }

    /**
     * 将解析后的明细批量写入 plan_detail。
     */
    protected void generatePlanDetails(Long planId, PlanContentVO content) {
        List<PlanDetail> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (WeeklyScheduleVO week : content.getWeeks()) {
            if (week == null || week.getDetails() == null) {
                continue;
            }
            for (PlanDetailVO item : week.getDetails()) {
                if (item == null) {
                    continue;
                }
                PlanDetail row = new PlanDetail();
                row.setPlanId(planId);
                row.setDayIndex(item.getDayIndex());
                row.setExerciseName(item.getExerciseName().trim());
                int minutes = item.getDurationMinutes() != null ? item.getDurationMinutes() : 0;
                row.setDurationMinutes(Math.max(minutes, 0));
                int order = item.getSortOrder() != null ? item.getSortOrder() : 0;
                row.setSortOrder(Math.max(order, 0));
                row.setCreateTime(now);
                batch.add(row);
            }
        }
        if (!batch.isEmpty()) {
            planDetailService.saveBatch(batch);
        }
    }

    protected String stripMarkdownCodeFence(String raw) {
        String s = raw;
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            }
            int fence = s.lastIndexOf("```");
            if (fence >= 0) {
                s = s.substring(0, fence);
            }
        }
        return s.trim();
    }

    /**
     * 默认计划：模型不可用或 JSON 非法时的安全回退（仍满足闭环入库）。
     */
    protected PlanContentVO buildDefaultPlan(PlanGenerateDto dto) {
        PlanContentVO vo = new PlanContentVO();
        vo.setPlanName("基础有氧与力量入门（系统推荐）");
        vo.setPlanDesc(String.format(
                "根据您当前信息（目标：%s，经验：%s）生成的保守入门方案，待大模型可用后可重新生成个性化版本。",
                dto.getFitnessGoal(), dto.getExerciseExperience()));
        int daysPerWeek = Math.min(Math.max(dto.getWeeklyTrainingDays(), 1), 7);
        int sessionMin = dto.getSessionDurationMinutes();
        int dayCounter = 1;
        for (int w = 1; w <= PLAN_WEEKS; w++) {
            WeeklyScheduleVO week = new WeeklyScheduleVO();
            week.setWeekIndex(w);
            int order = 1;
            for (int d = 0; d < daysPerWeek; d++) {
                PlanDetailVO warm = new PlanDetailVO();
                warm.setDayIndex(dayCounter);
                warm.setExerciseName("动态热身");
                warm.setDurationMinutes(Math.min(10, sessionMin / 4));
                warm.setSortOrder(order++);
                week.getDetails().add(warm);

                PlanDetailVO main = new PlanDetailVO();
                main.setDayIndex(dayCounter);
                main.setExerciseName("快走或椭圆机有氧");
                main.setDurationMinutes(Math.max(sessionMin - 15, 20));
                main.setSortOrder(order++);
                week.getDetails().add(main);

                PlanDetailVO cool = new PlanDetailVO();
                cool.setDayIndex(dayCounter);
                cool.setExerciseName("静态拉伸放松");
                cool.setDurationMinutes(5);
                cool.setSortOrder(order++);
                week.getDetails().add(cool);
                dayCounter++;
            }
            vo.getWeeks().add(week);
        }
        return vo;
    }

    private void archiveActivePlans(Long userId) {
        fitnessPlanService.lambdaUpdate()
                .eq(FitnessPlan::getUserId, userId)
                .eq(FitnessPlan::getStatus, STATUS_IN_PROGRESS)
                .set(FitnessPlan::getStatus, STATUS_COMPLETED)
                .set(FitnessPlan::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private FitnessPlan persistPlan(Long userId, PlanContentVO content) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(PLAN_WEEKS).minusDays(1);
        LocalDateTime now = LocalDateTime.now();
        FitnessPlan plan = new FitnessPlan();
        plan.setUserId(userId);
        plan.setPlanName(content.getPlanName());
        plan.setPlanDesc(StringUtils.hasText(content.getPlanDesc()) ? content.getPlanDesc() : "");
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setStatus(STATUS_IN_PROGRESS);
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        if (!fitnessPlanService.save(plan)) {
            throw new BusinessException("保存健身计划失败");
        }
        return plan;
    }

    private PlanSummaryVO toSummary(FitnessPlan p) {
        PlanSummaryVO vo = new PlanSummaryVO();
        vo.setId(p.getId());
        vo.setPlanName(p.getPlanName());
        vo.setPlanDesc(p.getPlanDesc());
        vo.setStartDate(p.getStartDate());
        vo.setEndDate(p.getEndDate());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private PlanFullVO toFull(FitnessPlan p) {
        PlanFullVO vo = new PlanFullVO();
        vo.setId(p.getId());
        vo.setUserId(p.getUserId());
        vo.setPlanName(p.getPlanName());
        vo.setPlanDesc(p.getPlanDesc());
        vo.setStartDate(p.getStartDate());
        vo.setEndDate(p.getEndDate());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());
        return vo;
    }
}
