package com.luckyun.stock.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.OperationLog;

import java.util.List;

public interface OperationLogService extends IService<OperationLog> {

    /** 记录操作日志 */
    void log(String type, String targetType, Long targetId, String targetName, String detail, String operatorName);

    /** 获取最近操作日志（用于桌面端一次拉取） */
    List<OperationLog> getRecent(int limit);

    /** 分页获取操作日志（用于移动端上拉加载），types 为空则查全部 */
    IPage<OperationLog> getPage(long page, long pageSize, List<String> types);
}
