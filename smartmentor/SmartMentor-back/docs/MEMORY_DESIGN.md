# SmartMentor 长期记忆机制设计（Hermes 式三层记忆）

> 状态：设计稿，待评审。本文只描述方案，不含实现。
> 目标读者：开发者 + 答辩评委。

## 1. 为什么要做

当前系统已有相当完整的"记忆"，但有两个硬伤：

| 已有能力 | 位置 | 缺口 |
|---|---|---|
| 语义记忆（结构化画像：风格/目标/薄弱模块/错误模式） | `StudentProfile` + `ProfileService` | 只存"结论"，不存"上次具体聊了什么" |
| 情景记忆（对话历史全量持久化） | `ChatMessage` / `ChatSession` | 喂模型时**写死最近 20 条滑动窗口**（`ChatService.java:299`），超窗即失忆 |
| 反思写回（每 3 轮抽取薄弱信号、增量更新画像） | `ChatService.maybeUpdateProfileFromChat` | 只写回画像，不沉淀可检索的记忆条目 |

**两个痛点**：

1. **长对话失忆** —— 超过 10 轮，更早的内容被硬截断丢弃。
2. **跨会话失忆** —— 换一个 session，AI 完全不记得上次聊过什么；`buildStudentContext` 拉的是结构化诊断/错题，不含"上次对话的具体内容"。

**本设计补齐**：跨会话、可语义检索的长期记忆。学生这次问的问题，能召回他**任意历史会话**里语义相关的片段，注入到当前对话。

## 2. 设计原则（务实优先）

- **零新中间件**：不引入 pgvector / Milvus / RediSearch。单个学生的记忆条目量级是几百到上千条，这个规模下"向量存 MySQL JSON 字段 + Java 内存算余弦"本地计算开销极低，完全够用。上向量库是拿大炮打蚊子，且答辩时还要解释多一个运维组件。
- **记忆写入不阻塞 SSE**：写入挂在**已有的异步写回循环**旁（`maybeUpdateProfileFromChat` 的 daemon 线程池），复用其节流逻辑，绝不阻塞对话流。
- **记忆读取不拖慢首包**：召回发生在 `streamChat` 之前的同步路径上（见 §4.2），故**必须**满足：短超时（默认 300ms）、可被总开关关闭、任何异常立即跳过（召回失败 = 当作无相关记忆，照常对话）。embedding 慢/挂时绝不能推迟 SSE 首 token。详见 §11 实现约束。
- **embedding 可替换 + 可降级**：embedding 源做成接口，单点失败时降级为关键词召回，记忆系统不因 embedding 不可用而崩。
- **复用现有零件**：摘要复用 `summarizeAiReply` / `buildRecentConversationText`，画像注入复用 `buildStudentContext` 的拼接位。

## 3. 三层记忆模型（Hermes 对标）

```
┌─────────────────────────────────────────────────────────────┐
│  工作记忆 Working Memory   —— 已有，不动                        │
│  最近 20 条对话滑动窗口（ChatService.buildContextMessages）     │
├─────────────────────────────────────────────────────────────┤
│  情景记忆 Episodic Memory  —— 本次新增（跨会话、可检索）         │
│  把每若干轮对话压成"记忆条目"，向量化后存 student_memory，       │
│  下次对话按语义相似度召回 top-K                                 │
├─────────────────────────────────────────────────────────────┤
│  语义记忆 Semantic Memory  —— 已有，复用                        │
│  StudentProfile：风格/目标/薄弱模块/错误模式/知识状态           │
└─────────────────────────────────────────────────────────────┘
```

## 4. 数据流

### 4.1 写入侧（异步，挂在现有写回循环旁）

```
对话满 3 轮（沿用现有节流：messageCount % 6 == 0）
   │  [复用 maybeUpdateProfileFromChat 的 profileUpdateExecutor]
   ▼
LLM 把最近几轮压成 1~N 条"记忆条目"
   │   每条：{ type: fact|preference|weakness|goal, content: "一句话事实" }
   │   （复用 chatJsonSync + 严格 JSON 提示词，与 extractWeakSignals 同款）
   ▼
对每条 content 调 embedding → float[]
   │   embedding 不可用时：embedding 存 null，仍入库（降级走关键词召回）
   ▼
存入 student_memory 表（去重：同 student 同 content 近似则跳过）
```

### 4.2 读取侧（同步，在 buildContextMessages 里——位于 streamChat 之前，受 §11 超时约束）

```
学生发来新消息 message
   ▼
若 memory 总开关关闭 → 直接跳过，不注入任何记忆段
   ▼
embed(message) → queryVec      [整段召回包在 300ms 超时 + try/catch 里]
   │   超时 / embedding 不可用 / 异常 → 立即返回空，照常对话（不推迟首包）
   ▼
取该生全部 student_memory 行（一次 findByStudentId）
   ▼
按 embedding_model + embedding_dim 过滤：只保留与 queryVec 同模型同维度的行
   │   （不同模型/维度的向量不可比，跳过；这些行只能走关键词兜底）
   ▼
内存逐条算余弦相似度（被过滤掉或 embedding 为 null 的行走关键词匹配兜底）
   ▼
按相似度降序取 top-K（K=3~5），过滤掉相似度 < 阈值的
   ▼
拼成【相关记忆】段，作为一条 system message 注入当前 prompt
```

