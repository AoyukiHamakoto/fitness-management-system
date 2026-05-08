package com.fitness.management.service.impl;

import com.fitness.management.chat.ChatFallbackMode;
import com.fitness.management.chat.ChatUserContext;
import com.fitness.management.config.LlmProperties;
import com.fitness.management.service.ChatFallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 固定话术降级：按关键词匹配常见健身意图，否则输出通用建议并附上系统侧用户摘要。
 */
@Service
@RequiredArgsConstructor
public class ChatFallbackServiceImpl implements ChatFallbackService {

    private static final Pattern P_PLAN = Pattern.compile("计划|课程|训练安排|周期");
    private static final Pattern P_CHECKIN = Pattern.compile("打卡|坚持|连续|毅力");
    private static final Pattern P_BODY = Pattern.compile("体重|体脂|减脂|增肌|塑形|bmi");
    private static final Pattern P_INJURY = Pattern.compile("受伤|疼|痛|扭伤|拉伤|膝盖|腰");
    private static final Pattern P_CARDIO = Pattern.compile("有氧|跑步|跳绳|爬楼|心率");
    private static final Pattern P_STRENGTH = Pattern.compile("力量|哑铃|杠铃|深蹲|卧推|器械");
    private static final Pattern P_REST = Pattern.compile("休息|恢复|睡眠|疲劳|过度");
    private static final Pattern P_DIET = Pattern.compile("饮食|吃|营养|蛋白|碳水");
    private static final Pattern P_GREET = Pattern.compile("(你好|您好|\\bhi\\b|\\bhello\\b)", Pattern.CASE_INSENSITIVE);

    private final LlmProperties llmProperties;

    @Override
    public boolean isLlmConfigured() {
        return StringUtils.hasText(llmProperties.getApiKey()) && StringUtils.hasText(llmProperties.getApiUrl());
    }

    @Override
    public String composeFixedReply(ChatUserContext ctx, String userMessage, ChatFallbackMode mode) {
        String banner = switch (mode) {
            case NO_LLM_CONFIGURED -> "> **提示：** 当前未配置或未启用在线大模型，以下为系统预设的健身建议，仅供参考；配置 `LLM_API_KEY` 后可使用智能生成回答。\n\n";
            case LLM_UNAVAILABLE -> "> **提示：** 大模型服务暂时不可用，已自动切换为预设回复。\n\n";
        };

        String core = pickTemplate(ctx, userMessage == null ? "" : userMessage);
        String appendix = "\n\n---\n**你的档案摘要（来自系统）**\n"
                + "- 连续打卡：**" + ctx.getStreakDays() + "** 天\n"
                + "- 计划：" + oneLine(ctx.getCurrentPlanSummary()) + "\n"
                + "- 今日任务：" + oneLine(ctx.getTodayTasksSummary()) + "\n"
                + "- 身体数据：" + oneLine(ctx.getLatestBodyDataSummary()) + "\n"
                + "\n*以上摘要由系统自动附带，预设回复无法像大模型一样深度推理；如有伤病不适请咨询医生。*";

        return banner + core + appendix;
    }

    private static String oneLine(String s) {
        if (!StringUtils.hasText(s)) {
            return "（无）";
        }
        return s.trim().replace("\r\n", " ").replace("\n", " ");
    }

