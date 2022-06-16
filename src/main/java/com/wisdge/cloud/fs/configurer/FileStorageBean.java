package com.wisdge.cloud.fs.configurer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file-storages")
public class FileStorageBean {
    private FileStorageConfig[] storages;
}
