package com.wisdge.cloud.fs;

import com.wisdge.commons.filestorage.FileStorage;
import com.wisdge.commons.filestorage.IFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = { "com.wisdge.cloud"} )
@MapperScan({"com.wisdge.cloud"})
@EnableSwagger2
public class Application implements CommandLineRunner, DisposableBean {
    private final static CountDownLatch latch = new CountDownLatch(1);

    @Value("${cloud.app-name:cloud.fs}")
    private String appName;

    @Resource
    private FileStorage fileStorage;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        log.info(fileStorage.getFileStorage("default").toString());
        log.info("{} ------>> 启动成功", appName);
    }

    @Override
    public void destroy() {
        latch.countDown();

        Map<String, IFileStorageClient> fileStorageClientMap = fileStorage.getFileStorages();
        Iterator<String> iter = fileStorageClientMap.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            IFileStorageClient client = fileStorageClientMap.get(key);
            client.destroy();
            log.info("Destroy {} completely", client.getClass().getName());
        }
        log.info("{} ------>> 关闭成功", appName);
    }

}
