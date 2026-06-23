package com.luckyun.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.OperationLog;

import java.util.List;

public interface OperationLogService extends IService<OperationLog> {

    /** 记录操作日志 */
    void log(String type, String targetType, Long targetId, String targetName, String detail, String operatorName);

    /** 获取最近操作日志 */
    List<OperationLog> getRecent(int limit);
}
