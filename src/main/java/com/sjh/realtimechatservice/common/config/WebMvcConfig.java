package com.sjh.realtimechatservice.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * HTML 파일을 브라우저에서 직접 열어 테스트할 때 CORS 오류 방지.
     * file:// 또는 다른 포트에서 오는 요청을 허용.
     * 운영 환경에서는 allowedOrigins를 실제 도메인으로 제한해야 함.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}