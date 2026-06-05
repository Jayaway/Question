package com.example.survey.controller;

import com.example.survey.common.Result;
import com.example.survey.service.ResponseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/survey")
public class ResponseController {

    private final ResponseService responseService;

    public ResponseController(ResponseService responseService) {
        this.responseService = responseService;
    }

    /**
     * 提交答卷。不需要登录，匿名可填。
     *
     * 请求体格式：
     * {
     *   "answers": {
     *     "1": "满意",
     *     "2": "价格,卫生",
     *     "3": "希望增加麻辣烫窗口"
     *   }
     * }
     */
    @PostMapping("/{id}/submit")
    public Result<?> submit(@PathVariable Long id,
                            @RequestBody Map<String, Map<Long, String>> body,
                            HttpServletRequest request) {
        Map<Long, String> answers = body.get("answers");
        String ip = request.getRemoteAddr();    // 获取填写者 IP
        responseService.submit(id, answers, ip);
        return Result.success();
    }
}
