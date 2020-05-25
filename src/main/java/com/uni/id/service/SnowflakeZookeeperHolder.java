package com.uni.id.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author songning
 * @date 2020/5/25
 * description
 */
@Slf4j
@Data
public class SnowflakeZookeeperHolder {

    private String snowflakeName;
    private String host;
    private Integer port;
    private String ip;

    /**
     * //保存自身的key ip:port-000000001
     */
    private String zkAddressNode = null;

    /**
     * 保存自身的key ip:port
     */
    private String listenAddress;
    private int workerId;
    private long lastUpdateTime;

    public SnowflakeZookeeperHolder(String snowflakeName, String host, int port, String ip) {
        this.snowflakeName = snowflakeName;
        this.host = host;
        this.port = port;
        this.listenAddress = ip + ":" + port;
    }

    public boolean init() {
        try {
            CuratorFramework curator = createWithOptions(this.host, new RetryUntilElapsed(1000, 4));
            curator.start();
            Stat stat = curator.checkExists().forPath(getForeverPath());
            if (stat == null) {
                // 不存在根节点,机器第一次启动,创建/snowflake/ip:port-000000000,并上传数据
                zkAddressNode = createNode(curator);
                // worker id 默认是0
                updateLocalWorkerId(workerId);
                // 定时上报本机时间给forever节点
                scheduledUploadData(curator, zkAddressNode);
                return true;
            } else {
                // ip:port->00001
                Map<String, Integer> nodeMap = new HashMap<>(2);
                // ip:port->(ipPort-000001)
                Map<String, String> realNode = new HashMap<>(2);
                // 存在根节点,先检查是否有属于自己的根节点
                List<String> keys = curator.getChildren().forPath(getForeverPath());
                for (String key : keys) {
                    String[] nodeKey = key.split("-");
                    realNode.put(nodeKey[0], key);
                    nodeMap.put(nodeKey[0], Integer.parseInt(nodeKey[1]));
                }
                Integer workerId = nodeMap.get(listenAddress);
                if (workerId != null) {
                    // 有自己的节点,Zk_addressNode=ip:port
                    zkAddressNode = getForeverPath() + "/" + realNode.get(listenAddress);
                    //启动worker时使用会使用
                    this.workerId = workerId;
                    if (!checkInitTimeStamp(curator, zkAddressNode)) {
                        throw new RuntimeException("init timestamp check error,forever node timestamp gt this node time");
                    }
                    //准备创建临时节点
                    doService(curator);
                    updateLocalWorkerId(workerId);
                    log.info("[Old NODE]find forever node have this endpoint ip-{} port-{} workId-{} childNode and start SUCCESS", host, port, workerId);
                } else {
                    //表示新启动的节点,创建持久节点 ,不用check时间
                    String newNode = createNode(curator);
                    zkAddressNode = newNode;
                    String[] nodeKey = newNode.split("-");
                    workerId = Integer.parseInt(nodeKey[1]);
                    doService(curator);
                    updateLocalWorkerId(workerId);
                    log.info("[New NODE]can not find node on forever node that endpoint ip-{} port-{} workId-{},create own node on forever node and start SUCCESS ", host, port, workerId);
                }
            }
        } catch (Exception e) {
            log.error("Start node ERROR {}", e.getMessage());
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(new File(getPortPath())));
                workerId = Integer.parseInt(properties.getProperty("workerId"));
                log.warn("START FAILED ,use local node file properties workerId-{}", workerId);
            } catch (Exception e1) {
                log.error("Read file error ", e1);
                return false;
            }
        }
        return true;
    }

    private void doService(CuratorFramework curator) {
        // /snowflake_forever/ip:port-000000001
        scheduledUploadData(curator, zkAddressNode);
    }

    private void scheduledUploadData(final CuratorFramework curator, final String zkAddressNode) {
        // 每3s上报数据
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "schedule-upload-time");
            thread.setDaemon(true);
            return thread;
        }).scheduleWithFixedDelay(() -> updateNewData(curator, zkAddressNode), 1L, 3L, TimeUnit.SECONDS);
    }

    private boolean checkInitTimeStamp(CuratorFramework curator, String zkAddressNode) throws Exception {
        byte[] bytes = curator.getData().forPath(zkAddressNode);
        Endpoint endPoint = deBuildData(new String(bytes));
        // 该节点的时间不能小于最后一次上报的时间
        return endPoint.getTimestamp() <= System.currentTimeMillis();
    }

    /**
     * 创建持久顺序节点 ,并把节点数据放入 value
     *
     * @param curator
     * @return
     * @throws Exception
     */
    private String createNode(CuratorFramework curator) throws Exception {
        try {
            return curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(getForeverPath() + "/" + listenAddress + "-", buildData().getBytes());
        } catch (Exception e) {
            log.error("create node error msg {} ", e.getMessage());
            throw e;
        }
    }

    private void updateNewData(CuratorFramework curator, String path) {
        try {
            if (System.currentTimeMillis() < lastUpdateTime) {
                return;
            }
            curator.setData().forPath(path, buildData().getBytes());
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.info("update init data error path is {} error is {}", path, e);
        }
    }

    /**
     * 构建需要上传的数据
     *
     * @return
     */
    private String buildData() throws JsonProcessingException {
        Endpoint endpoint = new Endpoint(ip, port, System.currentTimeMillis());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(endpoint);
    }

    private Endpoint deBuildData(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Endpoint.class);
    }

    /**
     * 在节点文件系统上缓存一个workId值,zk失效,机器重启时保证能够正常启动
     *
     * @param workerId
     */
    private void updateLocalWorkerId(int workerId) {
        File leafConfFile = new File(getPortPath());
        boolean exists = leafConfFile.exists();
        log.info("file exists status is {}", exists);
        if (exists) {
            try {
                FileUtils.writeStringToFile(leafConfFile, "workerId=" + workerId, false);
                log.info("update file cache workerId is {}", workerId);
            } catch (IOException e) {
                log.error("update file cache error ", e);
            }
        } else {
            //不存在文件,父目录页肯定不存在
            try {
                boolean mkdirs = leafConfFile.getParentFile().mkdirs();
                log.info("init local file cache create parent dis status is {}, worker id is {}", mkdirs, workerId);
                if (mkdirs) {
                    if (leafConfFile.createNewFile()) {
                        FileUtils.writeStringToFile(leafConfFile, "workerId=" + workerId, false);
                        log.info("local file cache workerId is {}", workerId);
                    }
                } else {
                    log.warn("create parent dir error===");
                }
            } catch (IOException e) {
                log.warn("create workerId conf file error", e);
            }
        }
    }

    private CuratorFramework createWithOptions(String connectionString, RetryUntilElapsed retryPolicy) {
        return CuratorFrameworkFactory.builder().connectString(connectionString)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(10000)
                .sessionTimeoutMs(6000)
                .build();
    }

    /**
     * 上报数据结构
     */
    @Data
    static class Endpoint {
        private String ip;
        private int port;
        private long timestamp;

        Endpoint(String ip, int port, long timestamp) {
            this.ip = ip;
            this.port = port;
            this.timestamp = timestamp;
        }
    }

    /**
     * 保存所有数据持久的节点
     *
     * @return
     */
    private String getForeverPath() {
        return "/snowflake/" + snowflakeName + "/forever";
    }

    private String getPortPath() {
        return System.getProperty("java.io.tmpdir") + File.separator + snowflakeName + "/conf/" + port + "/workerId.properties";
    }
}
