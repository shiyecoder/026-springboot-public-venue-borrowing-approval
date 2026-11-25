package com.grad.venue.config;

import com.grad.venue.common.JwtUtil;
import com.grad.venue.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** 解析 Authorization 头中的 JWT，写入 SecurityContext 与 UserContext */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims c = jwtUtil.parse(auth.substring(7));
                String role = c.get("role", String.class);
                Long uid = c.get("uid", Number.class).longValue();
                String realName = c.get("realName", String.class);
                UserContext.set(new UserContext.CurrentUser(uid, c.getSubject(), realName, role));
                var token = new UsernamePasswordAuthenticationToken(c.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(token);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
