package com.example.survey.controller;

import com.example.survey.common.Result;
import com.example.survey.service.ResponseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/survey")
public class StatsController {

    private final ResponseService responseService;

    public StatsController(ResponseService responseService) {
        this.responseService = responseService;
    }

    @GetMapping("/{id}/stats")
    public Result<?> stats(@PathVariable Long id) {
        var stats = responseService.stats(id);
        return Result.success(Map.of(
                "stats", stats,
                "total", stats.isEmpty() ? 0 :
                        stats.get(0).getOptions().stream()
                                .mapToLong(io -> {
                                    var opt = stats.get(0).getOptions();
                                    return opt.isEmpty() ? 0 :
                                            opt.stream().mapToLong(
                                                    o -> o.getCount()).sum();
                                }).findFirst().orElse(0)
        ));
    }
}
