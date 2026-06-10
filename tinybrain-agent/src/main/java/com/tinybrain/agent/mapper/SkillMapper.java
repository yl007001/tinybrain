package com.tinybrain.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinybrain.agent.entity.Skill;
import org.apache.ibatis.annotations.Mapper;

/**
 * Skill 技能配置 Mapper
 */
@Mapper
public interface SkillMapper extends BaseMapper<Skill> {
}
