package com.parkcar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkcar.common.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JWT 鉴权拦截器
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }
        Claims claims = token == null ? null : jwtUtil.parse(token);
        if (claims == null) {
            writeUnauthorized(response);
            return false;
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String rolesStr = claims.get("roles", String.class);
        Set<String> roles = new LinkedHashSet<>();
        if (rolesStr != null && !rolesStr.isEmpty()) {
            roles.addAll(Arrays.asList(rolesStr.split(",")));
        }

        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setUsername(username);
        user.setRoles(roles);
        UserContext.set(user);

        // 角色校验
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            boolean allowed = Arrays.stream(requireRole.value()).anyMatch(roles::contains);
            if (!allowed) {
                UserContext.clear();
                writeForbidden(response);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(Result.CODE_UNAUTHORIZED, "未登录或token已过期")));
    }

    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(Result.CODE_FORBIDDEN, "无操作权限")));
    }
}
