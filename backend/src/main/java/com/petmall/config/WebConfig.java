package com.petmall.config;

import com.petmall.common.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AuthInterceptor authInterceptor;
    
    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.storage.type:sftp}")
    private String storageType;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/user/register", "/user/public/**",
                        "/product/public/**", "/category/public/**", "/file/upload", "/upload/**",
                        "/review/product/**",
                        // 异常转发到 /error 时不该再走登录校验，否则真实错误会被盖成 401
                        "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 远端模式图片由文件服务器的 nginx 直出，后端不再重复映射，避免同一 URL 两处真相
        if (!"local".equalsIgnoreCase(storageType)) {
            return;
        }
        // 相对路径先转绝对路径再转 file: URI，避免按进程工作目录解析不一致
        String resourceLocation = Paths.get(uploadPath).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(resourceLocation);
    }
}
