package com.fitness.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 身体数据记录表
 */
@Data
@TableName("body_data_record")
public class BodyDataRecord {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 体重（kg） */
    private BigDecimal weight;

    /** 创建时间 */
    private LocalDateTime createTime;
}
