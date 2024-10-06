package io.github.playlistmanager.security.config;

import io.github.playlistmanager.provider.JwtProvider;
import io.github.playlistmanager.security.filter.JwtAuthenticationFilter;
import io.github.playlistmanager.security.filter.LoginAuthenticationFilter;
import io.github.playlistmanager.security.oauth2.handler.OAuth2FailureHandler;
import io.github.playlistmanager.security.oauth2.handler.OAuth2SuccessHandler;
import io.github.playlistmanager.security.oauth2.user.service.impl.CustomOAuth2UserService;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Value;
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

    private final String host;
    private final JwtProvider jwtProvider;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final UserServiceImpl userService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    public SecurityConfig(
            @Value("${server.host}") String host,
            JwtProvider jwtProvider,
            AuthenticationConfiguration authenticationConfiguration,
            UserServiceImpl userService,
            CustomOAuth2UserService oAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler
    ) {
        this.host = host;
        this.jwtProvider = jwtProvider;
        this.authenticationConfiguration = authenticationConfiguration;
        this.userService = userService;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
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
                response.sendRedirect(host + "/logins");
            });
        });

        // OAuth2.0 로그인
        http.oauth2Login(oAuth2Login -> {
           oAuth2Login.loginPage("/logins");
           oAuth2Login.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(oAuth2UserService));
           oAuth2Login.successHandler(oAuth2SuccessHandler);
           oAuth2Login.failureHandler(oAuth2FailureHandler);
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
