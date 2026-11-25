package com.grad.venue.controller;

import com.grad.venue.common.PageResult;
import com.grad.venue.common.R;
import com.grad.venue.entity.*;
import com.grad.venue.service.OpsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 现场作业：入场交接、活动巡场、撤场验收、线下费用 */
@RestController
@RequestMapping("/api/venue-ops")
public class OpsController {

    private final OpsService opsService;

    public OpsController(OpsService opsService) {
        this.opsService = opsService;
    }

    @GetMapping("/handover/page")
    public R<PageResult<HandoverRecord>> handoverPage(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size) {
        return R.ok(opsService.handoverPage(page, size));
    }

    @PostMapping("/{applyId}/handover")
    public R<HandoverRecord> handover(@PathVariable Long applyId, @RequestBody HandoverRecord data) {
        return R.ok(opsService.handover(applyId, data));
    }

    @GetMapping("/patrol/page")
    public R<PageResult<PatrolRecord>> patrolPage(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return R.ok(opsService.patrolPage(page, size));
    }

    @PostMapping("/{applyId}/patrol")
    public R<PatrolRecord> patrol(@PathVariable Long applyId, @RequestBody PatrolRecord data) {
        return R.ok(opsService.patrol(applyId, data));
    }

    @GetMapping("/accept/page")
    public R<PageResult<VenueAcceptance>> acceptPage(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(opsService.acceptPage(page, size));
    }

    @PostMapping("/{applyId}/accept")
    public R<VenueAcceptance> accept(@PathVariable Long applyId, @RequestBody VenueAcceptance data) {
        return R.ok(opsService.accept(applyId, data));
    }

    @PostMapping("/accept/{id}/review")
    public R<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        opsService.reviewAcceptance(id, amount, String.valueOf(body.getOrDefault("remark", "复核确认")));
        return R.ok();
    }

    @GetMapping("/fee/page")
    public R<PageResult<FeeRecord>> feePage(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String feeType,
                                            @RequestParam(required = false) String payStatus) {
        return R.ok(opsService.feePage(page, size, feeType, payStatus));
    }

    @PostMapping("/fee")
    public R<FeeRecord> saveFee(@RequestBody FeeRecord f) {
        return R.ok(opsService.saveFee(f));
    }
}
