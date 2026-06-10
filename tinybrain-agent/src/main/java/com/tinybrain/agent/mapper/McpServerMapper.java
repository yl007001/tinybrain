package com.tinybrain.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinybrain.agent.entity.McpServer;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 服务器配置 Mapper
 */
@Mapper
public interface McpServerMapper extends BaseMapper<McpServer> {
}
