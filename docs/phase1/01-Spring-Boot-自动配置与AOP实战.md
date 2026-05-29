# Phase 1：Spring Boot 自动配置与 AOP 实战

## 一、Spring Boot 自动配置原理

### 1.1 什么是自动配置？

Spring Boot 的核心优势在于 **"约定优于配置"**。自动配置（Auto-Configuration）就是根据 classpath 中的依赖，自动创建所需的 Bean。

### 1.2 核心注解：@SpringBootApplication

```java
@SpringBootApplication  // 组合注解，等价于：
// @Configuration       → 标记为配置类
// @EnableAutoConfiguration → 开启自动配置（核心）
// @ComponentScan       → 自动扫描当前包及子包
public class TinyBrainApplication { ... }
```

### 1.3 自动配置的实现原理

面试必问：**"Spring Boot 自动配置是怎么实现的？"**

核心流程：

```
@EnableAutoConfiguration
    → 导入 AutoConfigurationImportSelector
        → 加载 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
            → 获取所有 AutoConfiguration 类名
                → 按 @Conditional 条件过滤
                    → 符合条件的配置类加载 Bean
```

**关键文件**：
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  — Spring Boot 3.x 使用的自动配置注册文件（2.x 用 spring.factories）

### 1.4 条件注解家族

Spring Boot 用大量 `@Conditional` 注解控制配置是否生效：

| 注解 | 作用 | 项目中的应用 |
|------|------|------------|
| `@ConditionalOnClass` | classpath 存在某类时才生效 | MyBatis-Plus 自动配置 |
| `@ConditionalOnMissingBean` | 容器中没有某 Bean 时生效 | 我们自定义 JacksonConfig |
| `@ConditionalOnProperty` | 配置项满足条件时生效 | 切换数据源 |
| `@ConditionalOnWebApplication` | Web 环境下生效 | Security 自动配置 |

### 1.5 自动配置在 TinyBrain 中的应用

**场景：Jackson 全局日期格式化**

```java
@Configuration  // 配置类标记
public class JacksonConfig {

    @Bean
    @Primary  // 优先使用此 ObjectMapper
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // ... 配置 LocalDateTime 序列化格式
        return mapper;
    }
}
```

当 Spring 扫描到此配置类，自动注册 ObjectMapper Bean，所有 Controller 返回 JSON 时统一使用此格式。

**面试题：@ConditionalOnMissingBean 的作用？**
> 答：当容器中不存在指定 Bean 时，才创建该 Bean。常用于覆盖自动配置——用户自定义 Bean 优先级高于框架默认配置。

---

## 二、AOP 实战：全局异常处理

### 2.1 AOP 核心概念

| 概念 | 说明 | TinyBrain 应用 |
|------|------|--------------|
| **JoinPoint** | 被拦截的方法 | Controller 所有方法 |
| **Pointcut** | 切点表达式 | `@RestControllerAdvice` |
| **Advice** | 增强逻辑 | 异常捕获 + 统一响应 |
| **Aspect** | 切面 = Pointcut + Advice | `GlobalExceptionHandler` |

### 2.2 @RestControllerAdvice 原理

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` 的组合注解。

```java
@RestControllerAdvice  // 拦截所有 Controller 异常
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }
}
```

执行流程：
1. Controller 方法抛出异常
2. DispatcherServlet 捕获异常
3. 查找 @ExceptionHandler 匹配的方法
4. 执行增强逻辑，返回统一格式的 R

### 2.3 为什么使用 RuntimeException？

```java
public class BusinessException extends RuntimeException { ... }
```

- **不用 checked exception**：避免每层都 throws，代码更简洁
- **事务回滚**：Spring 默认只回滚 RuntimeException，不回滚 Exception
- **统一处理**：全局异常处理器统一捕获，Controller 层无需 try-catch

**面试题：Spring 事务什么时候回滚？**
> 答：默认只回滚 RuntimeException 和 Error。如果希望 Checked Exception 也回滚，需设置 `@Transactional(rollbackFor = Exception.class)`。

---

## 三、Spring Security + JWT 认证

### 3.1 认证流程

```
客户端 → 登录请求(/api/auth/login)
    → SecurityConfig 白名单放行
        → AuthController.login()
            → UserService.login() 校验用户名密码
                → BCryptPasswordEncoder.matches() 比对
                    → 成功：生成 JWT Token 返回
                    → 失败：抛出 BusinessException
