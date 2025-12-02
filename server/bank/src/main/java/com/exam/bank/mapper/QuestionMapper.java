package com.exam.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.bank.model.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}

