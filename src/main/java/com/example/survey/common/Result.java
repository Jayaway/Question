package com.example.survey.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果。所有 Controller 方法都返回这个类，
 * 前端统一按 { code, message, data } 格式解析。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;       // 200=成功 400=参数错误 401=未登录 500=服务器异常
    private String message;
    private T data;

    // ---- 快速构造成功响应 ----
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // ---- 快速构造失败响应 ----
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }
}
