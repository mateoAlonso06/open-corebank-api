package com.banking.system.auth.infraestructure.config;

import jakarta.servlet.http.HttpServletResponse;
import com.banking.system.auth.infraestructure.adapter.out.filter.CsrfTokenFilter;
import com.banking.system.auth.infraestructure.adapter.out.filter.JwtAuthenticationFilter;
import com.banking.system.auth.infraestructure.adapter.out.filter.RateLimitFilter;
import com.banking.system.common.infraestructure.filter.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${security.csp.policy:default-src 'none'; frame-ancestors 'none';}")
    private String cspPolicy;

    private final CorrelationIdFilter correlationIdFilter;
    private final CsrfTokenFilter csrfTokenFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<RateLimitFilter> rateLimitFilterProvider;

    /**
     * Actuator endpoints are served on a separate management port (9090),
     * isolated from the public API. No authentication needed - network
     * isolation (Docker network / EC2 security groups) provides security.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        /* These headers protect the client-side interaction and enforce secure communication.
         */
        http.headers(headers -> headers
                // CSP: Configurable per environment (strict in prod, permissive in dev for Swagger UI)
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives(cspPolicy))
                // HSTS: Instructs the browser to only use HTTPS for the next year.
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
                // Anti-Clickjacking: Disables embedding the API responses in iframes.
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                // Prevent MIME type sniffing to reduce XSS risks.
                .contentTypeOptions(Customizer.withDefaults())
                // Prevent caching of sensitive data in the browser.
                .cacheControl(Customizer.withDefaults())
        );

        HttpSecurity httpSecurity = http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage())
                        )
                );

        // Add rate limit filter only if enabled
        rateLimitFilterProvider.ifAvailable(rateLimitFilter -> httpSecurity.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class));

        return httpSecurity
                // CorrelationId filter FIRST - ensures all subsequent filters and logs have the correlation ID
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                // CSRF filter BEFORE JWT - rejects invalid CSRF before authentication
                .addFilterBefore(csrfTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // For: JWT; JSON/XML; Content negotiation; AJAX requests; Request tracing
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "X-Correlation-ID", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        // Expose headers that client-side JavaScript can read from responses
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
