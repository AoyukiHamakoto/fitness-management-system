package com.fitness.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.management.entity.PlanDetail;
import com.fitness.management.mapper.PlanDetailMapper;
import com.fitness.management.service.PlanDetailService;
import org.springframework.stereotype.Service;

@Service
public class PlanDetailServiceImpl extends ServiceImpl<PlanDetailMapper, PlanDetail> implements PlanDetailService {
}
