package com.fitness.management.ai;

/**
 * 大模型流式输出回调：逐片段（字符/子串）推送、完整结束、异常。
 */
public interface AIResponseHandler {

    /**
     * 收到增量文本（本项目中按字符拆分以实现逐字效果）。
     */
    void onToken(String token);

    /**
     * 流式结束，fullContent 为拼接后的助手全文。
     */
    void onComplete(String fullContent);

    /**
     * 流式或解析过程中出错。
     */
    void onError(Throwable error);
}
