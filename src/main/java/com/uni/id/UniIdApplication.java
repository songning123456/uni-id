package com.uni.id;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author songning
 */
@SpringBootApplication
@EnableDubbo
public class UniIdApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniIdApplication.class, args);
    }

}
