package com.essp.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.essp.security.audit.CorrelationIdFilter;
import com.essp.security.audit.SecurityAuditService;
import com.essp.security.dto.ApiResponse;
import com.essp.security.jwt.JwtAuthenticationFilter;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final SecurityAuditService auditService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CorrelationIdFilter correlationIdFilter;

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtTokenProvider tokenProvider, SecurityAuditService auditService,
                          UserRepository userRepository, ObjectMapper objectMapper,
                          CorrelationIdFilter correlationIdFilter) {
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.correlationIdFilter = correlationIdFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with default strength 10
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userRepository);
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(correlationIdFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable) // Stateless JWT auth
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                // H2 console renders inside a same-origin frame during local development.
                .frameOptions(frame -> frame.sameOrigin())
                .xssProtection(xss -> xss.disable())
                .contentTypeOptions(contentType -> {})
            )
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Local development database console
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Static frontend files
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/favicon.ico").permitAll()
                // Role-based authorization rules
                .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/hr/**").hasAnyRole("HR_ADMIN", "SYSTEM_ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "HR_ADMIN", "SYSTEM_ADMIN")
                .requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE", "MANAGER", "HR_ADMIN", "SYSTEM_ADMIN")
                // All other API endpoints require authenticated token
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    objectMapper.writeValue(response.getOutputStream(),
                            ApiResponse.error("Unauthorized: Authentication token is missing or invalid"));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ANONYMOUS";
                    auditService.logAccessDenied(username, request.getRequestURI(), request.getRemoteAddr());
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    objectMapper.writeValue(response.getOutputStream(),
                            ApiResponse.error("Forbidden: You do not have permission to access this resource"));
                })
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(correlationIdFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "X-Requested-With", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