> 注：当前代码是先 `buildContextMessages` 再 `streamChat`（`ChatService.java:165`），召回就发生在这一步。所以召回的超时与降级不是"锦上添花"而是**首包延迟的硬约束**，见 §11。

时序图：

```
学生        ChatController      ChatService          MemoryService        LlmService(讯飞)
 │  发消息 ──────▶                  │                      │                    │
 │              ─ stream ─────────▶ │                      │                    │
 │                                  │  recall(sid, msg) ──▶ │                    │
 │                                  │                       │  embed(msg) ──────▶│
 │                                  │                       │ ◀──── queryVec ────│
 │                                  │                       │  余弦 top-K(内存)   │
 │                                  │ ◀── 相关记忆文本 ─────  │                    │
 │                                  │  注入 system prompt    │                    │
 │ ◀═ SSE 流式回答 ═════════════════ │  streamChat ─────────────────────────────▶│
 │                                  │                                            │
 │                            [onComplete 后，异步]                              │
 │                                  │  consolidate(sid) ──▶ │                    │
 │                                  │                       │ LLM 压记忆+embed   │
 │                                  │                       │  存 student_memory │
```

## 5. 表结构

```sql
CREATE TABLE student_memory (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT      NOT NULL,
    type          VARCHAR(20) NOT NULL,        -- fact / preference / weakness / goal
    content       VARCHAR(500) NOT NULL,       -- 一句话记忆，如"对反向传播的链式法则反复混淆"
    content_hash  CHAR(64)     NOT NULL,       -- content 的 SHA-256，去重唯一键用
    embedding     JSON         NULL,           -- 向量本体，存为 JSON 字符串；null 表示未向量化（走关键词召回）
    embedding_provider VARCHAR(30) NULL,       -- 如 spark-maas / spark-hmac，标明向量来自哪套接口
    embedding_model    VARCHAR(50) NULL,       -- 如 embedding-v1，模型升级后用于隔离不可比向量
    embedding_dim      INT          NULL,       -- 维度，召回时按同维度过滤
    source_session VARCHAR(100) NULL,          -- 来源会话，便于溯源
    salience      DECIMAL(3,2) DEFAULT 0.50,   -- 显著度，留给二期做衰减/巩固
    created_at    DATETIME     NOT NULL,
    KEY idx_student (student_id),
    UNIQUE KEY uk_student_content (student_id, content_hash)
) COMMENT='学生长期记忆条目（情景记忆）';
```

> **为什么要 provider/model/dim 三列**：设计强调 embedding 可替换。一旦模型升级、或从讯飞旧 HMAC 接口切到 MaaS `/embeddings`、或维度变化，旧向量与新 queryVec **不可比**——硬算余弦会得出错误相似度。召回时必须按 `embedding_model` + `embedding_dim` 与当前查询模型一致才参与余弦，否则降级关键词。
>
> **存储方式（贴合现有代码）**：`embedding` 不用 `@Type` 映射 `float[]`，而是沿用项目现有惯例——`String` 字段 + `@Column(columnDefinition = "JSON")`（与 `StudentProfile.resourcePreference` 等一致，见 `StudentProfile.java:49`），由 `ObjectMapper` 在 `MemoryService` 里序列化/反序列化为 `float[]`。`hibernate-types-55` 虽在 `pom.xml:82`，但本表无需用它，避免额外的 `@TypeDef` 心智负担。
>
> 对应 entity `StudentMemory` + `StudentMemoryRepository`：`findByStudentId(Long)`、`existsByStudentIdAndContentHash(Long, String)`。

## 6. 新增/改动清单

| 文件 | 动作 | 说明 |
|---|---|---|
| `entity/StudentMemory.java` | 新增 | 上表对应实体 |
| `repository/StudentMemoryRepository.java` | 新增 | `findByStudentId`、按 student+content 查重 |
| `service/EmbeddingClient.java` | 新增 | 接口：`float[] embed(String text)`，可降级返回 null |
| `service/SparkEmbeddingClient.java` | 新增 | 讯飞向量化实现，按 `spark.embedding.mode` 切 legacy-hmac / maas-openai-compatible（见 §8） |
| `service/MemoryService.java` | 新增 | `recall(studentId, query, k)` + `consolidate(studentId, sessionId)` + 余弦计算 |
| `service/ChatService.java` | 改 2 处 | ① `buildContextMessages` 注入【相关记忆】段；② `maybeUpdateProfileFromChat` 旁挂 `consolidate` |
| `application*.yml` | 改 | 加 `spark.embedding.*` 配置 + `smartmentor.memory.*` 开关 |
| `smartmentor_all.sql` | 改 | 加 `student_memory` 建表 |

**总量：4 个新类 + 1 个接口 + 2 处改动。** 不新增 controller、不新增前端、不碰评分链路。

## 7. 余弦相似度（核心算法，仅此一处需自写）

