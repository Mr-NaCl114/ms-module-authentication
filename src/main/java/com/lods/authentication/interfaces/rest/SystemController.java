package com.lods.authentication.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lods.authentication.domain.entity.AuthRole;
import com.lods.authentication.infrastructure.persistence.mapper.AuthRoleMapper;
import com.lods.types.response.DataResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 基础设施连通性探针。仅用于开发期验证 MySQL / Redis / Kafka 是否可用，
 * 不属于业务接口，接入正式鉴权后应限制为管理员可访问。
 */
@RestController
@RequestMapping("/api/auth")
public class SystemController {

    private final AuthRoleMapper roleMapper;
    private final StringRedisTemplate redisTemplate;
    private final String kafkaBootstrapServers;

    public SystemController(AuthRoleMapper roleMapper,
                            StringRedisTemplate redisTemplate,
                            @Value("${spring.kafka.bootstrap-servers:127.0.0.1:29092}") String kafkaBootstrapServers) {
        this.roleMapper = roleMapper;
        this.redisTemplate = redisTemplate;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    @GetMapping("/ping")
    public DataResponse<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "ms-module-authentication");
        body.put("time", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return DataResponse.success(body);
    }

    /** 走 MyBatis-Plus 查询角色表，验证 ORM 与数据源 */
    @GetMapping("/roles")
    public DataResponse<List<AuthRole>> roles() {
        List<AuthRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<AuthRole>().orderByAsc(AuthRole::getRoleLevel));
        return DataResponse.success(roles);
    }

    /** 一次性探测三类中间件，返回各自状态与耗时 */
    @GetMapping("/infra")
    public DataResponse<Map<String, Object>> infra() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mysql", probe(this::probeMysql));
        result.put("redis", probe(this::probeRedis));
        result.put("kafka", probe(this::probeKafka));
        return DataResponse.success(result);
    }

    private Map<String, Object> probe(Probe probe) {
        long start = System.nanoTime();
        try {
            String detail = probe.run();
            return Map.of(
                    "status", "UP",
                    "detail", detail,
                    "costMs", (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            return Map.of(
                    "status", "DOWN",
                    "detail", e.getClass().getSimpleName() + ": " + e.getMessage(),
                    "costMs", (System.nanoTime() - start) / 1_000_000);
        }
    }

    private String probeMysql() {
        Long count = roleMapper.selectCount(null);
        return "ms_auth_role rows=" + count;
    }

    private String probeRedis() {
        String key = "ms:probe:authentication";
        String value = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(30));
        String read = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return value.equals(read) ? "set/get ok" : "set/get mismatch";
    }

    private String probeKafka() throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
        try (AdminClient client = AdminClient.create(props)) {
            var cluster = client.describeCluster();
            int nodes = cluster.nodes().get(5, TimeUnit.SECONDS).size();
            String clusterId = cluster.clusterId().get(5, TimeUnit.SECONDS);
            return "brokers=" + nodes + ", clusterId=" + clusterId;
        }
    }

    @FunctionalInterface
    private interface Probe {
        String run() throws Exception;
    }
}
