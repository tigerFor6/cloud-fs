package com.wisdge.cloud.fs.configurer;

import com.wisdge.commons.filestorage.*;
import com.wisdge.commons.sms.AliSMSService;
import com.wisdge.commons.sms.ISmsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class LocalComponentConfigurer {

    @Resource
    private DataSource dataSource;

    @Resource
    private FileStorageBean fileStorageBean;

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
    public FileStorage fileStorage() {
        FileStorage fileStorage = new FileStorage();
        if (fileStorageBean == null)
            return fileStorage;

        for(FileStorageConfig config : fileStorageBean.getStorages()) {
            String name = config.getName();
            String type = config.getType();
            if (type.equalsIgnoreCase("AliOSS")) {
                AliOSSStorageClient fileStorageClient = new AliOSSStorageClient();
                fileStorageClient.setEndpoint(config.getEndpoint());
                fileStorageClient.setBucketName(config.getBucketName());
                fileStorageClient.setAccessKeyId(config.getAccessKeyId());
                fileStorageClient.setAccessKeySecret(config.getAccessKeySecret());
                fileStorageClient.setDownloadFromURL(config.isDownloadFromUrl());
                fileStorageClient.setExpiredMinutes(config.getExpiredMins());
                fileStorageClient.setRemoteRoot(config.getRemoteRoot());
                fileStorageClient.setWanEndpoint(config.getWanEndpoint());
                fileStorageClient.setSecurity(config.isSecurity());
                fileStorageClient.init();
                fileStorage.addFileStorage(name, fileStorageClient);
            } else if (type.equalsIgnoreCase("ftp")) {
                FtpStorageClient fileStorageClient = new FtpStorageClient();
                fileStorageClient.setProtocal(config.getProtocol());
                fileStorageClient.setHostname(config.getHost());
                fileStorageClient.setUsername(config.getUsername());
                fileStorageClient.setPassword(config.getPassword());
                fileStorageClient.setPassive(config.isPassive());
                fileStorageClient.setRemoteRoot(config.getRemoteRoot());
                fileStorageClient.setForcePortP(config.isForcePort());
                fileStorageClient.setImplicit(config.isImplicit());
                fileStorageClient.init();
                fileStorage.addFileStorage(name, fileStorageClient);
            }else if (type.equalsIgnoreCase("Minio")) {
                MinioStorageClient minioStorageClient = new MinioStorageClient();
                minioStorageClient.setEndpoint(config.getEndpoint());
                minioStorageClient.setAccessKey(config.getAccessKeyId());
                minioStorageClient.setAccessSecret(config.getAccessKeySecret());
                minioStorageClient.setBucketName(config.getBucketName());
                minioStorageClient.setRegion(config.getRegion());
                minioStorageClient.init();
                fileStorage.addFileStorage(name, minioStorageClient);
            }
        }
        return fileStorage;
    }
}
