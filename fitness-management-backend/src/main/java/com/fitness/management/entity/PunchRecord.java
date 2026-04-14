package com.fitness.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录表
 */
@Data
@TableName("punch_record")
public class PunchRecord {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 计划ID */
    private Long planId;

    /** 计划明细ID，空表示当日计划级打卡 */
    private Long planDetailId;

    /** 打卡日期 */
    private LocalDate punchDate;

    /** 实际完成组数 */
    private Integer actualSets;

    /** 实际使用重量（kg） */
    private BigDecimal actualWeight;

    /** 备注 */
    private String remark;

    /** 打卡时间 */
    private LocalDateTime createTime;
}
