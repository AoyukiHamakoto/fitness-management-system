package com.fitness.management.dto.checkin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 每日训练打卡提交参数（论文用例：关联计划明细与实做数据）。
 */
@Data
public class CheckInDto {

    /** 计划详情ID（对应当日具体训练项） */
    @NotNull(message = "计划详情ID不能为空")
    private Long planDetailId;

    /** 实际完成组数 */
    @NotNull(message = "完成组数不能为空")
    @Positive(message = "完成组数须为正整数")
    private Integer actualSets;

    /** 实际使用重量（kg），徒手或无负重可为 0 */
    @NotNull(message = "重量不能为空")
    @DecimalMin(value = "0", message = "重量不能为负数")
    private BigDecimal actualWeight;
}
