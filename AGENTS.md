# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 项目概览

SmartMentor（智导师）——基于大模型多智能体协作的个性化学习系统，参加"中国软件杯"A3 赛道（基于大模型的个性化资源生成与学习多智体系统）。核心理念是"溯源式学习"：不止识别学生哪道题错了，而是沿知识图谱前置依赖链回溯，定位真正的薄弱根因，再自底向上生成学习路径。

**注意**：申报书等文档以"高中数学"为例，但 `src/main/resources/knowledge-graph/` 中实际加载的知识图谱是计算机/软件类主题（`computer-ai-foundation`、`electronic-digital-circuit`、`software-java-web`）。改动涉及学科内容时以代码中的知识图谱为准，不要假设是数学。

## 仓库结构

代码主体在 `smartmentor/` 下，前后端分离：

- `smartmentor/SmartMentor-back/` — Spring Boot 3 + Java 17 后端（Maven）
- `smartmentor/smartmentor-web/` — Vue 3 + Vite 前端
- `smartmentor_all.sql` — 完整数据库脚本（约 35 张表）
- `JAVA期末大作业/`、`docx_render_qa/`、`gen_doc.py` — 文档生成工具链（项目同时作为 Java 期末大作业提交）
- `比赛材料/` — 答辩 PPT、宣传视频、截图

## 常用命令

后端（在 `smartmentor/SmartMentor-back/` 下，Windows 用 `mvnw.cmd`，bash 用 `./mvnw`）：

```bash
./mvnw spring-boot:run          # 启动后端，监听 http://localhost:8080
./mvnw clean test               # 运行全部测试
./mvnw test -Dtest=类名#方法名   # 运行单个测试
./mvnw clean package            # 打包
```

前端（在 `smartmentor/smartmentor-web/` 下）：

```bash
npm install
npm run dev      # Vite dev server，http://localhost:5173，/api 代理到 :8080
npm run build    # 生产构建到 dist/
npm run smoke    # 冒烟检查（node scripts/smoke.mjs）
```

前端依赖后端跑在 `:8080` 才能联调；Vite 已配置 `/api` 代理及 SSE（text/event-stream）透传。

## 运行所需的外部依赖

后端需要 **MySQL 8** + **Redis 6+**，以及至少一个大模型 API key。运行时密钥全部从环境变量读取（见后端 README / `.env.example`），关键变量：

- `SMARTMENTOR_DB_PASSWORD` / `SMARTMENTOR_REDIS_PASSWORD`
- `SMARTMENTOR_SPARK_API_KEY`（讯飞星火，主用）、`SMARTMENTOR_DEEPSEEK_API_KEY`（DeepSeek，备用）
- `SMARTMENTOR_JWT_SECRET`
- `SMARTMENTOR_LLM_PROVIDER`（默认 `deepseek`）、`SMARTMENTOR_LLM_FALLBACK_ENABLED`

没有数据库/大模型时的演示开关：设 `SMARTMENTOR_OFFLINE_DEMO_ENABLED=true`，Agent 改用 `OfflineDemoService` 的本地结构化响应。配置详见 `application.yml` / `application-dev.yml`，profile 默认 `dev`，JPA `ddl-auto: update`。

## 核心架构

### 多 Agent 协作引擎（后端 `agent/` 包）

这是项目的灵魂，理解它需要读多个文件：

- `AgentOrchestrator` — 事件驱动编排器。两种用法：`executePipeline(ctx, agents...)` 线性流水线，或 `on(event, handler)` + `fireEvent(event, ctx)` 事件级联。有 `MAX_COLLABORATION_ROUNDS=10` 防止事件链无限递归。`AgentContext.sessionData` 是 Agent 间共享的累积数据。
- `AgentCollaborationConfig` — 在 `@PostConstruct` 里注册事件→处理器的协作链路。这是看清 Agent 如何串联的唯一入口：
  - `DIAGNOSIS_COMPLETE` → TracingAgent（溯源）
  - `TRACING_COMPLETE` / `CROSS_MODULE_ROOT_FOUND` → PlanningAgent（生成路径）
  - `MASTERY_NOT_REACHED` / `CONSECUTIVE_ERRORS` → TeachingAgent（重新教学/干预）
  - `NEW_WEAKNESS_FOUND` → TracingAgent
