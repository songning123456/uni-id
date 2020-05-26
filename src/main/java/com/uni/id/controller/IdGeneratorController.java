package com.uni.id.controller;

import com.uni.id.entity.Result;
import com.uni.id.eum.Status;
import com.uni.id.service.IdGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sonin
 * @date 2020/5/26 21:30
 */
@RestController
@RequestMapping(value = "/generate")
@Slf4j
public class IdGeneratorController {

    @Resource(name = "idGenerator")
    IdGeneratorService idGeneratorService;

    @GetMapping(value = "/id")
    public Map<String, Object> idGeneratorCtrl() {
        Map<String, Object> map = new HashMap<>(2);
        try {
            Result result = idGeneratorService.get();
            if (Status.SUCCESS.equals(result.getStatus())) {
                map.put("status", 200);
                map.put("id", result.getId());
            } else {
                map.put("status", 505);
                map.put("id", result.getId());
            }
        } catch (Exception e) {
            log.error("idGenerator fail: {}", e.getMessage());
            map.put("status", 506);
            map.put("id", -1);
        }
        return map;
    }
}
