package com.btsheng.erp.business.platform.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_download_log")
public class SysDownloadLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("file_id")
    private Long fileId;
    @TableField("user_id")
    private Long userId;
    private String ip;
    private LocalDateTime ts;
    private String action;
}
