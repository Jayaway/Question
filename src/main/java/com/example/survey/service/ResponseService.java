package com.example.survey.service;

import com.example.survey.dto.StatsDto;
import java.util.List;
import java.util.Map;

public interface ResponseService {
    void submit(Long surveyId, Map<Long, String> answers, String ip);
    List<StatsDto> stats(Long surveyId);
}
