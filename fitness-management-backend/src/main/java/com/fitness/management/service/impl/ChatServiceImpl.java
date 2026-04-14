package com.fitness.management.service.impl;

import com.fitness.management.ai.AIResponseHandler;
import com.fitness.management.chat.ChatUserContext;
import com.fitness.management.dto.chat.ChatStreamRequestDto;
import com.fitness.management.entity.AiDialog;
import com.fitness.management.entity.BodyDataRecord;
import com.fitness.management.entity.FitnessPlan;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.service.AIService;
import com.fitness.management.service.AiDialogService;
import com.fitness.management.service.BodyDataRecordService;
import com.fitness.management.service.ChatService;
import com.fitness.management.service.CheckInService;
import com.fitness.management.service.FitnessPlanService;
import com.fitness.management.vo.checkin.TodayTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 论文用例：AI 智能对话 —限流、业务上下文、提示词增强、流式输出、落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int STATUS_IN_PROGRESS = 1;
    private static final int CHAT_REQUESTS_PER_MINUTE = 10;
    private static final String REDIS_CHAT_RL_PREFIX = "chat:rl:";

    private final AIService aiService;
    private final AiDialogService aiDialogService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CheckInService checkInService;
    private final FitnessPlanService fitnessPlanService;
    private final BodyDataRecordService bodyDataRecordService;

    @Override
    public void startStreamChat(Long userId, ChatStreamRequestDto dto, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> streamChat(userId, dto, emitter));
    }

    /**
     * 论文核心方法：限流、上下文、增强提示词、流式调用、落库（在异步线程中执行）。
     */
    protected void streamChat(Long userId, ChatStreamRequestDto dto, SseEmitter emitter) {
        try {
            String message = dto.getMessage() == null ? "" : dto.getMessage().trim();
            if (!StringUtils.hasText(message)) {
                emitErrorEvent(emitter, "消息内容不能为空");
                emitter.complete();
                return;
            }
            assertChatRateLimit(userId);
            String sessionId = StringUtils.hasText(dto.getSessionId())
                    ? dto.getSessionId().trim()
                    : UUID.randomUUID().toString();

            ChatUserContext ctx = getUserContext(userId);
            String systemPrompt = buildSystemPersona();
            String userPrompt = buildPromptWithContext(ctx, message);

            aiService.streamChatWithHandler(systemPrompt, userPrompt, new AIResponseHandler() {
                @Override
                public void onToken(String token) {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onComplete(String fullContent) {
                    try {
                        saveChatHistory(userId, sessionId, message, fullContent != null ? fullContent : "");
                        emitter.send(SseEmitter.event().name("done").data(""));
                    } catch (Exception e) {
                        log.error("保存对话历史或发送完成事件失败 userId={}", userId, e);
                        try {
                            emitErrorEvent(emitter, "保存对话记录失败");
                        } catch (IOException ignored) {
                        }
                    } finally {
                        emitter.complete();
                    }
                }

                @Override
                public void onError(Throwable error) {
                    String msg = error.getMessage() != null ? error.getMessage() : "对话服务异常";
                    if (error.getCause() != null && StringUtils.hasText(error.getCause().getMessage())) {
                        msg = error.getCause().getMessage();
                    }
                    try {
                        emitErrorEvent(emitter, msg);
                    } catch (IOException ignored) {
                    }
                    emitter.completeWithError(error);
                }
            });
        } catch (BusinessException e) {
            try {
                emitErrorEvent(emitter, e.getMessage());
            } catch (IOException ignored) {
            }
            emitter.complete();
        } catch (Exception e) {
            log.error("流式对话未捕获异常 userId={}", userId, e);
            try {
                emitErrorEvent(emitter, "对话处理失败");
            } catch (IOException ignored) {
            }
            emitter.completeWithError(e);
        }
    }

    /**
     * Redis 固定窗口：每分钟每用户最多 10 次对话请求（与论文频率限制一致）。
     */
    protected void assertChatRateLimit(Long userId) {
        long minuteWindow = System.currentTimeMillis() / 60_000;
        String key = REDIS_CHAT_RL_PREFIX + userId + ":" + minuteWindow;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, 2, TimeUnit.MINUTES);
        }
        if (count != null && count > CHAT_REQUESTS_PER_MINUTE) {
            throw new BusinessException("对话请求过于频繁，每分钟最多 " + CHAT_REQUESTS_PER_MINUTE + " 次，请稍后再试");
        }
    }

    /**
     * 汇总当前登录用户的计划、今日任务、体测与连续打卡，供提示词注入。
     */
    protected ChatUserContext getUserContext(Long userId) {
        ChatUserContext ctx = new ChatUserContext();
        ctx.setStreakDays(checkInService.getCurrentStreak(userId));

        FitnessPlan plan = fitnessPlanService.lambdaQuery()
                .eq(FitnessPlan::getUserId, userId)
                .eq(FitnessPlan::getStatus, STATUS_IN_PROGRESS)
                .orderByDesc(FitnessPlan::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (plan == null) {
            ctx.setCurrentPlanSummary("当前暂无进行中的健身计划。");
        } else {
            ctx.setCurrentPlanSummary(String.format(
                    "进行中计划「%s」：%s 至 %s。说明：%s",
                    plan.getPlanName(),
                    plan.getStartDate(),
                    plan.getEndDate(),
                    plan.getPlanDesc() != null ? plan.getPlanDesc() : "无"));
        }

        try {
            List<TodayTaskVO> tasks = checkInService.getTodayTasks(userId);
            if (tasks == null || tasks.isEmpty()) {
                ctx.setTodayTasksSummary("今日无待完成训练项（未到训练日、计划未开始或已超出计划天数）。");
            } else {
                StringBuilder sb = new StringBuilder();
                for (TodayTaskVO t : tasks) {
                    sb.append(String.format("- %s（建议 %d 分钟，今日已打卡：%s）%n",
                            t.getExerciseName(),
                            t.getDurationMinutes() != null ? t.getDurationMinutes() : 0,
                            Boolean.TRUE.equals(t.getCheckedIn()) ? "是" : "否"));
                }
                ctx.setTodayTasksSummary(sb.toString().trim());
            }
        } catch (BusinessException e) {
            ctx.setTodayTasksSummary("今日任务不可用：" + e.getMessage());
        }

        BodyDataRecord body = bodyDataRecordService.lambdaQuery()
                .eq(BodyDataRecord::getUserId, userId)
                .orderByDesc(BodyDataRecord::getRecordDate)
                .last("LIMIT 1")
                .one();
        if (body == null) {
            ctx.setLatestBodyDataSummary("暂无身体数据记录。");
        } else {
            ctx.setLatestBodyDataSummary(String.format(
                    "最近体测日期 %s，体重 %s kg",
                    body.getRecordDate(),
                    body.getWeight() != null ? body.getWeight().toPlainString() : "-"));
        }
        return ctx;
    }

    /**
     * 论文提示词工程：将业务上下文结构化写入用户消息，便于模型个性化回答。
     */
    protected String buildPromptWithContext(ChatUserContext ctx, String userMessage) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return """
                【当前时间】%s

                【用户业务上下文 — 请据此给出可执行、安全、个性化的建议，不要编造不存在的记录】
                1) 连续打卡天数：%d 天
                2) 当前计划：%s
                3) 今日训练任务：
                %s
                4) 身体数据：%s

                【用户问题】
                %s

                回答要求：语言简洁专业；涉及训练强度时结合其运动经验保守建议；若上下文显示无计划或无今日任务，先说明事实再给出通用建议。
                """
                .formatted(
                        now,
                        ctx.getStreakDays(),
                        ctx.getCurrentPlanSummary(),
                        ctx.getTodayTasksSummary(),
                        ctx.getLatestBodyDataSummary(),
                        userMessage);
    }

    /**
     * 系统人设与安全边界（论文中的角色与约束）。
     */
    protected String buildSystemPersona() {
        return """
                你是「个人智能健身管理系统」中的 AI 教练助手。你通过系统提供的用户上下文了解其计划与打卡情况。
                禁止输出违法、医疗诊断或替代医生建议的内容；出现伤病、胸痛、眩晕等情况应建议停止运动并就医。
                回答使用中文；优先条理清晰、分点说明；不要提及或猜测用户未提供的隐私信息。
                """;
    }

    /**
     * 对话结束后写入 ai_dialog：一条 user、一条 assistant，共用 sessionId。
     */
    protected void saveChatHistory(Long userId, String sessionId, String userContent, String assistantContent) {
        LocalDateTime now = LocalDateTime.now();
        List<AiDialog> rows = new ArrayList<>(2);
        AiDialog userRow = new AiDialog();
        userRow.setUserId(userId);
        userRow.setSessionId(sessionId);
        userRow.setRole("user");
        userRow.setContent(userContent);
        userRow.setCreateTime(now);

        AiDialog assistantRow = new AiDialog();
        assistantRow.setUserId(userId);
        assistantRow.setSessionId(sessionId);
        assistantRow.setRole("assistant");
        assistantRow.setContent(assistantContent != null ? assistantContent : "");
        assistantRow.setCreateTime(LocalDateTime.now());

        rows.add(userRow);
        rows.add(assistantRow);
        if (!aiDialogService.saveBatch(rows)) {
            throw new BusinessException("写入对话历史失败");
        }
    }

    private void emitErrorEvent(SseEmitter emitter, String message) throws IOException {
        emitter.send(SseEmitter.event().name("error").data(message != null ? message : "error"));
    }
}
