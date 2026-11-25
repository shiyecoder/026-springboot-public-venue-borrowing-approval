package com.grad.venue.controller;

import com.grad.venue.common.R;
import com.grad.venue.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 看板入口，与 /api/campus-venue/dashboard 等价，便于前端统一调用 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public R<Map<String, Object>> index(@RequestParam(required = false, defaultValue = "30d") String range) {
        return R.ok(dashboardService.index(range));
    }
}
