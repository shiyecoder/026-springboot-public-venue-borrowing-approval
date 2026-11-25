package com.grad.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.venue.common.BizException;
import com.grad.venue.common.JwtUtil;
import com.grad.venue.entity.SysLog;
import com.grad.venue.entity.SysMenu;
import com.grad.venue.entity.SysUser;
import com.grad.venue.mapper.SysLogMapper;
import com.grad.venue.mapper.SysMenuMapper;
import com.grad.venue.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 登录鉴权：BCrypt 校验、连续 5 次失败锁定 10 分钟、按角色下发菜单 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysLogMapper logMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    /** 失败计数：username -> [失败次数, 锁定到期时间戳] */
    private final Map<String, Object[]> failMap = new ConcurrentHashMap<>();

    public AuthService(SysUserMapper userMapper, SysMenuMapper menuMapper, SysLogMapper logMapper,
                       PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.logMapper = logMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String username, String password) {
        Object[] rec = failMap.get(username);
        if (rec != null && rec[1] != null && System.currentTimeMillis() < (Long) rec[1]) {
            throw new BizException(423, "账号已锁定，请 10 分钟后再试");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !encoder.matches(password, user.getPassword())) {
            int n = rec == null ? 1 : (Integer) rec[0] + 1;
            failMap.put(username, new Object[]{n, n >= 5 ? System.currentTimeMillis() + 600_000L : null});
            throw new BizException(401, n >= 5 ? "密码连续错误 5 次，账号锁定 10 分钟" : "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已停用，请联系管理员");
        }
        failMap.remove(username);
        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);

        SysLog log = new SysLog();
        log.setUserId(user.getId());
        log.setUserName(user.getRealName());
        log.setAction("登录");
        log.setBizNo("AUTH");
        log.setDetail("账号 " + username + " 登录成功");
        logMapper.insert(log);

        Map<String, Object> res = new HashMap<>();
        res.put("token", jwtUtil.create(user.getId(), user.getUsername(), user.getRole(), user.getRealName()));
        res.put("user", user);
        res.put("menus", menus(user.getRole()));
        return res;
    }

    /** 菜单按角色过滤，roleCodes 以逗号存储，admin 可见全部 */
    public List<SysMenu> menus(String role) {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getHidden, 0)
                        .orderByAsc(SysMenu::getSortNo))
                .stream()
                .filter(m -> "admin".equals(role) || (m.getRoleCodes() != null && m.getRoleCodes().contains(role)))
                .toList();
    }

    public SysUser profile(Long id) {
        return userMapper.selectById(id);
    }

    public void updateProfile(Long id, String realName, String phone, String avatar) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        if (realName != null && !realName.isBlank()) {
            u.setRealName(realName.trim());
        }
        if (phone != null) {
            u.setPhone(phone.trim());
        }
        if (avatar != null && !avatar.isBlank()) {
            u.setAvatar(avatar);
        }
        userMapper.updateById(u);
    }

    public void changePassword(Long id, String oldPwd, String newPwd) {
        SysUser u = userMapper.selectById(id);
        if (u == null || !encoder.matches(oldPwd, u.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (newPwd == null || newPwd.length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        u.setPassword(encoder.encode(newPwd));
        userMapper.updateById(u);
    }
}
