# Phase 1：MySQL 索引优化与事务原理

## 一、MySQL 索引原理

### 1.1 B+ Tree 索引结构

**面试必问：InnoDB 为什么用 B+ Tree 而不用 B-Tree 或红黑树？**

B+ Tree 的核心优势：

| 特性 | B+ Tree | B-Tree | 红黑树 |
|------|---------|--------|--------|
| 非叶子节点存储 | 只存键（不存数据） | 键+数据 | 键+数据 |
| 叶子节点 | 双向链表连接 | 无连接 | 无 |
| 单次 IO 可读键数 | 多（16KB/页） | 少 | 极少 |
| 范围查询 | ✅ 叶子链表遍历 | ❌ 中序遍历 | ❌ 中序遍历 |
| 磁盘 IO 次数 | **3-4 次**（树高） | 3-4 次 | log₂N 次 |

**结论**：B+ Tree 非叶子节点只存键，每页能存更多键 → 树高更低 → IO 次数更少。叶子节点双向链表使范围查询极快。

### 1.2 TinyBrain 中的索引设计

```sql
-- 用户表：用户名唯一索引
CREATE UNIQUE INDEX uk_username ON sys_user(username);

-- 文档表：复合索引（用户ID + 创建时间）
CREATE INDEX idx_user_create ON kb_document(user_id, create_time);

-- 文档表：状态索引
CREATE INDEX idx_status ON kb_document(status);
```

**索引选择依据**：

| 查询模式 | 索引策略 | 原因 |
|---------|---------|------|
| `WHERE user_id = ? AND status = ?` | 复合索引 `(user_id, status)` | 等值查询，最左前缀匹配 |
| `WHERE user_id = ? ORDER BY create_time DESC` | 复合索引 `(user_id, create_time)` | 索引排序，避免 filesort |
| `WHERE username = ?` | 唯一索引 `uk_username` | 等值查询，唯一性约束 |

### 1.3 最左前缀原则

**面试题：复合索引 (a, b, c) 哪些查询走索引？**

```sql
WHERE a = 1             → ✅ 走索引（匹配第一列）
WHERE a = 1 AND b = 2   → ✅ 走索引（匹配前两列）
WHERE a = 1 AND c = 3   → ✅ 走索引（匹配 a，c 索引下推）
WHERE b = 2             → ❌ 不走索引（没从 a 开始）
WHERE a = 1 OR b = 2    → ❌ OR 两边都覆盖才走
```

### 1.4 索引失效场景

| 场景 | 错误写法 | 正确写法 |
|------|---------|---------|
| 类型隐式转换 | `WHERE phone = 123456` | `WHERE phone = '123456'` |
| 非最左匹配 | WHERE b = 2（无 a） | 加 a 条件 |
| LIKE 左模糊 | WHERE name LIKE '%张' | 改为 LIKE '张%' |
| 索引列运算 | WHERE age + 1 = 20 | WHERE age = 19 |
| OR 非索引列 | WHERE id=1 OR name='a' | 改为 UNION |

---

## 二、MySQL 事务与锁

### 2.1 事务 ACID 特性

| 特性 | 含义 | InnoDB 实现 |
|------|------|-----------|
| **A**tomicity 原子性 | 全做或全不做 | undo log（回滚日志） |
| **C**onsistency 一致性 | 数据始终满足约束 | 应用层 + 数据库约束 |
| **I**solation 隔离性 | 事务互不干扰 | MVCC + 锁 |
| **D**urability 持久性 | 提交后永久保存 | redo log + binlog |

### 2.2 事务隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 实现方式 |
|---------|:---:|:---------:|:---:|--------|
| READ UNCOMMITTED | ✅ | ✅ | ✅ | 无 |
| READ COMMITTED | ❌ | ✅ | ✅ | MVCC（读已提交版本） |
| **REPEATABLE READ**（MySQL 默认） | ❌ | ❌ | ❌（部分） | MVCC + Gap Lock |
| SERIALIZABLE | ❌ | ❌ | ❌ | 所有操作加锁 |

**TinyBrain 中使用**：
```java
@Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
public DocumentVO create(DocumentCreateRequest request, Long userId) {
    // 创建文档、分块、向量化全部在同一事务中
}
```

### 2.3 MVCC 原理

**面试必问：MVCC 如何实现可重复读？**

