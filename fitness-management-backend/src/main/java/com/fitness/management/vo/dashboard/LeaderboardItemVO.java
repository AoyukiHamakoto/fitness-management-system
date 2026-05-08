package com.fitness.management.vo.dashboard;

import lombok.Data;

@Data
public class LeaderboardItemVO {

    private Integer rank;

    private Long userId;

    private String nickname;

    private Integer value;
}
