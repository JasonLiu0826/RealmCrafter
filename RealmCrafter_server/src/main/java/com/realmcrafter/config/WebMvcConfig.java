package com.realmcrafter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传文件对外提供访问：/api/v1/files/** 映射到本地 ./uploads/ 目录；兼容旧路径 /api/v1/upload/files/**。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${realmcrafter.upload.base-dir:./uploads}")
    private String baseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(baseDir).toAbsolutePath().normalize();
        String location = "file:" + path.toString() + "/";
        registry.addResourceHandler("/api/v1/files/**")
                .addResourceLocations(location);
        registry.addResourceHandler("/api/v1/upload/files/**")
                .addResourceLocations(location);
    }
}
