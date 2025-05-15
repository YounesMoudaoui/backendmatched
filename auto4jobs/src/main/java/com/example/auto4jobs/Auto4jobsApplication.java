package com.example.auto4jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@SpringBootApplication
public class Auto4jobsApplication {
	public static void main(String[] args) {
		SpringApplication.run(Auto4jobsApplication.class, args);
	}

	@Configuration
	public static class StaticResourceConfiguration implements WebMvcConfigurer {
		@Override
		public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/uploads/**")
					.addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
		}
	}
}