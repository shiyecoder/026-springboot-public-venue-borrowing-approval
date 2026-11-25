package com.grad.venue.controller;

import com.grad.venue.common.R;
import com.grad.venue.entity.PrepRule;
import com.grad.venue.entity.PrepScore;
import com.grad.venue.service.PrepService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 创新点：准备清单规则配置、计算、留痕与人工修正 */
@RestController
@RequestMapping("/api/venue-prep")
public class PrepController {

    private final PrepService prepService;

    public PrepController(PrepService prepService) {
        this.prepService = prepService;
    }

    @GetMapping("/rule")
    public R<List<PrepRule>> rules() {
        return R.ok(prepService.rules());
    }

    @PutMapping("/rule")
    public R<List<PrepRule>> saveRules(@RequestBody List<PrepRule> rules) {
        return R.ok(prepService.saveRules(rules));
    }

    @PostMapping("/{applyId}/evaluate")
    public R<Map<String, Object>> evaluate(@PathVariable Long applyId) {
        return R.ok(prepService.evaluate(applyId));
    }

    @GetMapping("/latest/{applyId}")
    public R<PrepScore> latest(@PathVariable Long applyId) {
        return R.ok(prepService.latest(applyId));
    }

    @PostMapping("/score/{id}/adjust")
    public R<PrepScore> adjust(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(prepService.adjust(id, body.get("level"), body.get("reason")));
    }

    @GetMapping("/board")
    public R<Map<String, Object>> board() {
        return R.ok(prepService.board());
    }
}
