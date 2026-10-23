package com.example.integrations.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthService authService;
    private final ObjectMapper mapper;

    public WebConfig(AuthService authService, ObjectMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(authService, mapper))
                .addPathPatterns("/api/**");
    }
}
