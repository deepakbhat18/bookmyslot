package com.college.bookmyslot.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ✅ ALLOW PREFLIGHT REQUESTS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // ✅ PUBLIC APIs
        if (
                path.startsWith("/api/auth") ||
                        path.startsWith("/api/public") ||
                        path.startsWith("/swagger") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/uploads")
        ) {
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("USER_ID") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Login required\",\"data\":null}");
            return false;
        }

        return true;
    }
}

