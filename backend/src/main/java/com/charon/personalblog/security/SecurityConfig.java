package com.charon.personalblog.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    UserDetailsService userDetailsService(JdbcClient jdbc) {
        return identity -> jdbc.sql("""
                SELECT id, username, password_hash, role, status
                FROM app_user
                WHERE lower(username) = lower(:identity) OR lower(email) = lower(:identity)
                """).param("identity", identity)
                .query((rs, row) -> new BlogPrincipal(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        "ACTIVE".equals(rs.getString("status"))))
                .optional()
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("user"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieName("XSRF-TOKEN");
        csrf.setHeaderName("X-XSRF-TOKEN");
        return http
                .csrf(config -> config.csrfTokenRepository(csrf))
                .formLogin(config -> config.disable())
                .httpBasic(config -> config.disable())
                .logout(config -> config.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/api/v1/auth/csrf", "/api/v1/auth/register",
                                "/api/v1/auth/verify-email", "/api/v1/auth/login",
                                "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/v1/site/**", "/api/v1/contents/**", "/api/v1/users/**",
                                "/api/v1/categories/**", "/api/v1/tags/**", "/api/v1/archive/**",
                                "/api/v1/search/**", "/api/v1/media/**", "/api/v1/comments/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"请先登录\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            response.getWriter().write("{\"title\":\"Forbidden\",\"status\":403,\"detail\":\"没有执行此操作的权限\"}");
                        }))
                .build();
    }
}