    private String pickTemplate(ChatUserContext ctx, String raw) {
        String t = raw.trim();
        if (!StringUtils.hasText(t)) {
            return generic(ctx);
        }
        String lower = t.toLowerCase(Locale.ROOT);

        if (P_GREET.matcher(lower).find()) {
            return """
                    你好！我是健身管理里的 **AI 助手（离线预设模式）**。

                    你可以问我：训练计划怎么安排、今天练什么、打卡习惯、有氧/力量怎么搭配、休息恢复等。我会结合你左侧摘要里的计划与打卡信息，尽量给**可执行、偏保守**的建议。
                    """.trim();
        }
        if (P_INJURY.matcher(t).find()) {
            return """
                    **安全优先：** 如出现锐痛、肿胀、麻木、胸闷、头晕等，请**立即停止运动**并视情况就医；本系统不能替代诊疗。

                    在医生允许恢复训练前，可做低冲击活动（如散步）并记录症状变化。恢复训练后应从低强度、少容量开始，逐步加量。
                    """.trim();
        }
        if (P_PLAN.matcher(t).find()) {
            return """
                    **关于训练计划**

                    1. 先确认你有一段「进行中」的计划；若没有，可在系统中生成或新建计划后再执行。
                    2. 每周安排建议：**力量 + 有氧 + 完整休息日** 组合，避免连续高强度。
                    3. 单次训练以「动作质量 > 重量」为先；新手每周总训练日 **3～4 天** 通常更易坚持。

                    你当前的计划摘要已在文末列出；若显示暂无进行中计划，请先到「健身计划」里创建或激活计划。
                    """.trim();
        }
        if (P_CHECKIN.matcher(t).find()) {
            return """
                    **关于打卡与坚持**

                    - 连续打卡是习惯信号，但偶尔中断也正常，不必过度焦虑。
                    - 小技巧：固定训练时段、提前准备好装备、把单次训练控制在可完成的长度（如 30～45 分钟）。
                    - 若长期完不成计划，优先**降低单次容量或频率**，而不是硬撑。

                    你在系统中的连续打卡天数见文末摘要。
                    """.trim();
        }
        if (P_BODY.matcher(t).find()) {
            return """
                    **体重与目标（通用原则）**

                    - 减脂：适度热量缺口 + 足量蛋白质 + 规律力量训练，避免过快减重。
                    - 增肌：渐进超负荷 + 充足睡眠；体重变化慢是常见现象，可看力量与围度。
                    - 体重波动受水分、盐分、生理期等影响，建议结合**周趋势**判断。

                    最近体测与体重记录见文末摘要。
                    """.trim();
        }
        if (P_CARDIO.matcher(t).find()) {
            return """
                    **有氧建议（预设）**

                    - 一般健康：每周 **150 分钟** 中等强度有氧（可拆分多日），或 75 分钟较高强度。
                    - 与力量训练同一天时，可力量优先或分上下午；减脂期可适当增加低冲击有氧（快走、单车）。
                    - 以「能完整说话、略有气喘」为中等强度参考；循序渐进。

                    具体安排请结合你当前计划与身体感受调整。
                    """.trim();
        }
        if (P_STRENGTH.matcher(t).find()) {
            return """
                    **力量训练（预设）**

                    - 复合动作（蹲、拉、推）优先，再补孤立动作；每周同类肌群建议 **48h+** 恢复。
                    - 组数/次数：新手可 2～3 组 × 8～12 次为主，动作稳定后再加重。
                    - 训练前动态热身，训练后轻度拉伸；保证睡眠与蛋白质摄入。

                    今日系统内安排的动作任务见文末摘要。
                    """.trim();
        }
        if (P_REST.matcher(t).find()) {
            return """
                    **休息与恢复**

                    - 睡眠是恢复核心，尽量规律作息；训练日与非训练日都可安排轻松步行。
                    - 出现持续疲劳、力量下降、易怒或失眠，考虑**减量或增加休息日**。
                    - 「超量恢复」发生在休息中，不是练得越多越好。

                    可把休息日写成计划的一部分，避免心理负担。
                    """.trim();
        }
        if (P_DIET.matcher(t).find()) {
            return """
                    **饮食（通用、非医疗）**

                    - 优先全食物：足量蔬菜、优质蛋白（肉蛋豆奶）、适量主食与健康脂肪。
                    - 减脂不必极端节食；增肌需总能量与蛋白足够。
                    - 训练前后可根据肠胃耐受安排少量碳水 + 蛋白；具体疾病饮食请遵医嘱。

                    本模式无法精确计算你个人 TDEE，仅提供原则性参考。
                    """.trim();
        }

        return generic(ctx);
    }

    private String generic(ChatUserContext ctx) {
        return """
                我当前运行在 **固定话术模式**（未使用在线大模型），无法针对长问题做深度推理，但可以先给你一份通用框架：

                1. **先看清现状**：你是否有进行中的计划、今天系统里安排了哪些动作（见文末摘要）。
                2. **目标拆解**：减脂 / 增肌 / 维持 对应的热量与训练侧重点不同。
                3. **执行原则**：循序渐进、动作标准优先、睡眠与营养跟上。
                4. **安全边界**：不适即停，伤病问医。

                你可以换短一点的问题，或包含关键词（如「计划」「打卡」「有氧」「力量」「休息」「饮食」），我会匹配更具体的预设段落。
                """.trim();
    }
}
