package com.wisdge.cloud.fs.configurer;

import com.wisdge.cloud.component.MessagesLocaleResolver;
import com.wisdge.cloud.fs.interceptor.MDCInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
@Slf4j
public class InterceptorWebMvcConfigurer implements WebMvcConfigurer {

    @Autowired
    private WebMvcProperties webMvcProperties;

    /**
     * 自定义LocalResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        MessagesLocaleResolver localeResolver = new MessagesLocaleResolver();
        localeResolver.setDefaultLocale(webMvcProperties.getLocale());
        log.info("Set customization locale as: {}", localeResolver.getDefaultLocale().toString());
        return localeResolver;
    }

    /**
     * 自定义Locale拦截器 其中lang表示切换语言的参数名
     */
    @Bean
    public LocaleChangeInterceptor localeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MDCInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(localeInterceptor()).addPathPatterns("/**");
    }

//    /**
//     * Configure cross origin requests processing.
//     *
//     * @param registry
//     * @since 4.2
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("*")
//                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS","HEAD")
//                .allowCredentials(true)
//                .maxAge(3600)
//                .allowedHeaders("*");
//    }
}