```
sim(a, b) = dot(a, b) / (||a|| * ||b||)
```

O(n·d)，n=记忆条数（百级），d=向量维度（讯飞约 1024）。
**性能定性**：本地余弦计算开销很低（百级条目 × 千维 = 几十万次浮点乘加，量级微秒级）；但召回的**端到端延迟主要由远程 embedding API 决定**（embed 查询消息 + 网络往返），DB 查询与 JSON 反序列化次之。所以 §11 的超时约束针对的是 embedding，不是余弦。
留一个自检：`MemoryServiceTest` 断言 `sim(v, v)==1`、正交向量 `sim==0`、top-K 顺序正确、**不同维度向量拒绝参与比较**。

## 8. 风险与待验证项（必须落实后再开工）

> **这是整个方案唯一的不确定点，写在最前面提醒。**

1. **讯飞 embedding 接口有两套，实现必须配置驱动、二选一，不能把某一套当成唯一事实**。讯飞官方当前同时存在两条向量化路径：
   - **legacy-hmac**：旧版独立 Embedding API，独立域名 + APPID/APIKey/APISecret 的 HMAC 签名鉴权，**非** OpenAI 兼容格式。
   - **maas-openai-compatible**：MaaS 平台的 HTTP `/embeddings` 服务，OpenAI 兼容、Bearer 鉴权，**可能可复用现有 `AbstractOpenAiChatClient` 的调用骨架**。
   - **行动**：开工前用 curl 实测你账号能用的那套，确认 ① 端点 URL ② 鉴权方式 ③ 请求/响应 JSON 结构 ④ 向量维度，并记下用的是哪条路径（写入 `embedding_provider`）。
   - **设计落点**：`SparkEmbeddingClient` 按 `spark.embedding.mode = legacy-hmac | maas-openai-compatible` 配置切换两种实现；HMAC 模式自带签名逻辑（讯飞有 Java demo），MaaS 模式走 OpenAI 兼容骨架。两种模式都只在 `SparkEmbeddingClient` 内部，`EmbeddingClient` 接口与上层 `MemoryService` 不变——这正是把 embedding 抽成接口的原因。
   - 参考：讯飞旧版 Embedding API 文档、MaaS Embedding & Rerank HTTP 协议文档（以你控制台实际可见的为准）。
2. **降级路径**：embedding 未配置/调用失败/超时时，`embed` 返回 null，写入仍入库（embedding=null + 三列元数据为空），召回退化为关键词匹配（复用 `ChatService` 已有的 `matchScore` n-gram 打分）。记忆系统不因 embedding 挂掉而不可用。
3. **成本**：每 3 轮才 embed 一次写入 + 每条消息 embed 一次查询。按现有节流，单学生一次会话 embedding 调用数与对话轮数同量级，可控。

## 9. 实现约束（开发前必须钉死的非功能项）

这一节把"答辩概念"落成"能稳定上线"的硬约束，缺一不可：

1. **召回超时**：读取侧整段召回（含 embed 查询）包在超时里，默认 `smartmentor.memory.recall-timeout-ms=300`。超时即返回空记忆段，**不得推迟 SSE 首包**。
2. **总开关**：`smartmentor.memory.enabled`（默认 true）一键关闭整个记忆读写；`smartmentor.memory.embedding-enabled` 单独控制是否调远程 embedding（关掉则纯关键词召回）。
3. **失败降级**：召回路径任何异常（embed 抛错、DB 错、反序列化错）一律 catch 后当作"无相关记忆"，照常对话。降级行为写进日志但不抛给用户。
4. **模型元数据隔离**：召回只对 `embedding_model` + `embedding_dim` 与当前查询模型一致的行算余弦；不一致的行不参与向量比较（只能走关键词兜底）。模型升级是常态，不做隔离迟早混算。
5. **写入去重**：唯一键 `(student_id, content_hash)`，`content_hash = SHA-256(content)`。`consolidate` 写入前先 `existsByStudentIdAndContentHash` 跳过重复，避免同一事实反复入库。
6. **测试覆盖**：`MemoryServiceTest` 至少覆盖——`sim(v,v)==1`、正交 `sim==0`、top-K 排序正确、**不同维度向量拒绝比较**、embedding=null 行走关键词兜底、超时路径返回空段。无框架、无 fixture，断言式自检即可。

## 10. 答辩话术（卖点对齐）

- "三层记忆架构：工作记忆（对话窗口）+ 情景记忆（跨会话语义召回）+ 语义记忆（学生画像）。"
- "长期记忆用**讯飞星火向量化**做语义嵌入，余弦相似度召回 top-K，跨会话也能记住学生的薄弱点和偏好。"——与赛题"AI 能力使用科大讯飞"二次对齐。
- 架构图（§3、§4 时序图）可直接进 PPT。

## 11. 不做什么（YAGNI）

- 不做向量库 / 不做 ANN 索引（百级数据线性扫描足够）。
- 不做记忆巩固/衰减的复杂逻辑（`salience` 字段先占位，二期需要再做合并）。
- 不做记忆的前端展示页（除非答辩明确要"记忆可视化"，那时再加一个只读接口）。
