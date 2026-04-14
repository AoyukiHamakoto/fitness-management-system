package com.fitness.management.config;

import com.fitness.management.security.AuthInterceptor;
import com.fitness.management.security.CurrentUserIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AuthWebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/plans/**", "/check-ins/**", "/api/chat/**");
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
