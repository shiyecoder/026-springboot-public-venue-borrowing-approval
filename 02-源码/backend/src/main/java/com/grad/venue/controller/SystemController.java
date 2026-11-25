package com.grad.venue.controller;

import com.grad.venue.common.PageResult;
import com.grad.venue.common.R;
import com.grad.venue.entity.*;
import com.grad.venue.service.SystemService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 系统管理：用户、角色、菜单、字典、消息、日志、上传 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/user/page")
    public R<PageResult<SysUser>> users(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String role) {
        return R.ok(systemService.pageUser(page, size, keyword, role));
    }

    @PostMapping("/user")
    public R<Void> saveUser(@RequestBody SysUser u) {
        systemService.saveUser(u);
        return R.ok();
    }

    @PostMapping("/user/{id}/reset")
    public R<Void> reset(@PathVariable Long id) {
        systemService.resetPassword(id);
        return R.ok();
    }

    @GetMapping("/role")
    public R<List<SysRole>> roles() {
        return R.ok(systemService.roles());
    }

    @PostMapping("/role")
    public R<Void> saveRole(@RequestBody SysRole r) {
        systemService.saveRole(r);
        return R.ok();
    }

    @GetMapping("/menu")
    public R<List<SysMenu>> menus() {
        return R.ok(systemService.menus());
    }

    @PostMapping("/role/{id}/menus")
    public R<Void> assign(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        systemService.assignMenus(id, body.getOrDefault("menuIds", List.of()));
        return R.ok();
    }

    @GetMapping("/dict")
    public R<List<SysDict>> dicts(@RequestParam(required = false) String type) {
        return R.ok(systemService.dicts(type));
    }

    @GetMapping("/message/page")
    public R<PageResult<SysMessage>> messages(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.messages(page, size));
    }

    @PostMapping("/message/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        systemService.readMessage(id);
        return R.ok();
    }

    @GetMapping("/log/page")
    public R<PageResult<SysLog>> logs(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(systemService.logs(page, size, keyword));
    }

    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(defaultValue = "common") String bizType) {
        return R.ok(systemService.upload(file, bizType));
    }
}
