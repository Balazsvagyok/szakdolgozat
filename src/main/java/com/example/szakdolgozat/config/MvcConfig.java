package com.example.szakdolgozat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/demo/registration").setViewName("registration");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/").setViewName("index");
    }

}
