package com.fitness.management.vo.dashboard;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckInHeatPointVO {

    private LocalDate date;

    private Integer count;
}
