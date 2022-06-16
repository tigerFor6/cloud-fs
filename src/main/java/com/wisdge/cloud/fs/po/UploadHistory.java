package com.wisdge.cloud.fs.po;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("上传文件历史")
@TableName("SYS_UPLOAD_HISTORY")
public class UploadHistory {
    @TableId(value="ID", type = IdType.ASSIGN_ID)
    @ApiModelProperty("id")
    private String id;

    @TableField("STATUS")
    @ApiModelProperty("上传结果")
    private int status;

    @TableField("MESSAGE")
    @ApiModelProperty("上传人")
    private String exception;

    @TableField("PATH")
    @ApiModelProperty("存储路径")
    private String storagePath;

    @TableField("NAME")
    @ApiModelProperty("原始文件名")
    private String fileName;

    @TableField("SIZE")
    @ApiModelProperty("文件尺寸")
    private double fileSize;

    @TableField("CREATE_BY")
    @ApiModelProperty("上传人")
    private String createBy;

    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("上传时间")
    private Date createTime;

}
