package com.luckyun.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.OperationLog;
import com.luckyun.stock.mapper.OperationLogMapper;
import com.luckyun.stock.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public void log(String type, String targetType, Long targetId, String targetName, String detail, String operatorName) {
        OperationLog log = new OperationLog();
        log.setType(type);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        log.setOperatorName(operatorName);
        save(log);
    }

    @Override
    public List<OperationLog> getRecent(int limit) {
        return lambdaQuery()
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT " + limit)
                .list();
    }
}
