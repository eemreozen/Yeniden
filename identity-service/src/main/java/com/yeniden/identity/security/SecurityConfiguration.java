package com.yeniden.identity.security;

import com.yeniden.identity.auth.AuthSessionService;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain identitySecurity(HttpSecurity http, AuthSessionService sessions,
                                         ProblemWriter problems) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);

        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/otp/request", "/api/v1/auth/otp/verify",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/identity/health").permitAll()
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers(new RegexRequestMatcher(
                                "/api/v1/users/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "GET")).permitAll()
                        .requestMatchers(new RegexRequestMatcher(
                                "/api/v1/users/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/account-created-at", "GET")).permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) -> problems.write(res, 401, "unauthorized"))
                        .accessDeniedHandler((req, res, ex) -> problems.write(res, 403, "forbidden")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> problems.write(res, 401, "unauthorized"))
                        .accessDeniedHandler((req, res, ex) -> problems.write(res, 403, "forbidden")))
                .addFilterAfter(new ActiveSessionFilter(sessions, problems), BearerTokenAuthenticationFilter.class)
                .build();
    }
}
