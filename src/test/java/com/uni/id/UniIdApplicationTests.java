package com.uni.id;

import com.uni.id.entity.Result;
import com.uni.id.service.IdGeneratorService;
import com.uni.id.service.impl.SnowflakeIdGeneratorServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UniIdApplicationTests {

    @Resource(name = "idGenerator")
    IdGeneratorService idGeneratorService;

    @Test
    public void contextLoads() {
        Result result = idGeneratorService.get();
        System.out.println(result);
    }

}
