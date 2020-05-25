package com.uni.id.config;

import com.uni.id.service.IdGeneratorService;
import com.uni.id.service.impl.SnowflakeIdGeneratorServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author songning
 * @date 2020/5/25
 * description
 */
@Configuration
public class BeanConfig {

    @Value("${snowflake.name}")
    private String snowflakeName;

    @Value("${snowflake.zookeeper.host}")
    private String host;

    @Value("${snowflake.zookeeper.port}")
    private Integer port;

    @Bean(name = "idGenerator")
    public IdGeneratorService idGenerator() {
        return new SnowflakeIdGeneratorServiceImpl(snowflakeName, host, port);
    }
}
