package com.example.survey.service;
import com.example.survey.entity.Question;

import java.util.List;
public interface QuestionService {
    Question add(Question question);
    Question update(Question question);
    void delete(Long id);
    List<Question> getBySurveyId(Long surveyId);
}
