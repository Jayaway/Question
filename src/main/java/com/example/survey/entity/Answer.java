package com.example.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("answer")
public class Answer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long responseId;

    private Long questionId;

    private String content;     // 答案内容。单选是 "A"，多选是 "A,B"，文本是用户输入的文字
}
