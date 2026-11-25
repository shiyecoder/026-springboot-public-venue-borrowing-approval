package com.grad.venue.controller;

import com.grad.venue.common.PageResult;
import com.grad.venue.common.R;
import com.grad.venue.entity.Venue;
import com.grad.venue.entity.VenueItem;
import com.grad.venue.service.DashboardService;
import com.grad.venue.service.VenueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 场地档案：分页、详情、增删改、看板 */
@RestController
@RequestMapping("/api/campus-venue")
public class VenueController {

    private final VenueService venueService;
    private final DashboardService dashboardService;

    public VenueController(VenueService venueService, DashboardService dashboardService) {
        this.venueService = venueService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/page")
    public R<PageResult<Venue>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String campus,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String activityType,
                                     @RequestParam(required = false) Integer minCapacity) {
        return R.ok(venueService.page(page, size, keyword, campus, status, activityType, minCapacity));
    }

    @GetMapping("/options")
    public R<List<Venue>> options() {
        return R.ok(venueService.options());
    }

    @GetMapping("/{id}")
    public R<Venue> detail(@PathVariable Long id) {
        return R.ok(venueService.get(id));
    }

    @GetMapping("/{id}/items")
    public R<List<VenueItem>> items(@PathVariable Long id) {
        return R.ok(venueService.items(id));
    }

    @PostMapping
    public R<Venue> create(@RequestBody Venue v) {
        v.setId(null);
        return R.ok(venueService.save(v));
    }

    @PutMapping("/{id}")
    public R<Venue> update(@PathVariable Long id, @RequestBody Venue v) {
        v.setId(id);
        return R.ok(venueService.save(v));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        venueService.delete(id);
        return R.ok();
    }

    @PostMapping("/item")
    public R<VenueItem> saveItem(@RequestBody VenueItem item) {
        return R.ok(venueService.saveItem(item));
    }

    @GetMapping("/dashboard")
    public R<java.util.Map<String, Object>> dashboard(@RequestParam(required = false, defaultValue = "30d") String range) {
        return R.ok(dashboardService.index(range));
    }
}
