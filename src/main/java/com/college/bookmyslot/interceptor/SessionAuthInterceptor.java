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


        if (
                path.startsWith("/api/auth") ||
                        path.startsWith("/api/public") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/uploads")
        ) {
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("USER_ID") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Login required");
            return false;
        }

        return true;
    }
}
