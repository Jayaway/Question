package com.example.survey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.survey.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