```

后续请求：
```
客户端请求 + Authorization: Bearer <token>
    → JwtAuthFilter 拦截
        → 解析 Token → 提取 userId + role
            → 设置 SecurityContext
                → Controller 通过 @RequestAttribute 获取当前用户
```

### 3.2 JWT 结构

```
Header: { "alg": "HS256" }
Payload: {
  "sub": "1",           // 用户ID（subject）
  "role": "ROLE_USER",  // 角色
  "iat": 1712345678,    // 签发时间
  "exp": 1712950478     // 过期时间（7天）
}
Signature: HMAC-SHA256(base64(Header) + "." + base64(Payload), secret)
```

**面试题：JWT 和 Session 的区别？**
> 答：JWT 无状态，服务端不存储会话信息，天然适合分布式/微服务架构。Session 有状态，需要会话同步或 Redis 共享。JWT 的缺点是无法主动失效，需要维护黑名单。

### 3.3 SecurityFilterChain 配置

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)          // 前后端分离，禁用 CSRF
        .sessionManagement(sm ->                         // 无状态
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll() // 登录注册放行
            .anyRequest().authenticated())               // 其他需认证
        .addFilterBefore(jwtAuthFilter,                  // JWT 过滤器
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

## 四、MyBatis-Plus 实践

### 4.1 核心特性

| 特性 | 说明 | 项目中如何使用 |
|------|------|-------------|
| **BaseMapper** | 自动继承 CRUD | `UserMapper extends BaseMapper<User>` |
| **分页插件** | 自动拼接 COUNT + LIMIT | `PaginationInnerInterceptor` |
| **逻辑删除** | 不真正删除数据 | `@TableLogic` + deleted 字段 |
| **自动填充** | insert/update 自动注入值 | `MetaObjectHandler` 填充 createTime |
| **条件构造器** | 链式查询条件 | `LambdaQueryWrapper<User>()` |

### 4.2 分页插件原理

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

执行流程：
1. 拦截器拦截 SQL
2. 识别 Page 参数
3. 自动生成 COUNT SQL（优化：替换复杂查询为简单的 count(*)）
4. 改写原 SQL 添加 LIMIT 分页
5. 执行两条 SQL，封装 Page 结果

### 4.3 字段自动填充

```java
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        // 自动填充 createTime 和 updateTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

## 五、TinyBrain 项目架构总结

```
请求流程
=======
HTTP Request
    → Spring Security (JWT 校验)
        → Controller (参数校验 @Valid)
            → Service (业务逻辑 + 事务 @Transactional)
                → Mapper (MyBatis-Plus CRUD)
                    → MySQL / H2

异常处理流程
===========
Controller 抛出异常
    → GlobalExceptionHandler 捕获
        → 转换为统一响应 R
            → 返回前端 { code, message, data }
```

**本阶段核心技术点总结**：

| 技术 | 深度要求 | 面试常见问题 |
|------|---------|------------|
| Spring Boot 自动配置 | 理解 `spring.factories` + `@Conditional` | "自动配置原理"、"如何自定义 Starter" |
| AOP | 理解 JDK 动态代理 vs CGLIB | "AOP 代理对象调用内部方法失效" |
| JWT | Token 结构 + 无状态认证 | "JWT 和 Session 区别"、"Token 续期方案" |
| Spring Security | 过滤器链 + SecurityContext | "SecurityFilterChain 执行顺序" |
| MyBatis-Plus | 分页插件原理 + Lambda 查询 | "MyBatis 一级二级缓存"、"${} 和 #{} 区别" |
| 事务管理 | 传播行为 + 隔离级别 | "事务失效场景"、"@Transactional 自调用" |
