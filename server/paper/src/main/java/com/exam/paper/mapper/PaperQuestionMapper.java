package com.exam.paper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.paper.model.PaperQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {}

