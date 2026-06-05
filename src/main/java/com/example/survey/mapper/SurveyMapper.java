package com.example.survey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.survey.entity.Survey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SurveyMapper extends BaseMapper<Survey> {
}
