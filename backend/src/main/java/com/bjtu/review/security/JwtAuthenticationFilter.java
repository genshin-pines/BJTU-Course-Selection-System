package com.bjtu.review.security;

import com.bjtu.review.utils.JwtUtils;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.entity.Student;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.mapper.StudentMapper;
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
import java.util.Collections;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final StudentMapper studentMapper;
    private final AdminMapper adminMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, StudentMapper studentMapper, AdminMapper adminMapper) {
        this.jwtUtils = jwtUtils;
        this.studentMapper = studentMapper;
        this.adminMapper = adminMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!jwtUtils.validateToken(token)) {
                writeUnauthorized(response, "Invalid token");
                return;
            }

            Long userId = jwtUtils.getUserId(token);
            String role = jwtUtils.getRole(token);
            String username = jwtUtils.getUsername(token);
            String sessionId = jwtUtils.getSessionId(token);

            if (!isCurrentSession(userId, role, sessionId)) {
                writeUnauthorized(response, "Session expired");
                return;
            }

            String authority = "ROLE_" + role.toUpperCase();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.singletonList(new SimpleGrantedAuthority(authority)));
            auth.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }

    private boolean isCurrentSession(Long userId, String role, String sessionId) {
        if (userId == null || role == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        if ("STUDENT".equalsIgnoreCase(role)) {
            Student student = studentMapper.selectById(userId);
            return student != null && Objects.equals(sessionId, student.getCurrentSessionId());
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            Admin admin = adminMapper.selectById(userId);
            return admin != null && Objects.equals(sessionId, admin.getCurrentSessionId());
        }
        return false;
    }
}
