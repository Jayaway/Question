package com.example.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long surveyId;

    private Integer type;           // 1=单选 2=多选 3=文本

    private String title;

    private String options;         // JSON 字符串，如 ["A","B","C"]

    private Integer sortOrder;

    private Integer required;       // 0=非必填 1=必填
}
