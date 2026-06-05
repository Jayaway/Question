package com.example.survey.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.survey.common.Result;
import com.example.survey.entity.Question;
import com.example.survey.entity.Survey;
import com.example.survey.mapper.QuestionMapper;
import com.example.survey.mapper.SurveyMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/survey")
public class SurveyController {

    private final SurveyMapper surveyMapper;
    private final QuestionMapper questionMapper;

    public SurveyController(SurveyMapper surveyMapper, QuestionMapper questionMapper) {
        this.surveyMapper = surveyMapper;
        this.questionMapper = questionMapper;
    }

    @GetMapping("/public")
    public Result<?> publicSurveys() {
        List<Survey> surveys = surveyMapper.selectList(
                new LambdaQueryWrapper<Survey>()
                        .eq(Survey::getStatus, 1)
                        .orderByDesc(Survey::getUpdateTime)
                        .orderByDesc(Survey::getId));
        return Result.success(surveys);
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Survey survey = surveyMapper.selectById(id);
        if (survey == null) {
            return Result.error("问卷不存在");
        }

        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, id)
                        .orderByAsc(Question::getSortOrder)
                        .orderByAsc(Question::getId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("survey", survey);
        data.put("questions", questions);
        return Result.success(data);
    }
}