核心组件：
1. **隐藏字段**：DB_TRX_ID（最后修改事务ID）、DB_ROLL_PTR（回滚指针）
2. **undo log**：记录数据的历史版本，构成版本链
3. **read view**：事务启动时创建的快照视图

可重复读实现：
```
事务 A（trx_id=100）启动时创建 read view = [100, 200]
                      活跃事务列表 = [100, 101]
                      低水位 = 100，高水位 = 200

当 A 读取数据时：
  - DB_TRX_ID < 100          → ✅ 可见（已提交的旧版本）
  - DB_TRX_ID 在 [100,200)   → ❌ 不可见（用 undo log 找旧版本）
  - DB_TRX_ID >= 200         → ❌ 不可见（未来的事务）
  - DB_TRX_ID 在活跃列表中   → ❌ 不可见（当前未提交的版本）
```

**READ COMMITTED 与 REPEATABLE READ 的区别**：
- RC：每次 SELECT 都创建新的 read view
- RR：事务内只创建一次 read view（第一次 SELECT 时）

### 2.4 行锁与 Gap Lock

```sql
-- 共享锁（S Lock）：允许其他事务读
SELECT * FROM kb_document WHERE id = 1 LOCK IN SHARE MODE;

-- 排他锁（X Lock）：不允许其他事务读写
SELECT * FROM kb_document WHERE id = 1 FOR UPDATE;
```

**间隙锁（Gap Lock）**：RR 级别下，锁住索引记录之间的间隙，防止幻读。

```
例如：索引值有 [1, 5, 10]
WHERE id BETWEEN 3 AND 8 → 间隙锁锁住 (1, 5) 和 (5, 10)
```

---

## 三、项目中用到的连接池：HikariCP

### 3.1 配置

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5          # 最小空闲连接
      maximum-pool-size: 20     # 最大连接数
      idle-timeout: 300000      # 空闲超时(5分钟)
      max-lifetime: 1200000     # 最大存活时间(20分钟)
      connection-timeout: 30000 # 获取连接超时(30秒)
```

**为什么选 HikariCP？**
- 字节码极致优化（使用 Javassist 生成代理类）
- 无锁集合（ConcurrentBag 替代 BlockingQueue）
- 大小最优：约 200KB 的 jar 包

### 3.2 连接池大小计算公式

```
核心数 = CPU 核心数
连接池大小 = 核心数 × 2 + 有效磁盘数（SSD 一般 +1）
```

TinyBrain（6 核 CPU）：`6 × 2 + 1 = 13`，配置 20 留有余量。

---

## 四、数据库设计最佳实践（TinyBrain）

### 4.1 表设计规范

| 规范 | 说明 |
|------|------|
| **主键** | BIGINT 自增（分布式场景建议雪花算法） |
| **逻辑删除** | 每个表都带 `deleted` 字段，不开物理删除 |
| **时间审计** | 每个表都带 `create_time` + `update_time` |
| **字段注释** | 每个字段必须有 COMMENT |
| **索引命名** | `idx_表名_字段名` / `uk_表名_字段名` |
| **长度限制** | varchar 不设过长，根据业务定 |

### 4.2 慢查询优化步骤

1. 开启慢查询日志
2. 用 `EXPLAIN` 分析 SQL（关注 type、rows、Extra）
3. 识别全表扫描 → 加索引
4. 避免 SELECT * → 只查需要的列
5. 分页优化：大偏移量用子查询或游标分页

---

## 五、面试高频题

### 基础题
1. InnoDB 和 MyISAM 的区别？
2. 聚簇索引和非聚簇索引的区别？
3. 覆盖索引是什么？如何实现？
4. 索引下推（ICP）优化是什么？

### 进阶题
1. MVCC 实现原理（详细讲 undo log + read view）
2. Next-Key Lock 是什么？解决了什么问题？
3. 事务隔离级别如何影响数据库并发性能？
4. 大表分页怎么优化？（特别是 LIMIT 100000, 10）

### 场景题
1. **"你的项目 QPS 突然涨了 100 倍，数据库扛不住怎么办？"**
   - 先看慢查询，优化索引
   - 加 Redis 缓存热点数据
   - 读写分离（主从复制）
   - 分库分表（ShardingSphere）
   - 如果还是扛不住 → 上消息队列削峰

2. **"数据库死锁了怎么排查？"**
   - `SHOW ENGINE INNODB STATUS` 查看最近死锁
   - 分析事务 SQL，检查锁竞争
   - 优化索引减少锁范围
   - 调整事务顺序，避免交叉加锁
