package com.fitness.management.chat;

import lombok.Data;

/**
 * 智能对话所需的业务侧上下文（当前计划、今日任务、体测、连续打卡等）。
 */
@Data
public class ChatUserContext {

    /** 当前进行中计划摘要，若无则为说明文案 */
    private String currentPlanSummary;

    /** 今日训练任务摘要 */
    private String todayTasksSummary;

    /** 最近一条身体数据摘要 */
    private String latestBodyDataSummary;

    /** 连续打卡天数 */
    private int streakDays;
}
