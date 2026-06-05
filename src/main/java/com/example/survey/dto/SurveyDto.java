package com.example.survey.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SurveyDto {
    private Long id;

    @NotBlank(message = "问卷标题不能为空")
    private String title;

    private String description;

}
