package com.fitness.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.management.entity.PunchRecord;
import com.fitness.management.mapper.PunchRecordMapper;
import com.fitness.management.service.PunchRecordService;
import org.springframework.stereotype.Service;

@Service
public class PunchRecordServiceImpl extends ServiceImpl<PunchRecordMapper, PunchRecord> implements PunchRecordService {
}
