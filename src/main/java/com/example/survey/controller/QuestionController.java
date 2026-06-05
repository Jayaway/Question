package com.example.survey.controller;

import com.example.survey.common.Result;
import com.example.survey.entity.Question;
import com.example.survey.service.QuestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public Result<?> add(@RequestBody Question question) {
        return Result.success(questionService.add(question));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                            @RequestBody Question question) {
        question.setId(id);
        return Result.success(questionService.update(question));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }
}
