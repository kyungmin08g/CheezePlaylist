package io.github.playlistmanager.security.config;

import io.github.playlistmanager.provider.JwtProvider;
import io.github.playlistmanager.security.filter.JwtAuthenticationFilter;
import io.github.playlistmanager.security.filter.LoginAuthenticationFilter;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final UserServiceImpl userService;

    public SecurityConfig(
            JwtProvider jwtProvider,
            AuthenticationConfiguration authenticationConfiguration,
            UserServiceImpl userService
    ) {
        this.jwtProvider = jwtProvider;
        this.authenticationConfiguration = authenticationConfiguration;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/").authenticated().anyRequest().permitAll();
        }).exceptionHandling(exceptionHandling -> { // 인증 실패 시 예외 처리
            exceptionHandling.authenticationEntryPoint((request, response, authException) -> {
                response.sendRedirect("/logins");
            });
        });

        http.addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userService), LoginAuthenticationFilter.class);
        http.addFilterAt(new LoginAuthenticationFilter(jwtProvider, authenticationManager(authenticationConfiguration), userService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
