package com.grad.venue.common;

/** 当前登录人上下文，由 JwtFilter 写入，Service 层按数据范围过滤时使用 */
public class UserContext {

    public record CurrentUser(Long id, String username, String realName, String role) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser u) {
        HOLDER.set(u);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long id() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.id();
    }

    public static String role() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.role();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.realName();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
