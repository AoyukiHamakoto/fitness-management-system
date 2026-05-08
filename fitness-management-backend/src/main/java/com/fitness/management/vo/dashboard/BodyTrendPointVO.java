package com.fitness.management.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BodyTrendPointVO {

    private LocalDate date;

    private BigDecimal weight;

    /** BMI = 体重(kg) / 身高(m)^2（当前为估算） */
    private BigDecimal bmi;

    /** 体脂率估算值（用于看板趋势展示） */
    private BigDecimal bodyFatRate;
}
