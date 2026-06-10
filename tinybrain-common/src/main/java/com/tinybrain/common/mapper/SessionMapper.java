package com.tinybrain.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinybrain.common.entity.Session;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
