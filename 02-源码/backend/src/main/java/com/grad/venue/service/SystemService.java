package com.grad.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.venue.common.BizException;
import com.grad.venue.common.PageResult;
import com.grad.venue.entity.*;
import com.grad.venue.mapper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/** 系统管理：用户、角色、菜单、字典、消息、日志、文件上传 */
@Service
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDictMapper dictMapper;
    private final SysMessageMapper msgMapper;
    private final SysLogMapper logMapper;
    private final PasswordEncoder encoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public SystemService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                         SysDictMapper dictMapper, SysMessageMapper msgMapper, SysLogMapper logMapper,
                         PasswordEncoder encoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.dictMapper = dictMapper;
        this.msgMapper = msgMapper;
        this.logMapper = logMapper;
        this.encoder = encoder;
    }

    public PageResult<SysUser> pageUser(long page, long size, String keyword, String role) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.like(keyword != null && !keyword.isBlank(), SysUser::getRealName, keyword)
                .eq(role != null && !role.isBlank(), SysUser::getRole, role)
                .orderByAsc(SysUser::getId);
        return PageResult.of(userMapper.selectPage(new Page<>(page, size), w));
    }

    public void saveUser(SysUser u) {
        if (u.getUsername() == null || u.getUsername().isBlank()) {
            throw new BizException("账号必填");
        }
        Long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, u.getUsername()).ne(u.getId() != null, SysUser::getId, u.getId()));
        if (dup != null && dup > 0) {
            throw new BizException("账号已存在");
        }
        if (u.getId() == null) {
            u.setPassword(encoder.encode(u.getPassword() == null ? "123456" : u.getPassword()));
            u.setStatus(1);
            userMapper.insert(u);
        } else {
            SysUser old = userMapper.selectById(u.getId());
            u.setPassword(old.getPassword());
            userMapper.updateById(u);
        }
    }

    public void resetPassword(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        u.setPassword(encoder.encode("123456"));
        userMapper.updateById(u);
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public void saveRole(SysRole r) {
        if (r.getId() == null) {
            roleMapper.insert(r);
        } else {
            roleMapper.updateById(r);
        }
    }

    public List<SysMenu> menus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortNo));
    }

    /** 为角色分配菜单：直接更新菜单上的 roleCodes，admin 始终可见全部 */
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        List<SysMenu> all = menuMapper.selectList(null);
        for (SysMenu m : all) {
            String codes = m.getRoleCodes() == null ? "" : m.getRoleCodes();
            boolean has = codes.contains(role.getRoleCode());
            boolean should = menuIds.contains(m.getId());
            if (should && !has) {
                m.setRoleCodes(codes.isBlank() ? role.getRoleCode() : codes + "," + role.getRoleCode());
                menuMapper.updateById(m);
            } else if (!should && has) {
                String next = List.of(codes.split(",")).stream()
                        .filter(c -> !c.equals(role.getRoleCode())).reduce((a, b) -> a + "," + b).orElse("");
                m.setRoleCodes(next);
                menuMapper.updateById(m);
            }
        }
    }

    public List<SysDict> dicts(String type) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(type != null && !type.isBlank(), SysDict::getDictType, type)
                .eq(SysDict::getStatus, 1).orderByAsc(SysDict::getSortNo));
    }

    public PageResult<SysMessage> messages(long page, long size) {
        return PageResult.of(msgMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysMessage>().orderByDesc(SysMessage::getId)));
    }

    public void readMessage(Long id) {
        SysMessage m = msgMapper.selectById(id);
        if (m != null) {
            m.setReadFlag(1);
            msgMapper.updateById(m);
        }
    }

    public PageResult<SysLog> logs(long page, long size, String keyword) {
        return PageResult.of(logMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysLog>().like(keyword != null && !keyword.isBlank(), SysLog::getBizNo, keyword)
                        .orderByDesc(SysLog::getId)));
    }

    /** 文件上传：按业务目录归档，返回可直接访问的相对路径 */
    public String upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".pdf").contains(ext)) {
            throw new BizException("仅支持 jpg/png/webp/pdf 格式");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BizException("文件不得超过 10MB");
        }
        try {
            String dir = uploadDir + (bizType == null ? "common" : bizType) + "/";
            Path p = Paths.get(dir).toAbsolutePath().normalize();
            Files.createDirectories(p);
            String fileName = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
            File dest = p.resolve(fileName).toFile();
            file.transferTo(dest);
            return "/uploads/" + (bizType == null ? "common" : bizType) + "/" + fileName;
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
    }
}
