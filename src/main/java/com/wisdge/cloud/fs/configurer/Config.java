package com.wisdge.cloud.fs.configurer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "config")
public class Config {
    private String appName;
    private String skin;
    private String version;
    private String digestType;
    private String outEncode = "UTF-8";
    private List<String> uploadAllow;
    private List<String> uploadForbidden;
    private int defaultPageSize = 50;
    private UploadLocker uploadLocker;

    /**
     * SWAGGER参数
     */
    private final Swagger swagger = new Swagger();

    /**
     * SWAGGER接口文档参数
     */
    @Data
    public static class Swagger {
        private String title;
        private String description;
        private String version;
        private String termsOfServiceUrl;
        private String contactName;
        private String contactUrl;
        private String contactEmail;
        private String license;
        private String licenseUrl;
    }
}
