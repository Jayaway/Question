package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsDto {
    private Long questionId;
    private String questionTitle;
    private Integer type;

    private List<OptionCount> options;  // 选择题用
    private List<String> texts;          // 文本题用

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionCount {
        private String option;
        private Long count;
    }
}
