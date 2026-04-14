package com.fitness.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.management.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
