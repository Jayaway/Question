package com.example.survey.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.survey.entity.Question;
import com.example.survey.mapper.QuestionMapper;
import com.example.survey.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;

    public QuestionServiceImpl(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Override
    public Question add(Question question) {
        questionMapper.insert(question);
        return question;
    }

    @Override
    public Question update(Question question) {
        Question exist = questionMapper.selectById(question.getId());
        if (exist == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        questionMapper.updateById(question);
        return questionMapper.selectById(question.getId());
    }

    @Override
    public void delete(Long id) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        questionMapper.deleteById(id);
    }

    @Override
    public List<Question> getBySurveyId(Long surveyId) {
        return questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, surveyId)
                        .orderByAsc(Question::getSortOrder));
    }
}
