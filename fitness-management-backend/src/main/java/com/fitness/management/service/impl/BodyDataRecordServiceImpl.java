package com.fitness.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.management.entity.BodyDataRecord;
import com.fitness.management.mapper.BodyDataRecordMapper;
import com.fitness.management.service.BodyDataRecordService;
import org.springframework.stereotype.Service;

@Service
public class BodyDataRecordServiceImpl extends ServiceImpl<BodyDataRecordMapper, BodyDataRecord> implements BodyDataRecordService {
}
