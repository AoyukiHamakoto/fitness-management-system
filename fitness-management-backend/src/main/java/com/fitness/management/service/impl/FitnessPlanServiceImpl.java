package com.fitness.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.management.entity.FitnessPlan;
import com.fitness.management.mapper.FitnessPlanMapper;
import com.fitness.management.service.FitnessPlanService;
import org.springframework.stereotype.Service;

@Service
public class FitnessPlanServiceImpl extends ServiceImpl<FitnessPlanMapper, FitnessPlan> implements FitnessPlanService {
}
