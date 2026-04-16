package org.example.security;

import lombok.Data;

@Data
public class SecurityContext {
    private Long userId;
    private String email;
    private String role;

    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    public static SecurityContext get() {
        SecurityContext context = contextHolder.get();
        if (context == null) {
            context = new SecurityContext();
            contextHolder.set(context);
        }
        return context;
    }

    public static void set(SecurityContext context) {
        contextHolder.set(context);
    }

    public static void clear() {
        contextHolder.remove();
    }

    public boolean hasRole(String role) {
        return this.role != null && this.role.equals(role);
    }

    public boolean hasAnyRole(String... roles) {
        if (this.role == null) {
            return false;
        }
        for (String role : roles) {
            if (this.role.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
