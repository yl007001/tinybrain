package com.tinybrain.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.tinybrain.knowledge.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档 Mapper
 * <p>
 * 继承 BaseMapper 自动获得 CRUD。
 * 复杂查询（全文检索、标签筛选）通过 XML 或 @Select 实现。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 分页查询用户文档（支持标题模糊搜索 + 标签筛选）
     */
    IPage<Document> selectUserDocuments(IPage<Document> page,
                                        @Param("userId") Long userId,
                                        @Param("keyword") String keyword,
                                        @Param("tags") List<String> tags,
                                        @Param("status") Integer status);
}
