package com.uni.id;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author songning
 */
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class UniIdApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniIdApplication.class, args);
    }

}
