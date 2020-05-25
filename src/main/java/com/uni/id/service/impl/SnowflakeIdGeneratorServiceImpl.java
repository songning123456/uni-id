package com.uni.id.service.impl;

import com.google.common.base.Preconditions;
import com.uni.id.util.Utils;
import com.uni.id.entity.Result;
import com.uni.id.eum.Status;
import com.uni.id.service.IdGeneratorService;
import com.uni.id.service.SnowflakeZookeeperHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @author songning
 * @date 2020/5/25
 * description
 */
@Slf4j
@Service
public class SnowflakeIdGeneratorServiceImpl implements IdGeneratorService {

    private long initialTimestamp;
    private final long workerIdBits = 10L;
    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private static final Random RANDOM = new Random();

    public SnowflakeIdGeneratorServiceImpl() {
    }

    public SnowflakeIdGeneratorServiceImpl(String snowflakeName, String host, int port) {
        // Thu Nov 04 2010 09:42:54 GMT+0800 (中国标准时间)
        this(snowflakeName, host, port, 1288834974657L);
    }

    /**
     * @param host             zk地址
     * @param port             snowflake监听端口
     * @param initialTimestamp 起始的时间戳
     */
    private SnowflakeIdGeneratorServiceImpl(String snowflakeName, String host, int port, long initialTimestamp) {
        this.initialTimestamp = initialTimestamp;
        if (currentTime() <= initialTimestamp) {
            throw new IllegalArgumentException("Snowflake not support initialTimestamp gt currentTime");
        }
        final String ip = Utils.getIp();
        SnowflakeZookeeperHolder snowflakeZookeeperHolder = new SnowflakeZookeeperHolder(snowflakeName, host, port, ip);
        log.info("initialTimestamp:{} ,ip:{} ,zkAddress:{} port:{}", initialTimestamp, ip, host, port);
        boolean initFlag = snowflakeZookeeperHolder.init();
        if (initFlag) {
            workerId = snowflakeZookeeperHolder.getWorkerId();
            log.info("START SUCCESS USE ZK WORKER_ID-{}", workerId);
        } else {
            throw new IllegalArgumentException("Snowflake Id Gen is not init ok");
        }
        /**
         * 最大能够分配的workerId = 1023
         */
        long maxWorkerId = ~(-1L << workerIdBits);
        Preconditions.checkArgument(workerId >= 0 && workerId <= maxWorkerId, "workerID must gte 0 and lte 1023");
    }

    @Override
    public synchronized Result get() {
        long timestamp = currentTime();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = currentTime();
                    if (timestamp < lastTimestamp) {
                        return new Result(-1, Status.EXCEPTION);
                    }
                } catch (InterruptedException e) {
                    log.error("wait interrupted");
                    return new Result(-2, Status.EXCEPTION);
                }
            } else {
                return new Result(-3, Status.EXCEPTION);
            }
        }
        long sequenceBits = 12L;
        if (lastTimestamp == timestamp) {
            long sequenceMask = ~(-1L << sequenceBits);
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // sequence为0的时候表示是下一毫秒时间开始对seq做随机
                sequence = RANDOM.nextInt(100);
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 如果是新的ms开始
            sequence = RANDOM.nextInt(100);
        }
        lastTimestamp = timestamp;
        long timestampLeftShift = sequenceBits + workerIdBits;
        long id = ((timestamp - initialTimestamp) << timestampLeftShift) | (workerId << sequenceBits) | sequence;
        return new Result(id, Status.SUCCESS);

    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}
