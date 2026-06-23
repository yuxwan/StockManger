package com.luckyun.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** CREATE_PRODUCT / UPDATE_PRODUCT / STOCK_IN / STOCK_OUT */
    private String type;

    /** PRODUCT / ORDER */
    private String targetType;

    private Long targetId;

    private String targetName;

    /** 操作描述，如"入库 5 件" */
    private String detail;

    /** 操作员昵称 */
    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
