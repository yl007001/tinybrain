# Phase 4：微服务化与 Spring Cloud Alibaba

## 一、为什么要微服务化？

### 1.1 单体架构 vs 微服务

| 维度 | 单体（Phase 1-3） | 微服务（Phase 4） |
|------|-----------------|-----------------|
| 模块间调用 | 方法调用（JVM内） | RPC（跨进程网络） |
| 独立部署 | ❌ 全部一起部署 | ✅ 每个服务独立部署 |
| 独立扩展 | ❌ 只能整体扩 | ✅ 按需扩缩容 |
| 技术栈 | 单一 | 可异构 |
| 运维复杂度 | 低 | 高 |
| 适合场景 | 小团队、早期项目 | 大团队、高并发 |

### 1.2 拆分策略

```
拆分前（Phase 3）：
    tinybrain-app（单体）
    ├── common
    ├── user
    ├── knowledge
    ├── rag
    └── agent

拆分后（Phase 4）：
    tinybrain-gateway（Spring Cloud Gateway）
        → 路由转发 + 统一鉴权
    tinybrain-user（独立服务）
        → 认证 + 用户管理
    tinybrain-knowledge（独立服务）
        → 文档CRUD + ES搜索
    tinybrain-rag（独立服务）
        → 向量库 + LLM调用
    tinybrain-agent（独立服务）
        → Agent引擎 + Function Calling
    tinybrain-common（公共库）
        → 所有服务引用（非独立运行）
```

---

## 二、Spring Cloud Gateway

### 2.1 网关职责

```
客户端 → Gateway → 微服务
              │
              ├─ JWT鉴权（GlobalFilter）
              ├─ 路由转发（Route配置）
              ├─ 限流（RequestRateLimiter）
              ├─ 跨域（CORS）
              └─ 日志/监控
```

### 2.2 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://tinybrain-user          # lb:// = 负载均衡
          predicates:
            - Path=/api/auth/**              # 匹配路径
          filters:
            - StripPrefix=0                  # 不截断路径
```

**Gateway 和 Zuul 的区别？**
> Gateway 基于 WebFlux（响应式），性能更高；Zuul 1.x 基于 Servlet，已进入维护期。

---

## 三、Nacos 服务发现

### 3.1 架构

```
Nacos Server（注册中心）
    │
    ├── tinybrain-user    → 注册地址: 192.168.1.2:8081
    ├── tinybrain-gateway → 注册地址: 192.168.1.2:8088
    └── ...
        │
    服务消费者（Gateway）
        → 从 Nacos 获取服务地址列表
            → 负载均衡转发
```

### 3.2 配置

```java
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现
public class GatewayApplication { ... }
```

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

---

## 四、Spring Cloud Alibaba 核心组件

### 4.1 组件选型

| 能力 | 阿里方案 | Spring 原生方案 | TinyBrain 选择 |
|------|---------|---------------|--------------|
| 注册中心 | **Nacos** | Eureka（已停更） | Nacos |
| 配置中心 | **Nacos Config** | Spring Cloud Config | Nacos Config |
| 服务调用 | **Dubbo** / OpenFeign | OpenFeign | OpenFeign |
| 熔断降级 | **Sentinel** | Hystrix（已停更） | Sentinel |
| 分布式事务 | **Seata** | - | Seata (TCC) |
| 网关 | - | Gateway | Gateway |

### 4.2 OpenFeign 服务调用

```java
// 用户服务远程调用接口
@FeignClient("tinybrain-user")
public interface UserServiceClient {
    @GetMapping("/api/auth/me")
    R<UserVO> getCurrentUser(@RequestHeader("X-User-Id") Long userId);
}
```

---

## 五、分布式事务（Seata）

### 5.1 分布式事务场景

```
创建文档时：
    MySQL: 写入 kb_document 表          ✅
    ES:    写入 Elasticsearch 索引       ❌ 如果失败
    向量库: 写入 VectorStore              ❌ 如果失败
                  怎么办？
```

### 5.2 TCC 模式

| 阶段 | 操作 | 说明 |
|------|------|------|
| **Try** | 预留资源 | 插入状态为"待确认"的文档记录 |
| **Confirm** | 确认执行 | 更新状态为"已发布" |
| **Cancel** | 回滚 | 删除预留记录 |

### 5.3 Seata 配置

```java
@LocalTCC
public interface DocumentTccService {
    @TwoPhaseBusinessAction(
        name = "createDocument",
        commitMethod = "confirm",
        rollbackMethod = "cancel"
    )
    DocumentVO tryCreate(DocumentCreateRequest request, Long userId);

    boolean confirm(DocumentCreateRequest request, Long userId);
    boolean cancel(DocumentCreateRequest request, Long userId);
}
```

**TCC vs 2PC：分布式事务方案对比**

在 TinyBrain 的 RAG 模块中，文档索引涉及"保存分块 → 向量化 → 存入向量库"三个步骤，如果使用 Seata TCC 模式，可以保证 MySQL 中的分块数据和向量库中的向量数据最终一致。

| 方案 | 一致性 | 性能 | 业务侵入 | 适用场景 |
|------|--------|------|---------|---------|
| 2PC (XA) | 强一致 | 低 | 低 | 短事务、跨库操作 |
| TCC | 最终一致 | 高 | 高 | 长事务、跨服务操作 |
| 本地消息表 | 最终一致 | 高 | 中 | 异步解耦 |
| Saga | 最终一致 | 高 | 中 | 长事务、可补偿 |

