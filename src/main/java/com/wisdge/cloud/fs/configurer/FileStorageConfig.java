package com.wisdge.cloud.fs.configurer;

import lombok.Data;

@Data
public class FileStorageConfig {
    private String name;
    private String type;
    private String endpoint;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
    private boolean downloadFromUrl;
    private int expiredMins;
    private String wanEndpoint;
    private String remoteRoot;
    private boolean security;

    private String host;
    private String protocol;
    private String username;
    private String password;
    private int port;
    private boolean ssh;
    private boolean forcePort;
    private boolean implicit;
    private boolean passive;

    private String region;
}
