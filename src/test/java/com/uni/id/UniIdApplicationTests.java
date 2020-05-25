package com.uni.id;

import com.uni.id.entity.Result;
import com.uni.id.service.IdGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UniIdApplicationTests {

    @Resource(name = "idGenerator")
    IdGeneratorService idGeneratorService;

    @Test
    public void contextLoads() {
        for (int i = 0; i < 100; i++) {
            Result result = idGeneratorService.get();
            log.info("当前i: {}, 结果: {}", i, result);
        }
    }

}
