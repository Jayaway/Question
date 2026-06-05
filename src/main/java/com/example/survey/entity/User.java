package com.example.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;   // 存 BCrypt 加密后的密文

    private String email;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
