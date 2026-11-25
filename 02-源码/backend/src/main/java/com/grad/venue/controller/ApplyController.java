package com.grad.venue.controller;

import com.grad.venue.common.PageResult;
import com.grad.venue.common.R;
import com.grad.venue.entity.FeeRecord;
import com.grad.venue.entity.ItemReservation;
import com.grad.venue.entity.VenueApplication;
import com.grad.venue.service.ApplyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 借用申请：列表、详情、保存、提交、审批、流转、物资预留、日历 */
@RestController
@RequestMapping("/api/venue-apply")
public class ApplyController {

    private final ApplyService applyService;

    public ApplyController(ApplyService applyService) {
        this.applyService = applyService;
    }

    @GetMapping("/page")
    public R<PageResult<VenueApplication>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String group,
                                                @RequestParam(required = false) String activityType,
                                                @RequestParam(required = false) Long venueId) {
        return R.ok(applyService.page(page, size, keyword, status, group, activityType, venueId));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(applyService.detail(id));
    }

    @PostMapping
    public R<VenueApplication> create(@RequestBody VenueApplication a) {
        a.setId(null);
        return R.ok(applyService.save(a));
    }

    @PutMapping("/{id}")
    public R<VenueApplication> update(@PathVariable Long id, @RequestBody VenueApplication a) {
        a.setId(id);
        return R.ok(applyService.save(a));
    }

    @PostMapping("/{id}/submit")
    public R<VenueApplication> submit(@PathVariable Long id) {
        return R.ok(applyService.submit(id));
    }

    /** body: { result: 通过/退回/驳回, opinion: 意见 } */
    @PostMapping("/{id}/approve")
    public R<VenueApplication> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(applyService.approve(id, body.getOrDefault("result", "通过"), body.get("opinion")));
    }

    /** body: { target: 活动开始/提交验收/取消, reason: 原因 } */
    @PostMapping("/{id}/transition")
    public R<VenueApplication> transition(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(applyService.transition(id, body.get("target"), body.get("reason")));
    }

    @PostMapping("/{id}/reserve")
    public R<ItemReservation> reserve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long itemId = Long.valueOf(String.valueOf(body.get("itemId")));
        Integer qty = Integer.valueOf(String.valueOf(body.get("qty")));
        return R.ok(applyService.reserve(id, itemId, qty));
    }

    @GetMapping("/conflicts/{id}")
    public R<List<VenueApplication>> conflicts(@PathVariable Long id) {
        return R.ok(applyService.conflicts(applyService.mustGet(id)));
    }

    @GetMapping("/unpaid/{applicantId}")
    public R<List<FeeRecord>> unpaid(@PathVariable Long applicantId) {
        return R.ok(applyService.unpaidDamage(applicantId));
    }

    @GetMapping("/calendar")
    public R<List<Map<String, Object>>> calendar(@RequestParam(required = false) Long venueId,
                                                 @RequestParam(required = false) String month) {
        return R.ok(applyService.calendar(venueId, month));
    }
}
