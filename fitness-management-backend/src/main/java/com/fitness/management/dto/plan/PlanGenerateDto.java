package com.fitness.management.dto.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 个性化健身计划生成入参（论文用例：体征与训练偏好）。
 */
@Data
public class PlanGenerateDto {

    /** 身高（cm） */
    @NotNull(message = "身高不能为空")
    @Positive(message = "身高须为正数")
    @Max(value = 250, message = "身高数值不合法")
    private BigDecimal heightCm;

    /** 体重（kg） */
    @NotNull(message = "体重不能为空")
    @Positive(message = "体重须为正数")
    @Max(value = 500, message = "体重数值不合法")
    private BigDecimal weightKg;

    /** 年龄 */
    @NotNull(message = "年龄不能为空")
    @Min(value = 10, message = "年龄须不小于10")
    @Max(value = 120, message = "年龄数值不合法")
    private Integer age;

    /** 性别：男 / 女 */
    @NotBlank(message = "性别不能为空")
    @Size(max = 10, message = "性别长度不合法")
    private String gender;

    /** 健身目标，如减脂、增肌、保持健康等 */
    @NotBlank(message = "健身目标不能为空")
    @Size(max = 200, message = "健身目标过长")
    private String fitnessGoal;

    /** 运动经验，如零基础、初级、中级等 */
    @NotBlank(message = "运动经验不能为空")
    @Size(max = 200, message = "运动经验描述过长")
    private String exerciseExperience;

    /** 每周可训练天数 */
    @NotNull(message = "每周训练天数不能为空")
    @Min(value = 1, message = "每周至少训练1天")
    @Max(value = 7, message = "每周训练天数不能超过7")
    private Integer weeklyTrainingDays;

    /** 每次训练时长（分钟） */
    @NotNull(message = "每次训练时长不能为空")
    @Min(value = 10, message = "每次训练至少10分钟")
    @Max(value = 300, message = "每次训练时长过长")
    private Integer sessionDurationMinutes;
}
