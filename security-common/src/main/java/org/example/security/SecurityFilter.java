package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Extract user info from headers (set by API Gateway)
            String userIdHeader = request.getHeader("X-User-Id");
            String emailHeader = request.getHeader("X-User-Email");
            String roleHeader = request.getHeader("X-User-Role");

            if (userIdHeader != null && emailHeader != null && roleHeader != null) {
                SecurityContext context = new SecurityContext();
                context.setUserId(Long.parseLong(userIdHeader));
                context.setEmail(emailHeader);
                context.setRole(roleHeader);
                SecurityContext.set(context);
                
                log.debug("Security context set for user: {} ({})", emailHeader, roleHeader);
            }

            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }
}
