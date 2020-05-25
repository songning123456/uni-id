package com.uni.id.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.uni.dubbo.service.GenerateIdService;
import com.uni.id.entity.Result;
import com.uni.id.eum.Status;
import com.uni.id.service.IdGeneratorService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author songning
 * @date 2020/5/25
 * description
 */
@Component
@Service
public class GenerateIdServiceImpl implements GenerateIdService {

    @Resource(name = "idGenerator")
    IdGeneratorService idGeneratorService;

    @Override
    public long getSnowflakeId() {
        Result result = idGeneratorService.get();
        if (Status.SUCCESS.equals(result.getStatus())) {
            return result.getId();
        } else {
            return -1;
        }
    }
}
