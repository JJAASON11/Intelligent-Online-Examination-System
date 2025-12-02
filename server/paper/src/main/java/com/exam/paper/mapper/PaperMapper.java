package com.exam.paper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.paper.model.Paper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {}

