package com.yeniden.identity.security;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.auth.AuthSessionService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveSessionFilter extends OncePerRequestFilter {
    private final AuthSessionService sessions;
    private final ProblemWriter problems;
    public ActiveSessionFilter(AuthSessionService sessions, ProblemWriter problems) {
        this.sessions = sessions;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            try {
                sessions.assertActive(UUID.fromString(auth.getToken().getSubject()),
                        UUID.fromString(auth.getToken().getClaimAsString("sid")));
            } catch (BaseException ex) {
                SecurityContextHolder.clearContext();
                problems.write(response, ex.getHttpStatus(), ex.getErrorCode());
                return;
            } catch (IllegalArgumentException | NullPointerException ex) {
                SecurityContextHolder.clearContext();
                problems.write(response, 401, "unauthorized");
                return;
            } catch (DataAccessException | org.springframework.transaction.TransactionException ex) {
                SecurityContextHolder.clearContext();
                problems.write(response, 503, "service_unavailable");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

