package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.security.TelegramAuthenticationFilter;
import com.example.dream_stream_bot.security.TelegramAuthenticationProvider;
import com.example.dream_stream_bot.service.admin.AdminUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final TelegramAuthenticationFilter telegramAuthenticationFilter;
    private final TelegramAuthenticationProvider telegramAuthenticationProvider;
    
    @Autowired
    public SecurityConfig(TelegramAuthenticationFilter telegramAuthenticationFilter,
                         TelegramAuthenticationProvider telegramAuthenticationProvider) {
        this.telegramAuthenticationFilter = telegramAuthenticationFilter;
        this.telegramAuthenticationProvider = telegramAuthenticationProvider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http,
                                                DaoAuthenticationProvider adminAuthenticationProvider) throws Exception {
        http
            .securityMatcher("/admin/**", "/api/admin/**", "/login", "/logout")
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/admin/**", "/admin/bots/**"))
            .authenticationProvider(adminAuthenticationProvider)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF для API
            .csrf(csrf -> csrf.disable())
            
            // Настройка CORS
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(java.util.List.of("*"));
                corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.List.of("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }))
            
            // Настройка сессий
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Добавляем наш кастомный фильтр
            .addFilterBefore(telegramAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Настройка провайдера аутентификации
            .authenticationProvider(telegramAuthenticationProvider)
            
            // Настройка авторизации
            .authorizeHttpRequests(authz -> authz
                // Публичные эндпоинты (но фильтр все равно применяется)
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/css/**").permitAll()
                .requestMatchers("/js/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                // Swagger UI и OpenAPI
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                
                // Dev эндпоинты (только в dev профиле)
                .requestMatchers("/dev/**").permitAll()
                
                // Auth эндпоинты (фильтр применяется, но аутентификация не требуется)
                .requestMatchers("/auth/**").permitAll()
                
                // API для авторизованных пользователей (USER или ADMIN)
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")

                // API ботов - доступ только ADMIN
                .requestMatchers("/api/bots/**").hasRole("ADMIN")
                
                // Все остальные запросы разрешены
                .anyRequest().permitAll()
            )
            
            // Настройка обработки ошибок
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                })
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider adminAuthenticationProvider(AdminUserDetailsService adminUserDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
