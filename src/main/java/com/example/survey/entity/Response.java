package com.example.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("response")
public class Response {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long surveyId;

    private String respondentIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submitTime;
}
