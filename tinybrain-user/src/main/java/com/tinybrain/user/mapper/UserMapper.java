package com.tinybrain.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinybrain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD + 分页能力。
 * 复杂查询可在此定义 XML 映射方法。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
