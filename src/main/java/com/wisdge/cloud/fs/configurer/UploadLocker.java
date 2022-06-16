package com.wisdge.cloud.fs.configurer;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("文件上传锁")
public class UploadLocker {
    private int minutes;
    private int maximal;
}
