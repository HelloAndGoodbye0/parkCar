package com.parkcar.security;

/**
 * 当前用户上下文（ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static String username() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    public static boolean isAdmin() {
        LoginUser u = HOLDER.get();
        return u != null && u.hasRole("ADMIN");
    }

    public static void clear() {
        HOLDER.remove();
    }
}