- 各 Agent 继承 `BaseAgent`，实现 `execute(ctx)` 返回 `AgentResponse`（含 `data` 和可选的级联 `event`）。Agent 包括 Diagnostic / Tracing / Planning / Teaching / Evaluation，以及 Profile / Presentation / Resource。
- Agent 的系统提示词外置在 `src/main/resources/prompts/*.md`，由 `PromptTemplateService` 加载。

### 大模型适配层（service 包）

多提供商可路由 + 回退：`ChatModelClient` 接口 → `SparkChatClient`（讯飞，主用）/ `DeepSeekChatClient`（备用），均继承 `AbstractOpenAiChatClient`（OpenAI 兼容 HTTP）。`LlmService` 按 `llm.provider` 选主用、不可用时回退。每次大模型调用都审计进 `agent_run_log` 表（prompt hash/版本、模型、延迟、成功/回退标志、质量分、输入输出摘要）。

### 服务端权威评分（重要约束）

为防客户端篡改答案，评分一律基于服务端快照，忽略前端传来的 `correctAnswer`：

- 诊断题保存为 `diagnostic_session` 快照（不依赖 Redis）
- 课程练习题在学习路径上提交前快照
- 检查点提交同样对服务端快照评分

新增任何答题/评分逻辑必须沿用此模式。

### 知识图谱

`src/main/resources/knowledge-graph/*.json` 提供知识点元数据（前置依赖、常见错误、考试权重、预估分钟数），由 `KnowledgeGraphService` 加载，驱动溯源回溯和学习路径生成。

### 前端结构

- API 全部走 `src/api/index.js` 单一模块：统一注入 `Authorization: Bearer`、token 存 `localStorage` 的 `sm_token`、对非 JSON/401 响应做防御处理。改接口调用从这里入手。
- 路由 `src/router/`：按 `meta.roles`（学生/教师）做角色守卫；`meta.public` 为免登录页。学生端页面在 `src/views/`（诊断三件套、溯源结果、学习路径/课程节点、对话、报告等）。
- 可视化与渲染依赖：`katex`（公式）、`d3` + `markmap` + `mermaid`（知识图谱/思维导图）、`marked`（AI 回复 Markdown）、`gsap`（动画）。无状态管理库、无 UI 组件库——均手写。

## 约定

- 后端包名 `com.tricia.smartmentor`，分层：`controller` / `service` / `agent` / `repository` / `entity` / `dto`。
- 后端用 Lombok；持久层 Spring Data JPA + MySQL，部分大字段用 `hibernate-types-55` 存 JSON。
- 鉴权：JWT（`JwtAuthenticationFilter` + `JwtUtil`），`SecurityConfig` 配置放行/受限路由，接口按角色限制（`/api/teacher/**`、`/api/diagnostic/**` 等）。
- 注释、文档、面向用户的文案用中文。

## 已知问题与处理记录

- PR #3（`codex/opendesign-landing-page`）的 Open Design 首页改造已知移除了旧首页的登录、注册、开始学习入口：首页顶部、首屏和底部 CTA 现在主要是页内锚点。后续若需要恢复匿名用户从首页进入系统，应在 `smartmentor/smartmentor-web/src/views/Landing.vue` 重新接入 `/login`、`/register` 或注册后跳转 `/onboarding` 的入口。
- PR #3 曾同时新增 `public/landing/` 和 `public/opendesign-landing/` 两套相同图片资源，但页面只引用 `/opendesign-landing/...`。未引用的 `smartmentor/smartmentor-web/public/landing/` 已按处理记录删除，避免重复约 16.75 MiB 的静态资源。
