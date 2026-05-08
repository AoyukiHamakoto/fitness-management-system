package com.fitness.management.vo.dashboard;

import lombok.Data;

@Data
public class CompletionRateVO {

    private Integer completed;

    private Integer pending;

    private Integer overCompleted;
}
