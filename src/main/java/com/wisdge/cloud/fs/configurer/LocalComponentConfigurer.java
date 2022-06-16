package com.wisdge.cloud.fs.configurer;

import com.wisdge.commons.filestorage.*;
import com.wisdge.commons.sms.AliSMSService;
import com.wisdge.commons.sms.ISmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Configuration
public class LocalComponentConfigurer {

    @Resource
    private DataSource dataSource;

    @Bean
    @ConfigurationProperties(prefix = "aliyun-sms")
    public ISmsService smsService() {
        return new AliSMSService();
    }

    @Bean
    public String dataSourceCatalog() throws SQLException {
        Connection connection = dataSource.getConnection();
        String catalog = connection.getCatalog();
        connection.close();
        return catalog;
    }

    @Bean
    @ConfigurationProperties(prefix = "file-service-configurer")
    public FileServiceConfigurer fileServiceConfigurer() {
        return new FileServiceConfigurer();
    }

    @Bean
    public FileStorage fileStorage() {
        return fileServiceConfigurer().getFileStorage();
    }
}
