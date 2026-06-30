# SmartMentor 文档合集

> 本文件由以下文档合并而成，目录如下：

1. SmartMentor项目申报书
2. SmartMentor改造方案
3. SmartMentor_A3赛题代码落地改造方案
4. SmartMentor项目四视角评价与优化实施方案
5. SmartMentor-接口文档合集


<!-- ============================================================ -->
# 【SmartMentor项目申报书】
<!-- 源文件：SmartMentor项目申报书.md -->

# 中国软件杯大学生软件设计大赛 项目申报书

## 2026-06-02 当前项目实现状态补充

SmartMentor 当前已从最初方案中的“学生端 + 教师端原型”推进到可联调演示状态：

- 学生端包含：仪表盘、诊断测试、诊断历史、溯因分析、学习路径、学习节点课程/练习/检查点、我的作业、AI 对话、学习报告、个人设置。
- 教师端包含：班级管理、知识热力图、知识点薄弱学生下钻、AI 分层作业生成、作业历史、学生预警、班级周报。
- 角色与权限：学生和教师看到不同页面；后端对 `/api/teacher/**`、`/api/diagnostic/**`、`/api/learning/**`、`/api/homework/**` 等接口按角色限制。
- 作业闭环：教师生成分层作业后，学生可在“我的作业”中查看自己被分配的层级题目。后续还需补作业提交、批改和完成率统计。
- 数据库：主 SQL 已合并当前实体结构，包括 `class_student`、`teacher_homework`、`question_bank`、`agent_run_log`、`answer_record`、`mastery_history` 等表。

## 赛道：A3 — 基于大模型的个性化资源生成与学习多智体系统开发

---

## 一、项目基本信息

| 项目 | 内容 |
|------|------|
| **项目名称** | SmartMentor（智导师）—— 基于多智体协作的高中数学薄弱知识溯源与个性化突破系统 |
| **参赛赛道** | A3：基于大模型的个性化资源生成与学习多智体系统开发 |
| **技术栈** | Java 17 + Spring Boot 3 + Vue 3 + TypeScript |
| **目标用户** | 高中数学学生及其数学教师 |
| **核心定位** | 不只告诉学生"哪里不会"，更要发现"为什么不会"——通过高中数学知识图谱溯源定位薄弱根因，实现精准突破 |
| **知识图谱规模** | 一期构建核心知识图谱 80-120 个知识点，覆盖函数、导数、三角、向量、数列、解析几何等核心模块；演示版重点打磨导数与函数链路 |

---

## 二、项目背景与问题分析

### 2.1 教育痛点

当前高中数学学习中，学生普遍面临以下困境：

1. **"头痛医头"式学习**：学生发现导数题不会做，就刷大量导数题，但根本原因可能是对变化率和导数几何意义理解不清。传统学习工具只能识别"哪道题错了"，无法发现错误背后的知识断层。

2. **知识链条断裂隐蔽**：高中数学知识体系环环相扣——集合→函数→三角函数→向量→数列→不等式→导数→解析几何。一个环节薄弱，后续所有依赖它的知识点都会"连锁塌方"。例如，解析几何做不好，真正原因可能是二次方程组的求解能力不足，而二次方程组又依赖因式分解的基本功。这种深层断裂很难靠学生自己发现。

3. **"一刀切"教学模式**：班级授课制下，教师难以针对每个学生的具体薄弱环节提供差异化辅导。同一个数学知识点，不同学生的卡点可能完全不同。

4. **学习路径盲目**：学生不知道应该先补哪个知识点、后补哪个知识点，学习缺乏科学规划。数学知识的层次依赖关系复杂，乱序补习事倍功半。

5. **缺乏效果闭环**：学了之后不知道有没有真正掌握，缺乏量化的效果验证。

### 2.2 现有方案的不足

| 现有方案 | 局限性 |
|----------|--------|
| 传统题库APP | 只做推题，不分析错因，更不追溯知识根因 |
| AI对话助手 | 通用问答，缺乏学科知识结构理解 |
| 自适应学习系统 | 基于统计推荐，缺少知识图谱的深层关联推理 |
| 教师一对一辅导 | 资源有限，无法覆盖所有学生 |

### 2.3 我们的解决方案

**SmartMentor（智导师）** 提出"溯源式学习"理念——通过多智体协作和知识图谱递归回溯，从一个错误出发，沿知识点的前置依赖链逐层追溯，找到真正的薄弱根因，然后从根因开始、自底向上地制定学习计划，实现"治本"而非"治标"。

**核心差异化**：
- 不是"你错了这道题，再做十道类似的"
- 而是"你错了这道题，是因为知识点A不牢固，而A不牢固是因为前置知识B没掌握，我们从B开始补起"

---

## 三、系统总体设计

### 3.1 系统架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端展示层 (Vue 3 + TypeScript)           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ 学生学习端 │  │ 教师管理端 │  │ 实时对话  │  │  可视化知识图谱   │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                      API网关层 (Spring Boot 统一路由)              │
├─────────────────────────────────────────────────────────────────┤
│                    多智体协作引擎 (Agent Orchestrator)            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌────────┐  │
│  │诊断Agent │→│溯源Agent │→│规划Agent │→│教学Agent│→│评估Agent│  │
│  │Diagnostic│ │ Tracing  │ │Planning  │ │Teaching│ │Evaluate│  │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ └────────┘  │
│                    ↕ 事件驱动协作 ↕                               │
├─────────────────────────────────────────────────────────────────┤
│                       核心服务层                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐ │
│  │ 数学知识引擎   │ │ 个性化引擎    │ │    质量保障引擎           │ │
│  │MathKnowledge │ │Personalize   │ │  QualityAssurance        │ │
│  └──────────────┘ └──────────────┘ └──────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                       数据与模型层                                │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌──────────────┐  │
│  │ Neo4j  │ │ MySQL  │ │ Redis  │ │ Milvus │ │ LLM (DeepSeek) │  │
│  │知识图谱 │ │业务数据 │ │缓存/会话│ │向量检索 │ │  大语言模型    │  │
│  │        │ │        │ │        │ │(可选)  │ │              │  │
│  └────────┘ └────────┘ └────────┘ └────────┘ └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心技术创新点

#### 创新点一：知识图谱驱动的薄弱根因溯源

传统自适应学习系统只能告诉学生"你不会A"，SmartMentor能发现"你不会A是因为前置知识B没掌握，而B没掌握是因为更基础的C有问题"。

**技术实现**：
- 基于Neo4j构建高中数学核心知识图谱，一期覆盖80-120个知识点（聚焦函数、导数、三角、向量、数列、解析几何等核心模块），知识点之间建立"前置依赖"有向边
- 溯源Agent通过递归回溯前置依赖链，利用BFS/DFS遍历找到真正的薄弱根因
- 演示版重点打磨导数与函数链路，确保该链路上的知识点粒度精细、依赖关系经过教学逻辑验证

```
示例溯源路径一（数学内部纵向溯源）：
学生做错"导数应用题"
  → 检测知识点"导数的应用" 掌握度低
    → 回溯前置："导数的计算" 掌握度低
      → 继续回溯："平均变化率与瞬时变化率" ← 根因！掌握度仅 0.25
        → 从"变化率与导数几何意义"开始制定学习计划

示例溯源路径二（跨模块横向溯源）：
学生做错"解析几何：直线与圆锥曲线"
  → 检测知识点"联立方程求交点" 掌握度低
    → 回溯前置："一元二次方程判别式与韦达定理" 掌握度低
      → 继续回溯："因式分解与配方法" ← 根因！
        → 从代数基础开始补起，再回到解析几何
```

#### 创新点二：五位一体学生画像

不同于简单的"做对/做错"二元判断，SmartMentor构建五维学生画像：

| 维度 | 说明 | 数据来源 |
|------|------|----------|
| **知识状态** | 每个知识点的掌握概率（BKT模型） | 作答记录、诊断测试 |
| **错误模式** | 常犯的错误类型分类 | LLM错因分析 |
| **学习行为** | 学习时段偏好、专注时长、做题速度 | 行为日志 |
| **认知风格** | 偏好视觉/文字/案例/公式推导 | 交互反馈 |
| **目标画像** | 目标院校、薄弱模块优先级 | 学生设置 |

#### 创新点三：多智体事件驱动协作

五个Agent不是简单的线性流水线，而是通过事件驱动机制实现动态协作。诊断、溯源、规划、教学、评估分别对应学习闭环中的不同决策环节，不是简单对话角色拆分，而是各自负责独立的推理和决策任务：

```
协作规则示例：
- 评估Agent发现学习效果不达标 → 触发诊断Agent重新诊断
- 溯源Agent发现跨模块根因（如解析几何的根因在代数）→ 触发规划Agent补充代数基础
- 教学Agent发现学生连续3题错误 → 触发溯源Agent重新检查
- 规划Agent发现前置知识已掌握 → 跳过直达目标知识点
```

#### 创新点四：三层质量保障机制

解决大模型"幻觉"问题，确保生成内容的学科准确性：

| 层级 | 机制 | 说明 |
|------|------|------|
| **第一层** | RAG知识锚定 | 基于教材原文的向量检索，生成内容必须有教材依据 |
| **第二层** | LLM自检 | 用第二次LLM调用检查首次生成内容的正确性 |
| **第三层** | 规则校验 | 公式LaTeX语法检查、数值计算验证、数学符号一致性检查 |

---

## 四、多智体（Multi-Agent）详细设计

### 4.1 Agent架构设计

所有Agent继承统一的BaseAgent基类，遵循 **构建Prompt → 调用LLM → 解析响应** 的标准流程：

```java
public abstract class BaseAgent {
    protected final ChatLanguageModel llm;            // LangChain4j LLM接口
    protected final QualityAssurance qualityAssurance; // 质量保障引擎

    public final AgentResult execute(AgentContext context) {
        String prompt = buildPrompt(context);          // 子类实现：构建Prompt
        String response = llm.generate(prompt);        // 调用大模型
        AgentResult result = parseResponse(response);  // 子类实现：解析响应
        qualityAssurance.validate(result);              // 质量检查
        return result;
    }

    protected abstract String buildPrompt(AgentContext context);
    protected abstract AgentResult parseResponse(String response);
}
```

### 4.2 五大Agent详细说明

#### Agent 1：诊断Agent（DiagnosticAgent）

**职责**：通过自适应出题，快速定位学生的薄弱知识点。

**工作流程**：
1. 从知识图谱中选择关键知识点，生成诊断题目
2. 根据学生作答情况，基于IRT（项目反应理论）动态调整题目难度
3. 调用LLM分析每道题的错因，不仅判断对错，还分析"错在哪一步、为什么错"
4. 输出薄弱知识点列表及每个知识点的初步掌握度评估

**自适应机制**：
- 答对 → 难度上调，跳过同级知识点
- 答错 → 难度下调，深入该知识点子节点
- 通常5-8道题即可完成一个知识模块的诊断

#### Agent 2：溯源Agent（TracingAgent）— 核心创新

**职责**：沿知识图谱的前置依赖链递归回溯，找到薄弱知识的真正根因。

**核心算法**：
```
输入：薄弱知识点列表
    对每个薄弱知识点 K：
        1. 获取 K 的所有前置知识点 P1, P2, ...
        2. 对每个前置知识点 Pi：
           - 若 Pi 的掌握度 < 阈值（0.6）：
             → Pi 也是薄弱点，递归对 Pi 执行溯源
           - 若 Pi 的掌握度未知：
             → 调用诊断Agent快速测试 Pi
        3. 回溯终止条件：
           - 前置知识均已掌握 → 当前节点即为根因
           - 到达知识图谱叶节点（无前置依赖）
           - 达到最大回溯深度（防止无限递归）
        4. 跨模块检查：
           - 检查是否存在跨数学模块的依赖（如解析几何→代数）
           - 若存在跨模块依赖且该基础知识薄弱 → 跨模块溯源
输出：根因知识点集合（含完整溯源路径）
```

**深度溯源示例**：
```
解析几何"椭圆与直线联立"做错
  → 检查前置："直线方程" ✓（已掌握）
  → 检查前置："椭圆标准方程" ✓（已掌握）
  → 检查前置："联立方程组求解" ← 掌握度 0.35
    → 继续回溯："韦达定理" ← 掌握度 0.40
      → 继续回溯："一元二次方程求根" ← 掌握度 0.30 ← 根因！
  → 输出溯源路径：
    椭圆联立 ←(依赖)← 方程组求解 ←(依赖)← 韦达定理 ←(依赖)← 一元二次方程
  → 学习计划从"一元二次方程求根"开始补起
```

#### Agent 3：规划Agent（PlanningAgent）

**职责**：根据溯源结果，生成科学的个性化学习路径。

**算法**：
1. 将所有需要学习的知识点构建DAG（有向无环图）
2. 拓扑排序确定学习顺序（保证先学前置知识）
3. 结合学生画像分配时间：
   - 掌握度低的知识点分配更多时间
   - 考虑学生每日可用学习时间
   - 区分"紧急"（考试临近）和"系统"（长期提升）模式
4. 设置里程碑检查点

**输出示例**：
```
学习计划（总计约3小时）：
├── Day 1: 平均变化率与瞬时变化率（45分钟）← 根因知识点
│   ├── 概念讲解（15分钟）
│   ├── 基础练习（20分钟）
│   └── 检查点测试（10分钟）
├── Day 2: 导数的概念与几何意义（40分钟）
│   ├── 建立在变化率基础上讲解（15分钟）
│   ├── 强化练习（20分钟）
│   └── 检查点测试（5分钟）
├── Day 3: 导数的计算（50分钟）
│   └── ...
└── Day 4: 导数的应用（45分钟）← 最终目标知识点
    └── 综合测试（含原始错题变式）
```

#### Agent 4：教学Agent（TeachingAgent）

**职责**：根据学生画像生成个性化的教学内容和练习题。

**三级教学策略**：

| 掌握度 | 策略 | 内容生成方式 |
|--------|------|-------------|
| < 0.4 | 基础夯实 | 概念重讲 + 直观案例 + 基础练习 |
| 0.4-0.7 | 强化突破 | 错因针对性讲解 + 变式训练 |
| > 0.7 | 拓展提升 | 综合应用 + 高考真题 + 能力迁移 |

**个性化维度**：
- 认知风格适配：视觉型学生多用图表，逻辑型学生多用推导
- 错误模式针对：如果学生常犯"符号错误"，在教学中强化符号处理
- RAG增强：从教材向量库中检索相关原文，确保内容准确

#### Agent 5：评估Agent（EvaluationAgent）

**职责**：验证学习效果，更新学生画像，触发学习闭环。

**评估机制**：
1. 每个知识点学习后立即进行检查点测试（3-5题）
2. 调用LLM分析答题过程，不只看对错，还看解题思路
3. 使用BKT模型更新知识点掌握概率
4. 判断是否达标（掌握度 ≥ 0.8）
5. 未达标 → 触发事件，驱动诊断Agent重新分析

### 4.3 Agent协作机制

```
事件驱动协作规则：

┌──────────────────────────────────────────────────────┐
│  评估Agent: 学习效果不达标                              │
│  → 事件: MASTERY_NOT_REACHED                         │
│  → 触发: 诊断Agent 重新诊断该知识点                     │
├──────────────────────────────────────────────────────┤
│  溯源Agent: 发现跨模块根因（如解析几何根因在代数）        │
│  → 事件: CROSS_MODULE_ROOT_FOUND                     │
│  → 触发: 规划Agent 生成基础模块补充计划                  │
├──────────────────────────────────────────────────────┤
│  教学Agent: 学生连续3题错误                             │
│  → 事件: CONSECUTIVE_ERRORS                          │
│  → 触发: 溯源Agent 重新检查是否遗漏了前置依赖            │
├──────────────────────────────────────────────────────┤
│  规划Agent: 检测到前置知识已掌握                         │
│  → 事件: PREREQUISITE_MASTERED                       │
│  → 动作: 跳过当前步骤，直达下一个目标知识点              │
├──────────────────────────────────────────────────────┤
│  诊断Agent: 发现新的薄弱点                              │
│  → 事件: NEW_WEAKNESS_FOUND                          │
│  → 触发: 溯源Agent 对新薄弱点进行根因分析               │
└──────────────────────────────────────────────────────┘
```

**协作编排器（AgentOrchestrator）** 负责：
- 接收事件并路由到对应Agent
- 管理Agent执行的并发控制（CompletableFuture）
- 维护会话上下文在多个Agent间的传递
- 防止Agent循环触发（设置最大协作轮次）

---

## 五、高中数学知识图谱深度设计

### 5.1 设计理念："聚焦数学，做深做透"

SmartMentor专注于高中数学单学科，将所有资源集中在构建一张精细、高质量的高中数学知识图谱上。一期聚焦核心模块（函数、导数、三角、向量、数列、解析几何），构建80-120个知识点；演示版重点打磨导数与函数链路。相比泛学科的浅层覆盖，单学科深度设计能够：
- 知识点粒度更细：从模块→章节→知识点→子技能，最细粒度到具体解题方法
- 前置依赖更精确：每条依赖边都经过教学逻辑验证
- 错误模式更专业：针对数学学科建立完整的错误分类体系
- 题目覆盖更充分：每个知识点配备充足的分层题库

### 5.2 知识图谱结构

```
高中数学知识图谱（一期 80-120 个知识点，聚焦核心模块）：

一、函数（约30个知识点）—— 核心主链路
├── 函数的概念与表示法
├── 定义域与值域
├── 单调性与最值
├── 奇偶性与对称性
├── 指数函数 ← 依赖：指数运算
├── 对数函数 ← 依赖：对数运算、指数函数
├── 幂函数
└── 函数与方程、函数模型

二、导数（约25个知识点）—— 核心主链路
├── 平均变化率与瞬时变化率 ← 依赖：函数概念
├── 导数的概念与几何意义 ← 依赖：变化率、函数单调性
├── 导数的计算 ← 依赖：导数概念、基本初等函数
├── 导数与单调性 ← 依赖：导数计算、函数单调性
├── 极值与最值 ← 依赖：导数与单调性
├── 导数的应用 ← 依赖：极值与最值
└── 构造函数与恒成立问题

三、三角函数（约20个知识点）
├── 任意角与弧度制
├── 三角函数定义
├── 同角关系与诱导公式
├── 正弦/余弦函数图像与性质
├── 和差角公式 ← 依赖：三角函数定义
├── 二倍角公式 ← 依赖：和差角公式
└── 三角恒等变换

四、向量（约15个知识点）
├── 向量的概念与线性运算
├── 向量的坐标表示
├── 数量积 ← 依赖：三角函数
└── 向量的应用

五、数列（约15个知识点）
├── 等差数列 ← 依赖：一次函数
├── 等比数列 ← 依赖：指数运算
├── 求和方法（裂项、错位相减等）
└── 数列与函数的关系

六、解析几何（约20个知识点）
├── 直线方程 ← 依赖：一次函数、向量
├── 圆的方程
├── 椭圆 ← 依赖：二次方程组
├── 直线与圆锥曲线 ← 依赖：韦达定理、判别式
└── 面积/最值综合问题

（二期扩展：集合与逻辑、不等式、立体几何、概率与统计等模块）
```

### 5.3 数学专属错误分类体系

针对数学学科建立精细的错误分类，支持LLM精准诊断：

| 错误大类 | 子类 | 示例 |
|----------|------|------|
| **概念性错误** | 定义理解偏差 | 把"单调递增"理解为"值越来越大" |
| | 概念混淆 | 混淆"极值"与"最值" |
| **计算性错误** | 符号错误 | 负号丢失、移项变号遗漏 |
| | 运算法则错误 | (a+b)² = a²+b² |
| | 精度错误 | 分数化简不彻底 |
| **逻辑性错误** | 充要条件误判 | 必要条件当充分条件用 |
| | 分类讨论遗漏 | 忽略参数为零的特殊情况 |
| | 推理跳步 | 省略关键步骤导致逻辑断裂 |
| **方法性错误** | 方法选择不当 | 该用韦达定理却硬算方程组 |
| | 转化方向错误 | 不等式放缩方向反了 |
| **审题性错误** | 条件遗漏 | 忽略"x>0"的隐含条件 |
| | 问题误读 | 求最小值看成求最大值 |

### 5.4 数学内容专属能力

| 能力 | 说明 |
|------|------|
| **LaTeX公式渲染** | 所有数学表达式使用LaTeX标准渲染，支持行内和块级公式 |
| **函数图像生成** | 基于ECharts动态绘制函数图像，支持交互缩放 |
| **几何图形绘制** | 平面几何和立体几何的可视化辅助 |
| **解题步骤拆解** | LLM生成的解答按步骤结构化展示，每步可展开查看详解 |
| **数学符号输入** | 前端支持数学符号快捷输入面板 |
| **计算过程验证** | 规则引擎自动验证数值计算步骤的正确性 |

---

## 六、技术架构详细设计

### 6.1 技术选型

| 层级 | 技术 | 选型理由 |
|------|------|---------|
| **后端框架** | Spring Boot 3.2 + Java 17 | 成熟生态，单体架构简洁高效 |
| **LLM集成** | LangChain4j + DeepSeek | Java原生LLM框架，对接DeepSeek大模型API |
| **知识图谱** | Neo4j 5.x | 图数据库标杆，Cypher查询语言表达力强 |
| **向量数据库** | Milvus 2.x（可选模块） | 开源向量检索引擎，支撑RAG；非核心依赖，可按需启用 |
| **关系数据库** | MySQL 8.0 | 业务数据持久化 |
| **ORM框架** | MyBatis-Plus | 简化CRUD，支持动态SQL |
| **缓存** | Redis 7.x | 会话管理、热点数据缓存、排行榜 |
| **前端框架** | Vue 3 + TypeScript + Vite | 组合式API，类型安全，快速开发 |
| **UI组件库** | Element Plus + ECharts | 丰富的组件和图表支持 |
| **实时通信** | SSE (Server-Sent Events) | LLM流式输出，Spring WebFlux支持 |
| **认证** | JWT + Spring Security | 无状态认证，支持多角色 |
| **构建部署** | Docker + Docker Compose | 一键部署所有服务 |

### 6.2 后端项目结构

```
smart-mentor-server/
├── pom.xml
├── src/main/java/com/smartmentor/
│   ├── SmartMentorApplication.java
│   │
│   ├── agent/                          # 多智体核心
│   │   ├── base/
│   │   │   ├── BaseAgent.java          # Agent基类
│   │   │   ├── AgentContext.java        # Agent上下文
│   │   │   └── AgentResult.java         # Agent结果
│   │   ├── diagnostic/
│   │   │   └── DiagnosticAgent.java     # 诊断Agent
│   │   ├── tracing/
│   │   │   └── TracingAgent.java        # 溯源Agent（核心）
│   │   ├── planning/
│   │   │   └── PlanningAgent.java       # 规划Agent
│   │   ├── teaching/
│   │   │   └── TeachingAgent.java       # 教学Agent
│   │   ├── evaluation/
│   │   │   └── EvaluationAgent.java     # 评估Agent
│   │   └── orchestrator/
│   │       ├── AgentOrchestrator.java   # Agent编排器
│   │       └── CollaborationRules.java  # 协作规则引擎
│   │
│   ├── math/                            # 数学学科专属
│   │   ├── MathKnowledgeService.java    # 数学知识服务
│   │   ├── MathPromptStrategy.java      # 数学Prompt策略
│   │   ├── MathErrorClassifier.java     # 数学错误分类器
│   │   ├── MathContentRenderer.java     # 数学内容渲染（LaTeX等）
│   │   ├── MathQuestionGenerator.java   # 数学题目生成
│   │   └── MathCalculationValidator.java # 数学计算验证
│   │
│   ├── knowledge/                       # 知识图谱
│   │   ├── entity/
│   │   │   └── KnowledgePoint.java      # 知识点实体
│   │   ├── repository/
│   │   │   └── KnowledgeGraphRepository.java
│   │   └── service/
│   │       └── KnowledgeGraphService.java
│   │
│   ├── profile/                         # 学生画像
│   │   ├── model/
│   │   │   └── StudentProfile.java      # 五维画像模型
│   │   ├── engine/
│   │   │   ├── PersonalizationEngine.java
│   │   │   └── BKTEngine.java           # 贝叶斯知识追踪
│   │   └── service/
│   │       └── ProfileService.java
│   │
│   ├── quality/                         # 质量保障
│   │   ├── QualityAssurance.java        # 三层质量检查
│   │   ├── RAGAnchor.java               # RAG锚定层
│   │   ├── LLMSelfCheck.java            # LLM自检层
│   │   └── RuleValidator.java           # 规则校验层
│   │
│   ├── effectiveness/                   # 效果量化
│   │   ├── EffectivenessTracker.java    # 效果追踪器
│   │   └── ReportGenerator.java         # 报告生成
│   │
│   ├── engagement/                      # 学习激励
│   │   ├── EngagementEngine.java        # 激励引擎
│   │   ├── mission/                     # 每日任务
│   │   ├── growth/                      # 成长体系
│   │   └── reminder/                    # 智能提醒
│   │
│   ├── controller/                      # API控制器
│   │   ├── ChatController.java          # SSE流式对话
│   │   ├── DiagnosticController.java    # 诊断API
│   │   ├── LearningController.java      # 学习API
│   │   ├── ProfileController.java       # 画像API
│   │   ├── TeacherController.java       # 教师端API
│   │   └── ReportController.java        # 报告API
│   │
│   ├── config/                          # 配置
│   │   ├── LangChainConfig.java         # LangChain4j配置
│   │   ├── Neo4jConfig.java
│   │   ├── MilvusConfig.java
│   │   └── SecurityConfig.java
│   │
│   └── common/                          # 公共模块
│       ├── exception/
│       ├── result/
│       └── utils/
│
├── src/main/resources/
│   ├── application.yml
│   ├── prompts/                         # Prompt模板
│   │   ├── diagnostic/
│   │   ├── tracing/
│   │   ├── planning/
│   │   ├── teaching/
│   │   └── evaluation/
│   └── knowledge/                       # 知识图谱初始数据
│       └── math-graph.cypher            # 高中数学完整知识图谱
```

### 6.3 前端项目结构

```
smart-mentor-web/
├── package.json
├── vite.config.ts
├── src/
│   ├── App.vue
│   ├── main.ts
│   ├── router/
│   │   └── index.ts
│   ├── stores/                          # Pinia状态管理
│   │   ├── user.ts
│   │   ├── learning.ts
│   │   └── diagnostic.ts
│   ├── views/
│   │   ├── student/                     # 学生端
│   │   │   ├── Dashboard.vue            # 学习总览
│   │   │   ├── Diagnostic.vue           # 智能诊断
│   │   │   ├── LearningPath.vue         # 学习路径
│   │   │   ├── ChatTutor.vue            # AI对话辅导
│   │   │   ├── KnowledgeMap.vue         # 知识图谱可视化
│   │   │   ├── Profile.vue              # 个人画像
│   │   │   └── Report.vue              # 学习报告
│   │   └── teacher/                     # 教师端
│   │       ├── ClassOverview.vue        # 班级总览
│   │       ├── HeatMap.vue              # 薄弱知识热力图
│   │       ├── HomeworkGen.vue          # 分层作业生成
│   │       ├── StudentAlert.vue         # 学生预警
│   │       └── WeeklyReport.vue         # 周报生成
│   ├── components/
│   │   ├── chat/
│   │   │   ├── ChatWindow.vue           # 对话窗口
│   │   │   ├── SSEMessage.vue           # SSE流式消息
│   │   │   └── MathRenderer.vue         # LaTeX渲染
│   │   ├── graph/
│   │   │   └── KnowledgeGraph.vue       # D3.js知识图谱
│   │   ├── diagnostic/
│   │   │   ├── QuestionCard.vue         # 题目卡片
│   │   │   └── TracingPath.vue          # 溯源路径可视化
│   │   └── common/
│   │       ├── RadarChart.vue           # 雷达图
│   │       └── ProgressRing.vue         # 进度环
│   └── api/
│       ├── auth.ts
│       ├── diagnostic.ts
│       ├── learning.ts
│       └── teacher.ts
```

### 6.4 数据库设计

#### 核心表结构

```sql
-- 学生基本信息表
CREATE TABLE student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(50),
    grade VARCHAR(20),        -- '高一'/'高二'/'高三'
    school VARCHAR(100),
    avatar_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 五维学生画像表
CREATE TABLE student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    learning_style VARCHAR(20),        -- 'visual'/'logical'/'example'/'formula'
    daily_study_minutes INT DEFAULT 60,
    preferred_time_slot VARCHAR(20),   -- 'morning'/'afternoon'/'evening'
    target_school VARCHAR(100),
    weak_module_priority JSON,         -- ["导数", "解析几何", "三角函数"]
    overall_mastery DECIMAL(3,2),      -- 数学总体掌握度 0.00-1.00
    ability_param DECIMAL(5,2),        -- IRT能力参数
    error_patterns JSON,               -- 错误模式统计
    knowledge_state JSON,              -- 各知识点掌握度 {kpId: mastery}
    streak_days INT DEFAULT 0,
    total_study_hours DECIMAL(10,1) DEFAULT 0,
    level INT DEFAULT 1,
    experience_points INT DEFAULT 0,
    last_diagnostic_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 学习记录表
CREATE TABLE learning_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    knowledge_point_id VARCHAR(50) NOT NULL,
    module VARCHAR(50),               -- '导数'/'解析几何'/'三角函数'等
    activity_type VARCHAR(30),         -- 'diagnostic'/'lesson'/'exercise'/'test'
    content_summary TEXT,
    duration_seconds INT,
    mastery_before DECIMAL(3,2),
    mastery_after DECIMAL(3,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 题目表
CREATE TABLE question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    knowledge_point_id VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,       -- 所属数学模块
    question_type VARCHAR(20),         -- 'choice'/'fill'/'solve'
    difficulty DECIMAL(3,2),           -- 0.00-1.00
    content TEXT NOT NULL,
    options JSON,                      -- 选择题选项
    answer TEXT NOT NULL,
    solution TEXT,                     -- 解析（含LaTeX）
    error_prone_points JSON,           -- 易错点标注
    source VARCHAR(50),                -- 'textbook'/'gaokao'/'mock'/'generated'
    gaokao_year INT,                   -- 高考真题年份（若适用）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 作答记录表
CREATE TABLE answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    student_answer TEXT,
    is_correct TINYINT(1),
    time_spent_seconds INT,
    error_type VARCHAR(50),            -- 错误类型分类
    error_detail VARCHAR(100),         -- 错误子类（如"符号错误-负号丢失"）
    error_analysis TEXT,               -- LLM错因分析
    solving_steps JSON,                -- 学生解题步骤记录
    context_type VARCHAR(30),          -- 'diagnostic'/'exercise'/'test'
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (question_id) REFERENCES question(id)
);

-- AI生成资源表
CREATE TABLE generated_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    knowledge_point_id VARCHAR(50),
    resource_type VARCHAR(30),         -- 'explanation'/'exercise'/'summary'/'plan'
    content TEXT NOT NULL,
    quality_score DECIMAL(3,2),        -- 质量评分
    rag_sources JSON,                  -- RAG引用来源
    model_name VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 学习路径表
CREATE TABLE learning_path (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    target_knowledge_point VARCHAR(50),
    root_cause_point VARCHAR(50),      -- 溯源发现的根因知识点
    path_nodes JSON,                   -- 有序知识点列表
    tracing_result JSON,               -- 溯源路径详情
    status VARCHAR(20) DEFAULT 'active', -- 'active'/'completed'/'paused'
    progress DECIMAL(3,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 教师表
CREATE TABLE teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    name VARCHAR(50),
    school VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 班级-学生关联表
CREATE TABLE class_student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_name VARCHAR(50),
    UNIQUE KEY uk_teacher_student (teacher_id, student_id),
    FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    FOREIGN KEY (student_id) REFERENCES student(id)
);
```

#### Neo4j知识图谱数据模型

```cypher
// 知识点节点
CREATE (kp:KnowledgePoint {
    id: 'math_derivative_application',
    name: '导数的应用',
    module: '导数与微积分',
    chapter: '第三章',
    difficulty: 0.7,
    importance: 0.9,           // 高考重要度
    gaokao_frequency: 0.85,    // 近5年高考出现频率
    description: '利用导数研究函数的单调性、极值、最值等',
    common_errors: '["极值点与最值点混淆", "忽略端点值", "分类讨论不完整"]'
})

// 同模块前置依赖关系
MATCH (a:KnowledgePoint {id: 'math_derivative_calc'}),
      (b:KnowledgePoint {id: 'math_derivative_application'})
CREATE (a)-[:PREREQUISITE_OF {weight: 0.9, type: 'strong'}]->(b)

// 跨模块前置依赖关系
MATCH (a:KnowledgePoint {id: 'math_quadratic_equation'}),
      (b:KnowledgePoint {id: 'math_conic_line_intersection'})
CREATE (a)-[:PREREQUISITE_OF {
    weight: 0.8,
    type: 'cross_module',
    description: '联立圆锥曲线与直线需要解一元二次方程'
}]->(b)

// 知识点与高考题型关联
MATCH (kp:KnowledgePoint {id: 'math_derivative_application'})
CREATE (kp)-[:EXAM_TYPE {
    type: '导数压轴题',
    typical_score: 12,
    position: '理科数学第21题'
}]->(kp)
```

### 6.5 核心API设计

| API | 方法 | 说明 |
|-----|------|------|
| `/api/auth/login` | POST | 学生/教师登录 |
| `/api/auth/register` | POST | 注册 |
| `/api/diagnostic/start` | POST | 启动诊断测试 |
| `/api/diagnostic/submit` | POST | 提交答案，获取下一题 |
| `/api/diagnostic/result` | GET | 获取诊断结果 |
| `/api/tracing/{studentId}/{kpId}` | GET | 获取溯源分析结果 |
| `/api/learning/path` | GET | 获取当前学习路径 |
| `/api/learning/path/generate` | POST | 生成新学习路径 |
| `/api/learning/lesson/{pathNodeId}` | GET | 获取个性化课程内容 |
| `/api/learning/exercise/submit` | POST | 提交练习答案 |
| `/api/chat/stream` | GET(SSE) | 流式AI对话辅导 |
| `/api/profile/overview` | GET | 获取学生画像总览 |
| `/api/profile/knowledge-map` | GET | 获取知识掌握图谱 |
| `/api/report/effectiveness` | GET | 获取效果报告 |
| `/api/teacher/class/heatmap` | GET | 班级薄弱知识热力图 |
| `/api/teacher/homework/generate` | POST | 生成分层作业 |
| `/api/teacher/alerts` | GET | 学生预警列表 |
| `/api/teacher/report/weekly` | GET | 周报生成 |

### 6.6 SSE流式输出实现

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam Long studentId,
            @RequestParam String message) {

        return Flux.create(sink -> {
            // 1. 获取学生画像
            StudentProfile profile = profileService.getProfile(studentId);
            // 2. 构建个性化Prompt
            String prompt = personalizationEngine.buildPrompt(profile, message);
            // 3. 流式调用LLM
            llm.generateStream(prompt)
                .onNext(token -> sink.next("data: " + token + "\n\n"))
                .onComplete(() -> sink.complete())
                .onError(sink::error);
        });
    }
}
```

---

## 七、学生端功能设计

### 7.1 功能全景

```
学生端功能架构：
├── 首页仪表盘
│   ├── 今日学习任务
│   ├── 连续学习天数
│   ├── 数学各模块掌握度雷达图
│   └── 最近学习动态
│
├── 智能诊断
│   ├── 选择数学模块（如：导数、解析几何等）
│   ├── 自适应出题（5-8题快速诊断）
│   ├── 即时错因分析（LLM深度分析每一步）
│   ├── 溯源路径可视化展示
│   └── 生成学习计划建议
│
├── 个性化学习
│   ├── 学习路径展示（路线图形式）
│   ├── 知识点讲解（个性化内容 + LaTeX公式）
│   ├── 练习与检查点测试
│   ├── AI对话辅导（流式输出）
│   └── 学习进度追踪
│
├── 知识图谱
│   ├── 交互式数学知识图谱浏览（D3.js）
│   ├── 掌握度颜色编码（红/黄/绿）
│   ├── 前置依赖关系展示
│   └── 点击知识点查看详情与关联题目
│
├── 学习报告
│   ├── 各模块掌握度变化趋势
│   ├── 错误模式分析（五大类错误统计）
│   ├── 效果对比（前测vs后测）
│   └── 能力雷达图（十大模块维度）
│
└── 成长激励
    ├── 每日任务清单
    ├── 学习等级与经验值
    ├── 里程碑成就
    └── 学习日历（打卡热力图）
```

### 7.2 核心交互流程

#### 流程一：首次使用 → 全面诊断 → 生成学习路径

```
学生注册登录
  → 选择年级、目标学校
  → 选择需要重点提升的数学模块
  → 开始诊断测试（约10-15分钟，覆盖核心模块）
  → 系统自动溯源分析
  → 生成数学知识掌握全景图谱
  → 推荐优先突破的知识点及根因
  → 生成第一份学习计划
```

#### 流程二：日常学习循环

```
打开APP → 查看今日学习任务
  → 进入学习路径中的当前知识点
  → 阅读个性化讲解内容
  → 完成练习题（3-5题）
  → 系统即时批改 + 错因分析
  → 通过检查点 → 进入下一个知识点
  → 未通过检查点 → 触发再次诊断/调整路径
  → 完成今日任务 → 获得经验值和连续天数+1
```

#### 流程三：遇到问题 → AI对话辅导

```
学生在学习过程中遇到困惑
  → 点击"问AI老师"
  → 输入问题（文字）
  → 系统结合当前学习上下文 + 学生画像
  → SSE流式输出个性化回答
  → 学生可以追问
  → 对话记录保存，供后续复习
```

---

## 八、教师端功能设计

> **定位说明**：核心闭环优先完成学生端（诊断→溯源→规划→教学→评估）；教师端作为加分项，聚焦班级热力图和分层作业两个核心功能。

### 8.1 功能概览

| 功能模块 | 说明 |
|----------|------|
| **班级总览** | 查看所管理班级的整体学习数据 |
| **薄弱知识热力图** | 可视化展示全班在各知识点上的掌握情况 |
| **分层作业生成** | AI根据学生分层自动生成差异化作业 |
| **学生预警** | 自动检测学习异常的学生（连续不登录、成绩下滑等） |
| **周报生成** | AI生成班级学习情况周报 |

### 8.2 薄弱知识热力图

```
知识点热力图示意（颜色代表班级平均掌握度）：

              集合  函数  三角  向量  数列  不等式  导数  概率
班级平均     🟢   🟡    🔴    🟡   🟢    🟡     🔴    🟢
A组(优秀)    🟢   🟢    🟡    🟢   🟢    🟢     🟡    🟢
B组(中等)    🟢   🟡    🔴    🟡   🟡    🟡     🔴    🟡
C组(薄弱)    🟡   🔴    🔴    🔴   🟡    🔴     🔴    🔴

🟢 ≥ 0.8    🟡 0.5-0.8    🔴 < 0.5
```

教师可以：
- 一眼看出全班最薄弱的知识点（如三角函数、导数）
- 点击具体知识点查看哪些学生特别薄弱
- 据此调整课堂教学重点

### 8.3 分层作业生成

教师选择知识点和分层规则后，系统自动生成三层难度的作业：

| 层级 | 适用学生 | 题目特点 |
|------|---------|---------|
| **基础层** | 掌握度 < 0.5 | 概念巩固题，注重基础 |
| **提高层** | 掌握度 0.5-0.8 | 综合应用题，注重方法 |
| **拓展层** | 掌握度 > 0.8 | 高考真题/竞赛变式，注重创新 |

---

## 九、效果量化体系

### 9.1 四维效果评估

| 维度 | 指标 | 计算方式 |
|------|------|---------|
| **掌握提升** | 前测-后测对比 | 同知识点前后两次诊断分数差 |
| **学习曲线** | 掌握度变化趋势 | BKT模型输出的掌握概率时序数据 |
| **错误消除** | 错误类型消失率 | 特定错误模式的重复出现率变化 |
| **能力雷达** | 多维能力变化 | 各知识模块的掌握度雷达图对比 |

### 9.2 效果可视化

```
学习效果报告示例：

📊 掌握度变化：
导数的应用:  学前 0.25 ████░░░░░░░░ → 学后 0.82 ████████████░
导数的计算:  学前 0.40 ████████░░░░ → 学后 0.88 █████████████
变化率理解:  学前 0.20 ████░░░░░░░░ → 学后 0.75 ███████████░░

📈 错误消除率：
计算粗心错误:    出现次数 15次 → 3次   消除率 80%
概念混淆错误:    出现次数 8次 → 1次    消除率 87.5%
公式记忆错误:    出现次数 6次 → 2次    消除率 66.7%

🎯 总体提升：平均掌握度从 0.28 提升至 0.82（+192.8%）
```

---

## 十、学习激励与留存设计

> **定位说明**：激励系统为辅助模块，在核心学习闭环完成后按优先级逐步实现。

### 10.1 激励体系

| 模块 | 设计 | 目的 |
|------|------|------|
| **每日任务** | 3个可选任务（诊断/练习/复习），完成获得经验值 | 引导日常学习行为 |
| **连续打卡** | 连续学习天数记录，7天/30天/100天里程碑奖励 | 培养学习习惯 |
| **成长等级** | Lv1学徒 → Lv10学霸，升级解锁新功能 | 长期成长动力 |
| **成就勋章** | "函数克星""导数达人""解析几何突破者""溯源之王"等特色成就 | 目标驱动 |
| **智能提醒** | 基于学习行为分析的个性化提醒（非固定时间轰炸） | 适时唤回 |

### 10.2 智能提醒策略

```
提醒规则：
- 连续2天未登录 → 温和提醒"你的学习路径在等你"
- 学习路径即将过期（知识遗忘曲线）→ "该复习了，别让掌握度下降"
- 完成里程碑 → 正向激励"恭喜突破导数专题！"
- 同伴激励 → "你的同学今天学了45分钟，一起加油"（可选开启）

提醒频率限制：每天最多1条，避免打扰
```

---

## 十一、项目创新点总结

### 11.1 核心创新点

| 编号 | 创新点 | 说明 |
|------|--------|------|
| **1** | **知识图谱驱动的薄弱根因溯源** | 不只找到"不会什么"，更要找到"为什么不会"。通过80-120个核心知识点构成的高中数学知识图谱，沿前置依赖链递归回溯，定位薄弱的真正根因。这是本系统最核心的差异化能力。 |
| **2** | **跨模块深度溯源** | 发现解析几何困难可能源于代数基础薄弱，导数问题可能根因在函数概念。在数学知识体系内部实现深层跨模块追踪。 |
| **3** | **五维学生画像** | 超越简单的"对/错"判断，从知识状态、错误模式、学习行为、认知风格、目标画像五个维度构建立体学生画像。 |
| **4** | **数学专属错误分类体系** | 建立概念性、计算性、逻辑性、方法性、审题性五大类错误分类，支撑LLM精准诊断每一步解题过程。 |
| **5** | **事件驱动的多智体协作** | 五个Agent不是线性流水线，而是通过事件驱动实现动态协作。诊断、溯源、规划、教学、评估分别对应学习闭环中的不同决策环节，各自负责独立推理任务。 |
| **6** | **三层质量保障** | RAG知识锚定 + LLM自检 + 规则校验（含数学计算验证），有效控制大模型幻觉问题。 |

### 11.2 与传统方案的对比

| 能力维度 | 传统自适应学习 | 通用AI助手 | SmartMentor |
|----------|--------------|-----------|-------------|
| 薄弱点定位 | 基于统计推荐 | 用户自述 | **知识图谱溯源** |
| 根因分析 | ❌ 不支持 | ❌ 不支持 | **✅ 递归回溯** |
| 跨模块追踪 | ❌ 不支持 | 部分支持 | **✅ 跨模块深度溯源** |
| 个性化程度 | 难度自适应 | 对话式 | **五维画像驱动** |
| 错误诊断 | 只判对错 | 简单解释 | **五类错误精准分类** |
| 生成质量 | N/A | 有幻觉风险 | **三层质量保障** |
| 效果验证 | 简单统计 | ❌ | **四维量化评估** |
| 数学专属 | 通用 | 通用 | **LaTeX/图像/计算验证** |

---

## 十二、关键技术难点与解决方案

### 12.1 八大技术难点及应对

| 序号 | 难点 | 风险 | 解决方案 |
|------|------|------|---------|
| 1 | **知识图谱构建工作量大** | 核心知识点的前置关系需要数学教学专业知识 | 利用LLM辅助生成初始图谱 + 高中数学教材目录自动提取 + 人工校验；一期聚焦导数-函数主链路确保质量，分阶段扩展 |
| 2 | **LLM响应延迟** | 多Agent串行调用导致响应缓慢 | SSE流式输出感知优化 + Agent并行执行 + Redis缓存高频查询 + 预计算常见溯源路径 |
| 3 | **大模型幻觉** | 生成的数学内容可能有计算错误或公式错误 | 三层质量保障机制（RAG + LLM自检 + 数学规则校验），关键内容强制RAG |
| 4 | **冷启动问题** | 新用户无历史数据，画像不准 | 首次使用引导诊断测试（约15分钟），快速建立初始画像；提供年级默认画像作为基线 |
| 5 | **掌握度评估准确性** | BKT模型参数可能与实际偏差 | BKT + IRT结合，多信号融合（不只看对错，还看用时、解题步骤）；在线参数校准 |
| 6 | **数学公式渲染兼容性** | 不同终端LaTeX渲染效果可能不一致 | 采用KaTeX前端渲染 + 服务端预渲染降级方案；统一数学符号规范 |
| 7 | **学生持续使用动力不足** | 学习工具类产品留存率普遍偏低 | 游戏化激励体系 + 智能提醒 + 可视化进步 + 教师端推动 |
| 8 | **多Agent协作稳定性** | Agent之间可能产生循环触发或死锁 | 设置最大协作轮次 + 事件去重 + 超时机制 + 降级策略（超时直接给出基本建议） |

---

## 十三、项目开发计划

### 13.1 开发周期：约12周

| 阶段 | 时间 | 任务 | 交付物 |
|------|------|------|--------|
| **第一阶段：基础搭建** | 第1-2周 | 项目初始化、数据库设计、基础框架搭建、用户认证 | 可运行的项目骨架 |
| **第二阶段：知识图谱** | 第3-4周 | Neo4j知识图谱设计、导数-函数主链路40-60个知识点构建与验证、图谱查询服务 | 核心链路知识图谱 |
| **第三阶段：核心Agent** | 第5-7周 | 五大Agent实现、LangChain4j集成、Agent协作编排器、质量保障引擎 | Agent系统可运行 |
| **第四阶段：学生端前端** | 第8-9周 | 诊断流程界面、学习路径界面、AI对话界面、知识图谱可视化、LaTeX渲染 | 学生端完整可用 |
| **第五阶段：教师端与扩展** | 第10-11周 | 教师端核心功能（班级热力图、分层作业）、知识图谱扩展至三角/向量/解析几何模块（累计80-120知识点）、效果报告、题库充实 | 完整系统 |
| **第六阶段：优化与演示** | 第12周 | 性能优化、演示数据准备、Bug修复、演示PPT制作 | 可演示的完整产品 |

### 13.2 里程碑节点

```
M1（第2周末）: 基础框架搭建完成，能跑通HelloWorld
M2（第4周末）: 导数-函数主链路知识图谱构建完成（40-60个知识点），能执行图谱查询
M3（第7周末）: Agent系统完成，能完成诊断-溯源-规划-教学-评估全流程
M4（第9周末）: 学生端前端完成，能进行完整的学习体验
M5（第11周末）: 知识图谱扩展至80-120知识点，教师端核心功能（热力图+分层作业）可用
M6（第12周末）: 系统优化完成，演示就绪
```

---

## 十四、团队分工建议

| 角色 | 人数 | 职责 |
|------|------|------|
| **后端开发** | 2人 | Agent系统、核心服务、API开发 |
| **前端开发** | 1人 | Vue前端、可视化、LaTeX渲染、交互设计 |
| **知识图谱** | 1人 | 数学知识点梳理、图谱构建、题库建设、错误分类体系 |
| **项目管理** | 1人（兼任）| 进度跟踪、文档、演示准备 |

---

## 十五、演示方案

### 15.1 演示场景设计

**演示场景**：一个高二学生"小明"，在导数应用题上总是做错。

**演示流程**（约8分钟）：

1. **登录与诊断**（1.5分钟）
   - 小明登录系统
   - 选择"导数"模块开始智能诊断，系统出5道题
   - 小明在导数相关题目上表现不佳

2. **溯源分析**（2分钟）— 核心亮点
   - 系统展示溯源过程动画
   - 从"导数应用"沿知识图谱回溯
   - 最终定位根因："平均变化率与瞬时变化率理解不清"
   - 展示完整溯源路径的可视化

3. **学习路径生成**（1分钟）
   - 系统从根因知识点开始规划学习路径
   - 展示路线图：变化率与导数几何意义 → 导数概念 → 导数计算 → 导数应用

4. **个性化学习**（2分钟）
   - 进入"平均变化率与瞬时变化率"知识点
   - 展示针对小明认知风格生成的个性化讲解
   - 完成练习题，系统即时批改和分析

5. **AI对话辅导**（1分钟）
   - 小明对变化率概念有疑问
   - 在对话框中提问
   - 展示SSE流式输出的个性化回答

6. **教师端展示**（0.5分钟）
   - 切换到教师视角
   - 展示班级薄弱知识热力图
   - 快速演示分层作业生成

### 15.2 演示数据

预置以下演示数据：
- 高中数学核心知识图谱（80-120个知识点，聚焦函数、导数、三角、向量、数列、解析几何）
- 3个不同画像的学生账号（薄弱/中等/优秀）
- 1个教师账号（带20人模拟班级数据）
- 200+道分层题目（基础/提高/拓展，含近5年高考真题）
- 预设多条典型溯源路径用于演示（重点演示导数-函数链路）

---

## 十六、总结

SmartMentor（智导师）是一个基于多智体协作的高中数学薄弱知识溯源与个性化突破系统。其核心价值在于：

1. **溯源式学习**：不停留在表面的"你错了这道题"，而是深入到"你为什么错"——通过80-120个核心知识点构成的高中数学知识图谱递归回溯，找到真正的薄弱根因。

2. **数学深度专精**：聚焦高中数学单学科，从知识图谱粒度、错误分类体系、题库覆盖度、公式渲染到计算验证，每个环节都为数学量身打造。

3. **精准个性化**：五维画像驱动的真正个性化，不是"千人一面"的推荐，而是"千人千面"的内容生成。

4. **闭环验证**：四维效果量化体系，让学习效果可见、可量化、可比较。

5. **智能协作**：五个专业Agent通过事件驱动机制实现灵活协作，不是简单的工具堆砌，而是有机的智能系统。

SmartMentor将AI大模型的强大生成能力与结构化的高中数学知识图谱深度融合，在保障数学内容准确性的前提下，实现了真正以学生为中心的个性化数学学习体验。

---

*项目名称：SmartMentor（智导师）*
*参赛赛道：A3 — 基于大模型的个性化资源生成与学习多智体系统开发*
*技术栈：Java 17 + Spring Boot 3 + Vue 3 + TypeScript*



<!-- ============================================================ -->
# 【SmartMentor改造方案】
<!-- 源文件：SmartMentor改造方案.md -->

# SmartMentor 改造方案

## 2026-06-02 合并更新：当前实现状态

本方案已按当前项目代码重新校准。早期文档中“后端大量 Mock、教师作业不可领取、学习路径只是静态展示”等问题，已有一部分完成改造。

### 已完成并接入的能力

- 角色分离：学生与教师登录后进入不同页面；教师不能进入诊断、学习路径、做题、学习报告等学生页面；学生不能进入教师面板。
- 学生学习路径闭环：路径节点支持课程内容、针对练习、检查点、补救节点、继续下一节点；同一路由切换节点不再需要手动刷新。
- 题目选项与提交修复：练习题选项兼容字符串、对象、Map 等多种后端返回结构；LaTeX 渲染不再破坏 Vue 点击事件。
- 学习报告真实化：报告页已从后端真实接口读取数据，不再展示固定假数据。
- 教师热力图与下钻：教师可以按班级/模块查看知识点热力图，下钻到薄弱学生，并从知识点生成分层作业。
- 教师布置作业：作业保存到 `teacher_homework`，包含知识点、分层题目、学生层级分配、截止时间和备注。
- 学生领取作业：新增 `/api/homework` 和 `/api/homework/{homeworkId}`，学生端“我的作业”页面可查看老师分配给自己的层级题目。
- 数据库脚本合并：`smartmentor.sql` 已补入当前实体需要的 `class_student`、`teacher_homework`、`question_bank`、`agent_run_log` 等结构。

### 仍需补强的点

- 知识图谱当前加载 `函数`、`导数`、`三角函数`、`基础` 四类 JSON；前端仍展示 `向量`、`数列`、`解析几何`，需要补齐对应知识图谱文件或从下拉中移除。
- 教师作业已有题库复用和兜底生成，但仍需要进一步提高题目质量、答案解析完整度和人工审核能力。
- 学生作业目前完成“领取/查看”，尚未设计提交、批改、教师批阅与完成率统计闭环。
- 自动化测试仍偏少，应补诊断、学习路径、作业、权限矩阵的集成测试。

## 一、现状问题诊断

经全面审查后端（SmartMentor）和前端（smartmentor-web）代码，发现以下核心问题：

### 问题1：学习诊断形同虚设
- **题目固定**：3个模块各8题，硬编码在 `DiagnosticService.java` 内存中，学生每次做完全相同的题
- **评判粗暴**：答案仅做字符串比对（A/B/C/D），无法评估解题过程
- **自适应假象**：虽有难度调整逻辑（±0.10），但题库太小且固定，实际体验无差异
- **诊断后无路线**：诊断结束只输出一个准确率，不生成真实的学习路线

### 问题2：后端数据全是Mock
| 功能 | 申报书承诺 | 实际实现 |
|------|-----------|----------|
| 知识溯源 | Neo4j图遍历+BKT模型 | 硬编码3条依赖链，掌握度=`0.25+i*0.05` |
| 学习路线 | 拓扑排序个性化DAG | **永远生成相同5个分数节点**，与诊断结果无关 |
| 课程内容 | RAG检索教材+个性化生成 | 硬编码一份固定内容 |
| 练习批改 | LLM步骤分析 | `answer.contains("1/2")` 字符串匹配 |
| 学习报告 | 真实数据分析 | `new Random(studentId.hashCode())` 随机生成 |
| 知识图谱 | Neo4j 80-120节点 | 未接入任何图数据库 |
| 教师热力图 | 真实班级数据 | 随机数模拟 |
| AI作业生成 | LLM分层生成 | 3个硬编码模板 |

### 问题3：AI仅用于聊天，无多Agent体现
- 唯一的AI调用：`ChatService` → `DeepSeekService` → DeepSeek流式对话
- 无 DiagnosticAgent、TracingAgent、PlanningAgent、TeachingAgent、EvaluationAgent
- 无Agent编排器、无事件驱动协作
- 与普通ChatGPT套壳无本质区别

---

## 二、改造目标

将系统从"前端完整+后端Mock"状态改造为**真正的多Agent智能辅导系统**，重点实现：

1. **AI驱动的自适应诊断**：动态出题、过程分析、精准定位薄弱点
2. **知识溯源与个性化路线生成**：基于知识图谱的根因追溯和学习路径规划
3. **多Agent协作闭环**：5个Agent各司其职、事件驱动协同
4. **前后端数据真实联动**：消除所有Mock数据

---

## 三、改造架构设计

### 3.1 多Agent架构（核心改造）

```
┌─────────────────────────────────────────────────────┐
│                AgentOrchestrator                      │
│         (事件驱动编排器, 管理Agent协作流程)            │
├─────────────────────────────────────────────────────┤
│                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │诊断Agent │  │溯源Agent │  │规划Agent │           │
│  │Diagnostic│  │ Tracing  │  │Planning  │           │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘           │
│       │              │              │                 │
│  ┌────┴─────┐  ┌────┴─────┐                         │
│  │教学Agent │  │评估Agent │                         │
│  │Teaching  │  │Evaluation│                         │
│  └──────────┘  └──────────┘                         │
│                                                       │
├─────────────────────────────────────────────────────┤
│  BaseAgent 基类: buildPrompt → callLLM → parse →     │
│                   qualityCheck                        │
└─────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  DeepSeek LLM   │
│  (统一AI网关)    │
└─────────────────┘
```

### 3.2 知识图谱（简化但真实）

不依赖Neo4j，使用**JSON文件 + 内存图结构**实现轻量知识图谱：

```
knowledge-graph/
├── derivatives.json      # 导数模块 ~25个知识点
├── functions.json        # 函数模块 ~30个知识点
├── trigonometry.json     # 三角函数模块 ~20个知识点
└── cross-module.json     # 跨模块依赖关系
```

每个知识点结构：
```json
{
  "id": "derivative_concept",
  "name": "导数的概念",
  "module": "导数",
  "prerequisites": ["limit_concept", "average_rate_of_change"],
  "difficulty": 0.5,
  "examWeight": 0.8,
  "questionTemplates": ["concept_check", "calculation", "application"],
  "commonErrors": ["混淆平均变化率与瞬时变化率", "忽略Δx→0的极限过程"]
}
```

### 3.3 数据流架构

```
学生选择模块 → 诊断Agent(动态生成题目) → 学生作答 → 诊断Agent(AI分析答案)
     → 输出薄弱知识点列表
     → 溯源Agent(沿知识图谱追溯根因)
     → 规划Agent(生成个性化学习路径DAG)
     → 教学Agent(为每个节点生成教学内容)
     → 学生学习 → 评估Agent(检验掌握度)
     → 未达标? → 事件触发重新诊断/溯源
```

---

## 四、具体改造任务清单

### Phase 1: 基础设施改造（预计2-3天）

#### 任务1.1: 构建知识图谱数据
- [ ] 创建 `src/main/resources/knowledge-graph/` 目录
- [ ] 编写导数模块知识图谱JSON（25个知识点，含前置依赖关系）
- [ ] 编写函数模块知识图谱JSON（30个知识点）
- [ ] 编写三角函数模块知识图谱JSON（20个知识点）
- [ ] 编写跨模块依赖关系JSON
- [ ] 实现 `KnowledgeGraphService`：加载JSON、图遍历、前置查询、路径计算

#### 任务1.2: 改造DeepSeek服务为通用AI网关
- [ ] 重构 `DeepSeekService`，支持非流式调用（同步返回完整结果）
- [ ] 增加结构化输出支持（JSON mode / function calling模拟）
- [ ] 添加重试机制和降级策略
- [ ] 增加prompt模板管理（按Agent分类）

#### 任务1.3: 构建Agent基础框架
- [ ] 创建 `agent/` 包
- [ ] 实现 `BaseAgent` 抽象类：`buildPrompt()` → `callLLM()` → `parseResponse()` → `qualityCheck()`
- [ ] 实现 `AgentContext` 数据类（携带学生画像、当前状态等上下文）
- [ ] 实现 `AgentOrchestrator`：事件注册、事件触发、Agent调度
- [ ] 定义事件枚举：`DIAGNOSIS_COMPLETE`, `TRACING_COMPLETE`, `MASTERY_NOT_REACHED`, `CONSECUTIVE_ERRORS`等

---

### Phase 2: 诊断Agent改造（预计2-3天）

#### 任务2.1: AI动态出题
- [ ] 删除硬编码题库
- [ ] 实现 `DiagnosticAgent.generateQuestion(knowledgePoint, difficulty, studentProfile)`
- [ ] Prompt设计：根据知识点+难度+学生错误模式，让LLM生成选择题（含4选项、正确答案、考察点、常见错误分析）
- [ ] 输出格式：结构化JSON，包含题目LaTeX、选项、答案、解析
- [ ] 实现题目缓存（相同知识点+难度不重复出题）

#### 任务2.2: AI答案分析
- [ ] 实现 `DiagnosticAgent.analyzeAnswer(question, studentAnswer, correctAnswer)`
- [ ] Prompt设计：分析学生选错的原因（概念错误/计算错误/审题错误/方法错误/粗心）
- [ ] 输出：错误类型分类 + 具体错误分析 + 涉及的薄弱知识点

#### 任务2.3: 自适应诊断流程
- [ ] 基于知识图谱选取初始诊断知识点（模块核心节点）
- [ ] IRT自适应逻辑：答对→提高难度/跳到高阶节点；答错→降低难度/探测前置节点
- [ ] 动态决定是否继续出题（信息增益不足时提前终止）
- [ ] 诊断结束：汇总所有知识点掌握度估计，输出薄弱点列表

---

### Phase 3: 溯源Agent + 规划Agent（预计2-3天）

#### 任务3.1: 溯源Agent实现
- [ ] 实现 `TracingAgent.traceRootCause(weakPoints, studentMastery, knowledgeGraph)`
- [ ] 算法：BFS/DFS遍历知识图谱的prerequisite边
- [ ] 对未知掌握度的前置节点，调用诊断Agent快速验证（1-2题）
- [ ] 终止条件：前置节点已掌握(≥0.6) / 叶子节点 / 达到最大深度
- [ ] 输出：根因知识点 + 完整溯源路径 + 每个节点的掌握度
- [ ] 实现跨模块追溯（如解析几何问题追溯到代数基础）

#### 任务3.2: 规划Agent实现
- [ ] 实现 `PlanningAgent.generatePath(tracingResult, studentProfile)`
- [ ] Prompt设计：基于溯源结果+学生画像，规划学习顺序和时间分配
- [ ] 算法辅助：对溯源得到的知识点集合做拓扑排序（保证前置先学）
- [ ] 时间分配：掌握度越低的节点分配越多时间
- [ ] 输出：有序学习节点列表，每个节点含{知识点、预计时长、学习类型(概念/练习/检验)、目标掌握度}
- [ ] 存入 `LearningPath` 实体（替换现有硬编码逻辑）

---

### Phase 4: 教学Agent + 评估Agent（预计2-3天）

#### 任务4.1: 教学Agent实现
- [ ] 实现 `TeachingAgent.generateLesson(knowledgePoint, studentProfile, masteryLevel)`
- [ ] 三级内容策略：
  - 掌握度<0.4：基础重教（直观举例+图示描述+基础练习）
  - 0.4-0.7：针对性讲解（针对错因+变式训练）
  - \>0.7：进阶应用（综合题+高考真题）
- [ ] 输出结构化教学内容：概念讲解(Markdown+LaTeX) + 例题 + 练习题(3-5道)
- [ ] 适配学生认知风格（视觉型多图示/逻辑型多推导）

#### 任务4.2: 评估Agent实现
- [ ] 实现 `EvaluationAgent.assessMastery(knowledgePoint, exerciseResults)`
- [ ] 生成3-5道检测题（难度递进）
- [ ] AI分析解题过程，判断是否真正掌握
- [ ] BKT简化模型更新掌握度：`P(mastered|correct) = P(L)*P(G') / P(correct)`
- [ ] 掌握度≥0.8 → 通过，进入下一节点
- [ ] 掌握度<0.8 → 触发 `MASTERY_NOT_REACHED` 事件 → 重新教学或追溯

---

### Phase 5: Agent协作与事件驱动（预计1-2天）

#### 任务5.1: 事件驱动编排
- [ ] 实现协作规则注册：
  ```java
  orchestrator.on(DIAGNOSIS_COMPLETE, ctx -> tracingAgent.execute(ctx));
  orchestrator.on(TRACING_COMPLETE, ctx -> planningAgent.execute(ctx));
  orchestrator.on(MASTERY_NOT_REACHED, ctx -> {
      if (ctx.consecutiveFailures >= 3) tracingAgent.recheck(ctx);
      else teachingAgent.reteach(ctx);
  });
  orchestrator.on(CONSECUTIVE_ERRORS, ctx -> tracingAgent.execute(ctx));
  ```
- [ ] 实现最大协作轮次限制（防止无限循环）
- [ ] 实现Agent间上下文传递（StudentContext贯穿全流程）

#### 任务5.2: 智能聊天升级
- [ ] 改造 `ChatService`：根据当前学习上下文注入Agent能力
- [ ] 如果学生在学习某知识点时提问 → 教学Agent参与回答（注入知识点上下文+学生错误模式）
- [ ] 如果学生问"我哪里薄弱" → 触发诊断Agent
- [ ] 如果学生问"为什么学这个" → 注入溯源结果解释

---

### Phase 6: 前后端联动修复（预计1-2天）

#### 任务6.1: 后端数据真实化
- [ ] `ProfileService`：从真实诊断记录计算五维画像（非随机数）
- [ ] `ReportService`：从 `AnswerRecord` + `LearningPath` 计算真实报告数据
- [ ] `TeacherService`：热力图从班级学生的真实 `StudentProfile` 聚合
- [ ] `EngagementService`：任务完成与真实学习行为挂钩

#### 任务6.2: 诊断结果→学习路线全链路打通
- [ ] 诊断完成 → 自动调用溯源Agent → 自动调用规划Agent → 生成真实学习路线
- [ ] 前端"生成学习路线"按钮触发完整Agent链（而非直接返回Mock路线）
- [ ] 学习路线中每个节点的教学内容由教学Agent实时生成

---

## 五、技术实现要点

### 5.1 Prompt工程设计

**诊断Agent出题Prompt示例：**
```
你是高中数学诊断专家。根据以下信息生成1道诊断题：

【知识点】{knowledgePoint.name}
【难度要求】{difficulty} (0-1, 1最难)
【学生信息】年级:{grade}, 常见错误类型:{errorPatterns}
【已出过的题】{previousQuestionSummaries}

要求：
1. 输出JSON格式：{"question": "题目(LaTeX)", "options": ["A..","B..","C..","D.."], "answer": "A/B/C/D", "analysis": "解析", "targetKP": "考察知识点", "commonErrors": ["错误1","错误2"]}
2. 题目难度要符合要求，不要和已出过的题重复
3. 选项要有区分度，错误选项应该对应常见错误类型
```

**溯源Agent分析Prompt示例：**
```
你是数学知识诊断专家。学生在以下知识点表现薄弱：

{weakPoints列表，含掌握度}

这些知识点的前置依赖关系如下：
{knowledgeGraph子图}

请分析：
1. 最可能的根本原因知识点是哪个？
2. 推理过程是什么？
3. 建议的学习路径顺序？

输出JSON: {"rootCause": "知识点ID", "reasoning": "推理过程", "suggestedOrder": ["id1","id2",...]}
```

### 5.2 性能优化策略

| 场景 | 策略 |
|------|------|
| 出题延迟 | 诊断开始时预生成2-3题缓存，学生答题时异步生成下一题 |
| 溯源耗时 | 图遍历用规则算法，仅对不确定节点调用LLM验证 |
| 教学内容 | 首次生成后缓存至数据库，同知识点+同难度等级复用 |
| 学习路线 | 拓扑排序为规则算法，LLM仅参与时间分配建议 |

### 5.3 LLM调用成本控制

- 诊断：每次诊断约8-10次LLM调用（出题+答案分析）
- 溯源：约2-4次调用（仅不确定节点验证）
- 规划：1次调用
- 教学：每个节点1次调用（缓存后不再调用）
- 评估：每个检查点1-2次调用
- **单次完整流程预估：15-20次LLM调用**

### 5.4 Agent与规则的混合策略

**不是所有逻辑都需要LLM**，合理划分：

| 逻辑 | 实现方式 | 原因 |
|------|----------|------|
| 题目生成 | LLM | 需要多样性和难度控制 |
| 答案对错判断 | 规则 | 选择题直接比对 |
| 错因分析 | LLM | 需要理解解题思路 |
| 图遍历/拓扑排序 | 规则算法 | 确定性计算，无需AI |
| 掌握度更新 | BKT公式 | 数学模型，确定性 |
| 教学内容生成 | LLM | 需要个性化表达 |
| 时间分配 | 规则+LLM微调 | 规则算基础，LLM优化 |

---

## 六、改造优先级排序

```
紧急且重要（先做）          重要但可延后
┌─────────────────────┐    ┌─────────────────────┐
│ 1. 知识图谱构建      │    │ 5. 评估Agent        │
│ 2. Agent基础框架     │    │ 6. 事件驱动协作      │
│ 3. 诊断Agent改造     │    │ 7. 教师端数据真实化  │
│ 4. 溯源+规划Agent    │    │ 8. 报告数据真实化    │
└─────────────────────┘    └─────────────────────┘
```

**最小可演示版本（MVP）所需改造：**
1. 知识图谱JSON（至少导数模块完整）
2. 诊断Agent（AI出题+AI答案分析）
3. 溯源Agent（图遍历+根因定位）
4. 规划Agent（拓扑排序+个性化路线）
5. 教学Agent（动态生成课程内容）

完成以上5项后，系统即可展示完整的"诊断→溯源→路线→学习"闭环，体现多Agent协作。

---

## 七、改造后的关键差异对比

| 维度 | 改造前 | 改造后 |
|------|--------|--------|
| 诊断题目 | 固定24题循环 | AI实时生成，永不重复 |
| 答案评估 | ABCD字符串比对 | AI分析错因+分类 |
| 知识溯源 | 硬编码3条链 | 基于真实图谱的BFS追溯 |
| 学习路线 | 永远相同5节点 | 基于诊断结果的个性化DAG |
| 教学内容 | 一份固定Mock | AI按掌握度分层生成 |
| 练习批改 | `contains("1/2")` | AI过程分析+BKT更新 |
| Agent协作 | 不存在 | 5Agent事件驱动闭环 |
| AI角色 | 聊天机器人 | 多Agent教学系统的统一智能引擎 |

---

## 八、文件改动范围预估

### 新增文件（约15-20个）
```
src/main/java/com/tricia/smartmentor/
├── agent/
│   ├── BaseAgent.java
│   ├── AgentContext.java
│   ├── AgentOrchestrator.java
│   ├── AgentEvent.java
│   ├── DiagnosticAgent.java
│   ├── TracingAgent.java
│   ├── PlanningAgent.java
│   ├── TeachingAgent.java
│   └── EvaluationAgent.java
├── graph/
│   ├── KnowledgeGraphService.java
│   ├── KnowledgePoint.java (图节点POJO)
│   └── GraphTraversal.java
├── engine/
│   ├── BKTEngine.java
│   └── PersonalizationEngine.java
src/main/resources/knowledge-graph/
├── derivatives.json
├── functions.json
├── trigonometry.json
└── cross-module.json
```

### 重构文件（约8-10个）
```
- DeepSeekService.java     → 增加同步调用+JSON模式
- DiagnosticService.java   → 对接DiagnosticAgent（删除硬编码题库）
- TracingService.java      → 对接TracingAgent（删除Mock逻辑）
- LearningService.java     → 对接PlanningAgent+TeachingAgent（删除固定5节点）
- ChatService.java         → 注入学习上下文，增加Agent路由
- ProfileService.java      → 真实数据计算
- ReportService.java       → 真实数据聚合
- TeacherService.java      → 真实数据聚合
```

### 前端改动（极少）
前端API调用已完整对接后端，**数据结构不变的情况下前端无需改动**。仅需确保：
- 诊断返回的question结构保持兼容
- 学习路线的node结构保持兼容
- 溯源结果的graph结构保持兼容

---

## 九、风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|----------|
| LLM输出不稳定 | 生成的题目格式异常 | JSON Schema约束 + 重试 + 降级为缓存题 |
| LLM响应慢 | 用户等待时间长 | 预生成+异步+loading动画 |
| 知识图谱不完善 | 溯源断链 | 标记"未建模"节点，降级为模块级建议 |
| API调用成本 | DeepSeek费用 | 缓存策略 + 规则算法优先 + 调用计数告警 |
| 改造周期紧 | 无法全部完成 | 按Phase推进，优先保证MVP可演示 |

---

## 十、总结

当前系统的核心问题是：**前端已经做好了，后端全是Mock数据和硬编码逻辑，AI只用于聊天**。

改造的本质是：**用LLM替换硬编码逻辑，用Agent架构组织AI调用，用知识图谱提供结构化支撑**。

改造后系统将真正实现申报书中承诺的"多Agent协作的智能辅导系统"，而不仅仅是一个带聊天功能的静态学习网站。



<!-- ============================================================ -->
# 【SmartMentor_A3赛题代码落地改造方案】
<!-- 源文件：SmartMentor_A3赛题代码落地改造方案.md -->

# SmartMentor A3 赛题代码落地改造方案

## 1. 改造目标

将当前 SmartMentor 从偏高中数学场景的个性化学习系统，改造成符合中国软件杯 A3 赛题要求的：

**面向高校多专业、多学历层次的个性化资源生成与学习多智能体系统。**

系统需要体现：

- 不同专业学生适配
- 不同学历层次适配
- 对话式学习画像构建
- 多智能体协同
- 个性化学习路径规划
- 至少 5 类个性化学习资源生成
- 学习效果评估与路径动态调整

官网赛题参考：https://www.cnsoftbei.com/content-3-1286-1.html

## 2. 总体落地策略

不推翻现有系统，复用现有能力：

- 现有 `DiagnosticAgent` 继续负责诊断
- 现有 `TracingAgent` 继续负责知识缺口溯源
- 现有 `PlanningAgent` 继续负责路径规划
- 现有 `TeachingAgent` 扩展为资源生成 Agent
- 现有 `EvaluationAgent` 继续负责练习和检查点评估
- 现有 `ChatService` 改造成面向高校课程的对话式辅导入口

改造重点是：

1. 去掉高中数学固定语义
2. 增加专业、学历、课程、目标等画像维度
3. 将知识库从数学知识点改成多专业课程知识库
4. 将课程页从单一讲解页改成多类型资源生成中心
5. 将视频推荐从“一数高中数学”改成通用高校课程资源推荐

## 3. 阶段一：学生画像模型改造

### 3.1 后端实体

修改：

- `SmartMentor/src/main/java/com/tricia/smartmentor/entity/StudentProfile.java`

新增字段：

```java
@Column(name = "major_direction", length = 50)
private String majorDirection;

@Column(name = "education_level", length = 20)
private String educationLevel;

@Column(name = "current_course", length = 100)
private String currentCourse;

@Column(name = "learning_goal", length = 50)
private String learningGoal;

@Column(name = "foundation_level", length = 20)
private String foundationLevel;

@Column(name = "resource_preference", columnDefinition = "JSON")
private String resourcePreference;

@Column(name = "academic_interest", length = 255)
private String academicInterest;
```

建议保留原有字段：

- `learningStyle`
- `dailyStudyMinutes`
- `weakModulePriority`
- `errorPatterns`
- `knowledgeState`
- `overallMastery`
- `abilityParam`

原有 `targetSchool`、`targetScore` 不再作为核心字段，前端不再突出展示。

### 3.2 数据库 SQL

在 `D:\Idea\中国软件杯` 下新增或更新迁移脚本，例如：

- `SmartMentor_A3_profile_migration.sql`

内容：

```sql
ALTER TABLE student_profile
ADD COLUMN major_direction VARCHAR(50) NULL COMMENT '专业方向',
ADD COLUMN education_level VARCHAR(20) NULL COMMENT '学历层次',
ADD COLUMN current_course VARCHAR(100) NULL COMMENT '当前课程',
ADD COLUMN learning_goal VARCHAR(50) NULL COMMENT '学习目标',
ADD COLUMN foundation_level VARCHAR(20) NULL COMMENT '基础水平',
ADD COLUMN resource_preference JSON NULL COMMENT '资源偏好',
ADD COLUMN academic_interest VARCHAR(255) NULL COMMENT '学术或职业兴趣';
```

### 3.3 DTO 与服务

修改：

- `SmartMentor/src/main/java/com/tricia/smartmentor/dto/ProfileSettingsRequest.java`
- `SmartMentor/src/main/java/com/tricia/smartmentor/service/ProfileService.java`
- `SmartMentor/src/main/java/com/tricia/smartmentor/controller/ProfileController.java`
- `SmartMentor/src/main/java/com/tricia/smartmentor/dto/CurrentUserResponse.java`

`ProfileSettingsRequest` 增加同名字段，`ProfileService.updateSettings` 保存到 `StudentProfile`。

### 3.4 前端页面

修改：

- `smartmentor-web/src/views/Profile.vue`
- `smartmentor-web/src/views/Diagnostic.vue`

画像表单增加：

- 专业方向：计算机类、软件工程、电子信息、自动化、数据科学
- 学历层次：高职、本科、研究生
- 当前课程：人工智能基础、Java Web 开发、数字电路基础
- 学习目标：考试复习、项目实践、科研入门、就业技能、竞赛提升
- 基础水平：入门、基础、进阶
- 资源偏好：文档、视频、代码、论文、练习、项目案例

短期先用表单落地，后续再升级为对话式画像构建。

## 4. 阶段二：知识库多课程化

### 4.1 资源文件改造

当前目录：

- `SmartMentor/src/main/resources/knowledge-graph/`

当前数学文件：

- `functions.json`
- `derivatives.json`
- `trigonometry.json`
- `foundations.json`

建议新增高校课程知识库：

- `computer-ai-foundation.json`
- `software-java-web.json`
- `electronic-digital-circuit.json`

保留旧文件作为备份，运行时优先加载新课程知识库。

### 4.2 知识点 JSON 结构

示例：

```json
{
  "module": "机器学习",
  "course": "人工智能基础",
  "majorDirection": "计算机类",
  "nodes": [
    {
      "id": "ai_ml_basic",
      "name": "机器学习基础",
      "description": "理解监督学习、无监督学习、训练集、测试集、特征和标签等基本概念。",
      "course": "人工智能基础",
      "majorDirection": "计算机类",
      "educationLevels": ["本科", "研究生"],
      "difficulty": 3,
      "prerequisites": ["ai_intro", "math_probability_basic"],
      "resourceTypes": ["讲解文档", "分层练习", "代码案例", "视频推荐", "拓展阅读"]
    }
  ]
}
```

### 4.3 KnowledgeGraphService 改造

修改：

- `SmartMentor/src/main/java/com/tricia/smartmentor/service/KnowledgeGraphService.java`

新增能力：

```java
List<KnowledgeNode> findNodesByCourse(String majorDirection, String currentCourse, String educationLevel);

List<KnowledgeNode> findPrerequisites(String nodeId);

List<String> listCoursesByMajor(String majorDirection);
```

诊断和路径规划时，不再默认使用全量数学知识点，而是按画像中的专业、课程、学历过滤。

## 5. 阶段三：Agent Prompt 去高中化

### 5.1 Prompt 文件

修改：

- `SmartMentor/src/main/resources/prompts/diagnostic-system.md`
- `SmartMentor/src/main/resources/prompts/tracing-system.md`
- `SmartMentor/src/main/resources/prompts/planning-system.md`
- `SmartMentor/src/main/resources/prompts/teaching-system.md`
- `SmartMentor/src/main/resources/prompts/evaluation-system.md`

统一删除：

- 高中数学
- 高考
- 年级
- 分数提升
- 数学专项

统一替换为：

- 高校课程
- 专业方向
- 学历层次
- 当前课程
- 学习目标
- 学术或实践能力
- 多模态学习资源

### 5.2 Java fallback Prompt

修改：

- `DiagnosticAgent.java`
- `TracingAgent.java`
- `PlanningAgent.java`
- `TeachingAgent.java`
- `EvaluationAgent.java`
- `ChatService.java`

示例替换：

```java
"你是SmartMentor智学导师系统的诊断Agent，专门负责高中数学学习诊断。"
```

改成：

```java
"你是SmartMentor智学导师系统的高校课程个性化诊断Agent，"
+ "需要根据学生的专业方向、学历层次、当前课程、学习目标、基础水平和历史学习记录生成诊断内容。"
```

### 5.3 AgentContext 扩展

修改：

- `SmartMentor/src/main/java/com/tricia/smartmentor/agent/AgentContext.java`

建议在 `studentProfile` map 中传递全部画像字段，不一定新增强类型字段，减少侵入。

构建 AgentContext 的位置：

- `DiagnosticService`
- `TracingService`
- `LearningService`
- `ChatService`

都需要把 `StudentProfile` 的新字段写入上下文。

## 6. 阶段四：诊断流程课程化

### 6.1 API 请求体

修改：

- `DiagnosticController`
- `DiagnosticService`

`/diagnostic/start` 请求支持：

```json
{
  "majorDirection": "计算机类",
  "educationLevel": "本科",
  "course": "人工智能基础",
  "learningGoal": "项目实践",
  "foundationLevel": "基础"
}
```

如果请求体为空，则读取 `StudentProfile` 中的默认画像。

### 6.2 诊断题生成

诊断题范围：

```text
学生画像 -> 当前课程 -> 课程知识图谱 -> 诊断知识点集合 -> DiagnosticAgent 生成题目
```

题型建议：

- 概念理解题
- 应用判断题
- 案例分析题
- 代码理解题
- 实践方案题

不同学历层次差异：

- 高职：偏操作、案例和步骤
- 本科：理论与实践结合
- 研究生：加入论文、方法比较、开放问题

## 7. 阶段五：学习路径多专业化

### 7.1 后端路径节点

修改：

- `LearningService`
- `PlanningAgent`
- `LearningPath` 中的 `nodes` JSON 结构

节点建议结构：

```json
{
  "nodeId": "ai_ml_basic",
  "title": "机器学习基础",
  "course": "人工智能基础",
  "majorDirection": "计算机类",
  "educationLevelFit": "本科",
  "learningGoalFit": "项目实践",
  "status": "unlocked",
  "mastery": 0.42,
  "resourcePlan": ["讲解文档", "分层练习", "代码案例", "视频推荐"]
}
```

### 7.2 前端路径页

修改：

- `smartmentor-web/src/views/LearningPaths.vue`
- `smartmentor-web/src/views/LearningPathDetail.vue`

页面展示：

- 专业方向
- 当前课程
- 学历适配
- 学习目标
- 推荐资源类型
- 路径动态调整说明

## 8. 阶段六：Lesson 页面改成资源生成中心

### 8.1 后端返回结构

修改：

- `LearningService.generateLesson()`
- `TeachingAgent`

返回增加 `resources`：

```json
{
  "resources": {
    "document": {
      "title": "机器学习基础讲解文档",
      "content": "..."
    },
    "mindMap": {
      "title": "知识点思维导图",
      "nodes": []
    },
    "exercises": [],
    "extendedReading": [],
    "practiceCase": {
      "title": "鸢尾花分类实验",
      "steps": []
    },
    "video": {},
    "animationScript": {
      "title": "监督学习过程动画脚本",
      "scenes": []
    }
  }
}
```

### 8.2 前端 Lesson.vue

修改：

- `smartmentor-web/src/views/Lesson.vue`

新增资源卡片区：

1. 课程讲解文档
2. 知识点思维导图
3. 分层练习题
4. 拓展阅读材料
5. 实操案例
6. 视频推荐
7. 动画讲解脚本

保留原有：

- 练习提交
- 检查点
- AI 伴学

即页面结构变成：

```text
顶部：节点标题 + 专业/课程/学历/目标
左侧：资源生成中心
右侧：AI 伴学
底部：练习和检查点
```

## 9. 阶段七：视频推荐通用化

### 9.1 后端服务

当前：

- `BilibiliVideoService.java`

短期可以保留类名，但改为通用课程资源推荐逻辑。

修改点：

- 不再默认 `author-name: 一数`
- 不再固定 `高中数学`
- 搜索词按课程和知识点生成

建议搜索词：

```text
currentCourse + knowledgePointName
majorDirection + knowledgePointName
knowledgePointName + 教程
knowledgePointName + 公开课
knowledgePointName + 实战
knowledgePointName + 项目案例
```

优先作者配置：

```yaml
smartmentor:
  resource:
    preferred-authors:
      - 中国大学MOOC
      - 学堂在线
      - 北京大学
      - 清华大学
      - 浙江大学
```

### 9.2 长期命名

后续建议将：

```text
BilibiliVideoService
```

重构为：

```text
LearningResourceRecommendationService
```

## 10. 阶段八：报告页学习效果评估扩展

修改：

- `ReportService`
- `smartmentor-web/src/views/Report.vue`

新增指标：

- 当前课程掌握度
- 学习目标达成度
- 资源使用偏好
- 练习正确率
- 路径完成度
- 推荐资源命中率
- 弱项变化趋势

展示口径：

```text
系统根据学生画像、学习路径完成情况、资源使用反馈和练习评估结果，
动态调整后续资源推送策略。
```

## 11. 阶段九：前端全局文案替换

需要全局搜索并替换：

```text
高中
高中数学
高考
数学专项
年级
目标分数
```

重点文件：

- `smartmentor-web/src/views/Landing.vue`
- `smartmentor-web/src/views/Dashboard.vue`
- `smartmentor-web/src/views/Diagnostic.vue`
- `smartmentor-web/src/views/DiagnosticResult.vue`
- `smartmentor-web/src/views/TracingResult.vue`
- `smartmentor-web/src/views/LearningPaths.vue`
- `smartmentor-web/src/views/LearningPathDetail.vue`
- `smartmentor-web/src/views/Lesson.vue`
- `smartmentor-web/src/views/Profile.vue`
- `smartmentor-web/src/views/Report.vue`

替换方向：

```text
高中数学 -> 高校课程
目标分数 -> 学习目标
年级 -> 学历层次
数学模块 -> 课程模块
知识点掌握 -> 课程能力掌握
```

## 12. 推荐提交顺序

### 第 1 次提交：画像和文案基础

- 新增 `StudentProfile` 字段
- 新增迁移 SQL
- 更新 `ProfileSettingsRequest`
- 更新 `ProfileService`
- 前端 `Profile.vue` 增加画像字段
- 全局去掉明显高中数学文案

### 第 2 次提交：知识库和诊断课程化

- 新增 3 个高校课程知识库 JSON
- `KnowledgeGraphService` 支持课程过滤
- `DiagnosticService` 读取课程画像
- `DiagnosticAgent` Prompt 改造

### 第 3 次提交：路径和 Agent 多专业化

- `TracingAgent` Prompt 改造
- `PlanningAgent` Prompt 改造
- `LearningService` 路径节点增加课程字段
- 前端路径页展示专业/课程/学历

### 第 4 次提交：资源生成中心

- `TeachingAgent` 生成 `resources`
- `LearningService.generateLesson()` 输出多类型资源
- `Lesson.vue` 展示至少 5 类资源卡片

### 第 5 次提交：资源推荐和报告

- `BilibiliVideoService` 通用化
- `ReportService` 增加资源效果指标
- `Report.vue` 展示资源使用和目标达成

## 13. 演示闭环

建议最终演示流程：

1. 学生进入系统，填写或对话构建画像
2. 选择专业方向、学历层次和当前课程
3. 系统生成课程诊断题
4. DiagnosticAgent 识别薄弱知识点
5. TracingAgent 分析知识缺口来源
6. PlanningAgent 生成个性化路径
7. 进入学习节点
8. TeachingAgent 生成讲解文档、思维导图、练习题、拓展阅读、实操案例、视频推荐、动画脚本
9. 学生完成练习和检查点
10. EvaluationAgent 更新掌握度并调整后续路径

## 14. 风险与控制

### 风险 1：一次性改动太大

控制：按提交顺序分阶段改，每次都运行：

```powershell
mvn -q -DskipTests compile
npm run build
```

### 风险 2：数据库字段和实体不一致

控制：实体新增字段后同步维护 SQL；不要引入 H2 兜底，继续使用 MySQL。

### 风险 3：知识库结构变更影响诊断和路径

控制：`KnowledgeGraphService` 先兼容旧字段，再逐步切换到新字段。

### 风险 4：多模态资源真实生成成本高

控制：第一版先生成“视频推荐 + 动画脚本 + 结构化案例”，演示时说明可接入 SeeDance 等多模态模型。

## 15. 最小可交付范围

如果时间有限，至少完成：

1. 高中数学文案和 Prompt 全部替换为高校课程
2. 学生画像支持专业、学历、课程、目标、资源偏好
3. 内置 3 个高校课程样例知识库
4. Lesson 页面展示 5 类资源卡片
5. 演示流程能跑通一门课程的诊断、路径、资源生成、评估闭环




<!-- ============================================================ -->
# 【SmartMentor项目四视角评价与优化实施方案】
<!-- 源文件：SmartMentor项目四视角评价与优化实施方案.md -->

# SmartMentor 项目四视角评价与优化实施方案

## 2026-06-02 合并更新

当前项目已完成一轮关键闭环修复，本文后续评价中关于“教师作业无法落到学生端”“老师和学生页面未分离”“学习节点跳转需要刷新”“学习报告存在假数据”的描述，以本节为准更新。

- 学生端：已新增“我的作业”页面，学生可通过 `/api/homework` 获取老师布置且分配给自己的分层作业。
- 教师端：分层作业会保存到 `teacher_homework`，并写入 `student_assignments`；教师端按钮、知识点加载、默认班级逻辑已修复。
- 权限：前端路由与后端 Spring Security 已同步区分学生/教师访问范围。
- 学习路径：节点练习可选答案、可提交；下一题、补救节点、继续下一节点可正常跳转；同组件路由复用无需刷新。
- 数据：学习报告已走真实接口，数据库脚本已合并当前实体表结构。

后续优先级调整为：学生作业提交与批改闭环、教师端作业完成率统计、向量/数列/解析几何知识图谱补齐、关键路径自动化测试。

生成日期：2026-05-30  
更新日期：2026-06-02  
评估范围：

- 后端：`D:\Idea\中国软件杯\SmartMentor`
- 前端：`D:\Idea\中国软件杯\smartmentor-web`
- 配套材料：接口文档、数据库脚本、申报书、既有改造方案

## 一、项目现状速览

SmartMentor 当前已经具备一个较完整的“AI 数学学习辅导系统”雏形。后端采用 Spring Boot 2.7.18 + Java 17，包含认证、诊断、知识溯源、学习路径、教学内容、练习评估、报告、教师端等模块；前端采用 Vue 3 + Vite，包含学生端、教师端、诊断、聊天、报告、学习路径等页面。

与早期“前端完整、后端 Mock 较多”的状态相比，当前代码已经明显推进：后端已存在 `agent` 包、`KnowledgeGraphService`、知识图谱 JSON、`DeepSeekService` 同步/流式调用、诊断和溯源的异步 AI 分析链路。也就是说，项目不再只是普通聊天套壳，而是已经开始向“多 Agent + 知识图谱 + 学习闭环”靠近。

2026-06-01 已补一轮学生端真实闭环：学习路径节点不再只是展示列表，当前实现已经把“路径节点 -> AI 伴学 -> 针对练习 -> 检查点 -> 解锁下一节点 / 生成补救节点”接到前后端。也就是说，学生进入学习路径后可以在同一节点内看 TeachingAgent 内容、向绑定当前路径节点的 AI 提问、完成练习，再用检查点结果驱动路径状态变化。

但从比赛交付、评审追问和真实使用角度看，项目仍有几个高风险短板：

1. 敏感配置明文存在开发配置中，包括数据库、Redis、邮箱授权码、模型 API Key，需要立刻迁移到环境变量并轮换密钥。
2. 后端同时保留 Maven 和 Gradle 构建文件，工程入口不唯一，影响评审复现和团队协作。
3. 前端同时保留旧版 `js/`、`css/` 与新版 `src/`，且 `dist/`、`node_modules/` 存在于项目目录，交付边界不清晰。
4. Agent 链路已搭建，但仍有硬编码和兜底逻辑，例如练习答案默认返回 `"B"`、教师作业题目仍是占位文本。
5. 测试非常薄弱，后端主要只有 `contextLoads`，前端没有自动化测试和端到端冒烟脚本。
6. 当前已具备路径节点闭环，但“检查点后自动触发新一轮诊断/再规划”的入口还应继续强化，使它从节点级闭环升级为完整学习周期闭环。

## 二、四个角度评价

### 1. 批判者视角：最大问题不是没功能，而是可信度不足

批判者会重点攻击“是否真实智能”“是否能复现”“是否有安全底线”。

当前项目最容易被质疑的点：

- 智能性证据不足。虽然已有 `DiagnosticAgent`、`TracingAgent`、`PlanningAgent`、`TeachingAgent`、`EvaluationAgent`，但部分业务仍依赖 fallback，且未提供 Agent 输入输出日志、质量评分、失败降级说明。
- 数据闭环不够硬。诊断结果、知识点掌握度、学习路径和后续练习评估之间仍存在断点。特别是练习题答案存储与校验逻辑尚未完全真实化。
- 安全问题严重。开发配置文件中出现明文账号、密码、邮箱授权码和模型 API Key。比赛展示时一旦泄露，会直接降低项目专业度。
- 工程管理不清晰。Maven/Gradle 双构建、前端旧版代码和构建产物并存，让项目看起来像“赶工拼接”，而不是稳定工程。
- 测试不足。只有启动级测试，无法证明诊断、溯源、路径生成、鉴权、教师端聚合等关键路径稳定。

批判者给分：65/100。  
理由：想法和功能面足够，但工程可信度、数据真实性和安全治理还没有达到高质量竞赛项目标准。

### 2. 旁观者视角：项目完整度高，但主线表达需要收束

旁观者不会逐行看代码，而是看项目给人的第一印象。

优点：

- 功能覆盖面完整，学生端和教师端都有，适合作为比赛作品展示。
- 界面页面数量足够，能呈现从诊断到学习报告的完整流程。
- 技术关键词齐全：多 Agent、知识图谱、DeepSeek、BKT 思想、个性化学习路径、教师热力图。
- 已有申报书、接口文档、数据库脚本和改造方案，说明项目材料准备较充分。

不足：

- 目录中构建产物和依赖目录较多，旁观者会感觉项目不够干净。
- 叙事主线需要更聚焦。项目不应强调“我们做了很多功能”，而应强调“我们解决了传统在线学习无法定位根因、无法持续闭环的问题”。
- 需要准备一条固定、稳定、可重复的演示路径，避免现场依赖实时大模型波动。

旁观者给分：78/100。  
理由：项目外观已经像完整系统，但需要通过清晰演示和工程整理提升专业感。

### 3. 使用者视角：流程完整，但体验上还缺少确定性和信任感

学生使用者关心的是：能不能快速知道自己哪里不会、为什么不会、下一步学什么。

现有体验优势：

- 登录后有仪表盘、诊断、学习路径、课程、聊天、报告等完整入口。
- 诊断题支持逐题反馈和正确答案展示。
- 诊断结束后可以进入溯源和学习路径，符合学习场景。
- 教学内容支持 Markdown/LaTeX，适合数学表达。

学生侧问题：

- 如果大模型生成题目失败，诊断体验会中断或退化，需要更平滑的题库兜底。
- 反馈中要避免只告诉“错了”，应明确“错因、对应知识点、下一步练习”。
- 学习路径节点状态、掌握度更新、检查点解锁已经接入前端任务流，仍需用真实演示数据验证多轮循环稳定性。
- Chat 模块已在路径节点页以内嵌 AI 伴学形式接入，并向后端传入 `pathId`、`nodeId`，后端会解析当前学习节点上下文；独立 Chat 页仍可保留为通用答疑入口。

教师使用者关心的是：能不能快速看班级问题、布置分层任务、跟踪学生变化。

教师侧优势：

- 已有班级学生列表、热力图、预警、周报、分层作业接口。
- 部分统计已经从真实学习记录、答题记录、掌握度历史中聚合。

教师侧问题：

- 分层作业题目仍有占位文本，必须改成真实题目生成或题库抽取。
- 热力图需要支持点击下钻：知识点 -> 薄弱学生 -> 推荐作业。
- 周报建议目前更像模板文本，需要显示数据来源和可执行建议。

使用者给分：72/100。  
理由：主流程成立，但“信任感”和“连续学习体验”还需要加强。

### 4. 专家视角：架构方向正确，但要补齐算法、数据和工程闭环

从专家角度看，SmartMentor 的核心价值不应是“调用大模型出题和聊天”，而是建立一个可解释的学习诊断系统：

```
学生行为数据
  -> 知识点掌握度估计
  -> 知识图谱根因追溯
  -> 个性化学习路径
  -> 针对性教学内容
  -> 练习与检查点评估
  -> 掌握度更新和再规划
```

当前架构的正确部分：

- 使用 Spring Boot 分层结构，Controller/Service/Repository/Entity 基本清晰。
- Agent 基类采用模板方法，统一 `buildPrompt -> callLLM -> parseResponse -> qualityCheck`，扩展方向正确。
- `DeepSeekService` 同时支持流式聊天和同步 JSON 输出，能支撑 Chat 与 Agent 两类场景。
- 轻量知识图谱用 JSON + 内存图结构实现，适合比赛项目，避免 Neo4j 部署复杂度。
- 诊断和溯源采用“本地快速返回 + 异步 AI 增强”，有利于用户体验。

专家会要求补强的部分：

- Agent 不能只“存在”，必须有可审计输入输出、质量检查、失败原因、fallback 策略和评估指标。
- 知识点掌握度不能只靠单次正确率，应统一使用 BKT/IRT 简化模型，并记录每次更新原因。
- 题目、答案、解析、错误类型必须持久化，不能依赖前端传正确答案或服务端默认答案。
- 业务核心链路需要集成测试，至少覆盖诊断开始、提交、完成、溯源、路径生成、课程生成、检查点评估。
- 生产配置要剥离所有本机密码、授权码和 API Key。

专家给分：74/100。  
理由：架构方向已经从普通 Web 系统升级到智能教学系统，但“可解释、可验证、可复现”仍需强化。

## 三、优先级排序

### P0：立刻处理，不处理会影响安全和交付

1. 迁移敏感配置  
   将 `application-dev.yml` 中的数据库密码、Redis 密码、邮箱授权码、DeepSeek API Key 迁移到环境变量或本地未提交配置；立即轮换已暴露的密钥。

2. 明确唯一构建入口  
   后端建议保留 Maven，删除或归档 Gradle 配置，统一使用：

   ```powershell
   cd D:\Idea\中国软件杯\SmartMentor
   .\mvnw.cmd test
   .\mvnw.cmd spring-boot:run
   ```

3. 清理前端交付边界  
   保留 `src/` 作为唯一源码入口；将旧版 `js/`、`css/` 标记为 legacy 或迁移完成后删除；`node_modules/`、`dist/` 不应作为源码交付重点。

4. 修复练习答案硬编码  
   `LearningService.findCorrectAnswer()` 当前默认返回 `"B"`，必须改为从 TeachingAgent 生成内容、题库表或课程快照中读取真实答案。

5. 修复教师作业占位题  
   `TeacherService.generateHomework()` 中“关于某知识点的练习题...”必须替换为真实题目来源。

### P1：比赛演示前必须完成

1. 固化一条可演示闭环  
   准备一个学生账号、一组知识点、一套稳定题目缓存或演示数据，保证能完整跑通诊断、溯源、路径、课程、练习、报告。

2. 增加关键路径测试  
   后端至少增加 Service 层测试和 Controller 集成测试；前端至少增加构建检查和核心页面冒烟脚本。

3. 增加 Agent 运行审计  
   每次 Agent 调用记录：agentName、inputHash、promptVersion、model、latency、success、fallbackUsed、qualityScore。

4. 优化教师端闭环  
   热力图点击知识点后能看到薄弱学生，并能一键生成分层作业。

5. 增强错误处理  
   大模型失败、Redis 失效、题目缓存过期时，用户应得到明确、可恢复的提示。

### P2：提升竞争力

1. 引入题库表与题目质量评估  
   建立 `question_bank`、`question_attempt`、`lesson_snapshot`，沉淀 AI 生成题目，避免每次都依赖实时生成。

2. 做掌握度模型统一  
   将诊断、练习、检查点全部写入同一套 `mastery_history`，统一更新 `student_profile.knowledge_state`。

3. 增加知识图谱可视化解释  
   前端展示“为什么从 A 知识点追溯到 B 根因”，提高专家评审信任感。

4. 做 Prompt 版本管理  
   每个 Agent 的 prompt 独立成模板，带版本号和示例输出。

5. 增加离线演示模式  
   在模型 API 不稳定时，使用预置的 Agent 响应样例完成现场演示。

## 四、详细优化方案

### 1. 安全与配置优化

目标：消除明文秘密，提高可部署性。

实施动作：

- 新建 `.env.example`，只放占位变量。
- 修改 `application.yml`，所有敏感项使用环境变量和默认空值。
- `application-dev.yml` 只保留本地非敏感默认项，或加入 `.gitignore`。
- 将已暴露的数据库、Redis、邮箱授权码、DeepSeek Key 全部轮换。
- 增加启动时配置校验：缺少关键环境变量时给出明确错误。

建议环境变量：

```powershell
$env:SMARTMENTOR_DB_PASSWORD="..."
$env:SMARTMENTOR_REDIS_PASSWORD="..."
$env:SMARTMENTOR_MAIL_PASSWORD="..."
$env:SMARTMENTOR_DEEPSEEK_API_KEY="..."
$env:SMARTMENTOR_JWT_SECRET="..."
```

验收标准：

- 代码和配置文件中不再出现真实密码、授权码、API Key。
- 新成员只按 README 配置环境变量即可启动。
- 密钥泄露风险在项目说明中有处理记录。

### 2. 工程结构优化

目标：让评审和队友可以一眼看懂如何启动、如何测试、如何演示。

后端建议结构：

```text
SmartMentor/
  src/main/java/com/tricia/smartmentor/
    agent/
    config/
    controller/
    service/
    repository/
    entity/
    dto/
  src/main/resources/
    knowledge-graph/
    prompts/
    application.yml
  src/test/java/
  pom.xml
  README.md
```

前端建议结构：

```text
smartmentor-web/
  src/
    api/
    assets/
    components/
    composables/
    router/
    views/
  package.json
  vite.config.js
  README.md
```

实施动作：

- 后端确定 Maven 为唯一构建入口，Gradle 文件移入 `legacy-build/` 或删除。
- 前端确定 Vue `src/` 为唯一入口，旧 `js/`、`css/` 若不用则移入 `legacy/`。
- 添加根级说明文档 `README.md`，包含启动顺序：MySQL、Redis、后端、前端。
- 将 `dist/` 作为构建产物，不作为源码评价重点。

验收标准：

- 任何人按 README 可在 15 分钟内启动项目。
- `npm run build` 和 `mvn test` 可稳定执行。
- 目录中不再同时出现两套活跃前端实现。

### 3. AI 与 Agent 链路优化

目标：让“多 Agent”从概念变成可证明的系统能力。

建议统一 Agent 输出协议：

```json
{
  "agentName": "DiagnosticAgent",
  "promptVersion": "diagnostic-analyze-v1",
  "success": true,
  "qualityScore": 0.86,
  "fallbackUsed": false,
  "data": {},
  "nextEvent": "DIAGNOSIS_COMPLETE",
  "explain": "基于答题记录识别出 3 个薄弱知识点"
}
```

实施动作：

- 为每个 Agent 增加 prompt 模板文件，放在 `src/main/resources/prompts/`。
- 增加 `AgentRunLog` 实体，记录调用耗时、模型、输入摘要、输出摘要、失败原因。
- `qualityCheck()` 不只检查字段存在，还要检查题目数量、知识点 ID 是否在图谱中、答案是否唯一、解析是否可渲染。
- 对 LLM 输出做二次规范化：知识点 ID 必须映射到知识图谱，错误类型必须映射到枚举。
- 对实时调用增加缓存：同一知识点、难度、题型可以复用高质量题目。

验收标准：

- 评审可以看到每一步由哪个 Agent 产生、为什么产生、是否使用 fallback。
- 模型失败时系统仍能用本地题库/图谱完成最低可用流程。
- Agent 生成内容可被后续业务真实消费，不停留在展示文本。

### 4. 诊断模块优化

目标：从“做 8 道题”升级为“可解释的自适应诊断”。

实施动作：

- 建立题目快照表，字段包括题干、选项、正确答案、解析、知识点、难度、错误类型、来源。
- `startDiagnostic()` 优先从高质量题库抽题，不足时调用 DiagnosticAgent 生成并入库。
- `submitAnswer()` 只根据服务端题目快照判分，不依赖前端传递正确答案。
- 题目选择策略从固定 8 题升级为：
  - 第 1-2 题：模块核心知识点中等难度
  - 答错：向前置知识点追溯
  - 答对：向高阶或综合应用推进
  - 信息增益不足时提前结束
- 结束后写入知识点掌握度历史，而不仅是诊断会话统计。

验收标准：

- 同一学生连续诊断，题目会根据历史掌握度变化。
- 每个薄弱点都有题目证据和错因解释。
- 诊断结果可以直接驱动溯源和学习路径。

### 5. 知识溯源优化

目标：让溯源结果有图谱证据和算法解释。

实施动作：

- 每个知识点 JSON 补齐：前置依赖、后继依赖、常见错误、题型、难度、考试权重。
- 溯源时输出每条路径的证据：
  - 起点薄弱知识点
  - 前置依赖链
  - 每个节点掌握度
  - 根因置信度
  - 为什么停止追溯
- 对跨模块追溯做显式标记，例如“导数应用题 -> 函数单调性 -> 一次/二次函数基础”。
- 前端图谱节点支持点击查看证据题目和错因。

验收标准：

- 评审能看懂“为什么系统认为根因是这个知识点”。
- 根因不是简单列出错题知识点，而是能沿图谱回溯。
- 学习路径顺序与图谱前置关系一致。

### 6. 学习路径与教学内容优化

目标：将路径、课程、练习、检查点变成连续学习产品。

已落地改造：

- 后端 `LearningService.generatePath()` 生成路径时设置首个可学习节点和 `currentNodeId`，路径列表/详情返回可直接展示的标题、模块和百分比进度。
- 后端 `LearningService.generateLesson()` 打开节点时会校验锁定状态，把可学习节点置为 `in_progress`，保存课程快照，并返回当前节点的 AI 伴学提示词。
- 前端 `Lesson.vue` 已改为节点任务流：学习讲解、AI 路径伴学、针对练习、节点检查点在同一页面内完成。
- AI 伴学调用 `/api/chat/stream` 时携带 `pathId` 和 `nodeId`，后端 `ChatService` 会从学习路径 JSON 中解析当前知识点、掌握度、教学策略和补救重点，再写入 system prompt。
- 检查点通过后更新掌握度、完成节点并解锁下一节点；检查点未通过时插入补救节点，并把 `currentNodeId` 指向补救节点。
- 学习路径详情页增加“继续当前节点”入口，学生能看到路径状态变化，而不是只看到静态路线图。

仍需继续补强：

- 路径节点应包含：
  - `knowledgePointId`
  - `currentMastery`
  - `targetMastery`
  - `estimatedMinutes`
  - `teachingStrategy`
  - `unlockRule`
  - `evidence`
- TeachingAgent 生成的课程内容要持久化为 `lesson_snapshot`，练习题正确答案随快照保存。
- 修复 `findCorrectAnswer()`，从 `lesson_snapshot.exercises` 或题库读取答案。
- 检查点未通过时，根据错因决定：
  - 重新学习当前节点
  - 插入补救节点
  - 回退到前置根因节点
- 路径完成或阶段性通过后，应提供“再诊断/再规划”按钮，直接进入下一轮诊断测试，而不是让学生手动回到诊断入口。
- 学习路径完成后写入报告和教师端周报数据。

验收标准：

- 学生完成练习后，节点掌握度真实变化。
- 检查点通过后自动解锁下一节点。
- 检查点失败后不是简单提示重学，而是给出具体补救动作。
- 节点通过后可以继续下一节点；路径完成后可以一键再次诊断并生成新路径，形成“测-学-测-再规划”循环。

### 7. 教师端优化

目标：从“看数据”升级为“看问题并采取行动”。

实施动作：

- 热力图支持三层下钻：
  - 班级模块热力图
  - 知识点薄弱学生列表
  - 学生错题和推荐干预
- 分层作业改为真实题目生成：
  - 基础层：概念和基础计算
  - 提高层：方法迁移和综合应用
  - 拓展层：高考真题/变式题
- 作业生成时保存作业快照，避免每次刷新内容变化。
- 周报增加“数据依据”，例如活跃人数、答题数、正确率、掌握度变化来自哪些表。

验收标准：

- 教师能从热力图一键生成分层作业。
- 生成作业不再出现占位文本。
- 周报建议能落到具体知识点和学生名单。

### 8. 前端体验优化

目标：减少演示风险，提高学生/教师使用信任感。

实施动作：

- 清理旧版前端入口，统一 Vue Router。
- API 请求增加非 JSON 响应保护，避免后端异常页导致 `res.json()` 报错不友好。
- 登录 token 当前存储在 `localStorage`，短期可接受，但要增加 XSS 风险说明；后续可改为 httpOnly Cookie。
- 诊断、溯源、课程生成页面增加：
  - 加载状态
  - AI 分析中提示
  - 失败重试
  - fallback 标识
- 数学公式统一使用 KaTeX 渲染，并处理渲染失败 fallback。
- 教师端增加下钻操作和生成作业确认弹窗。

验收标准：

- 前端构建无错误。
- 后端 401/403/500 都有清晰提示。
- AI 异步分析未完成时，用户能看到“分析中”，不会误以为无结果。

### 9. 测试与质量保障优化

目标：用自动化证明核心链路可用。

后端测试建议：

- `DiagnosticServiceTest`
  - 开始诊断生成题目
  - 提交答案判分
  - 完成诊断生成薄弱点
- `TracingServiceTest`
  - 从诊断结果生成根因
  - 图谱 fallback 可用
- `LearningServiceTest`
  - 从溯源结果生成路径
  - 课程快照保存
  - 练习答案从快照读取
  - 检查点通过解锁下一节点
- `SecurityIntegrationTest`
  - 未登录访问 `/api/**` 返回 401
  - 学生不能访问教师接口
  - 教师可以访问教师接口

前端测试建议：

- `npm run build`
- 使用 Playwright 或简单脚本做核心页面冒烟：
  - 登录页
  - 诊断页
  - 诊断结果页
  - 溯源页
  - 学习路径页
  - 教师端页

验收标准：

- 每次提交前至少执行后端测试和前端构建。
- 核心链路有自动化测试覆盖。
- 比赛演示数据可重复恢复。

## 五、实施方案

### 阶段 0：安全止血与工程整理，0.5-1 天

任务：

- 迁移所有敏感配置到环境变量。
- 轮换已暴露密钥。
- 明确 Maven 为后端唯一构建入口。
- 标记或移除前端 legacy 目录。
- 补充后端和前端 README。

交付物：

- `.env.example`
- 更新后的 `application.yml`
- 后端 README
- 前端 README
- 清理后的目录结构说明

验收：

```powershell
cd D:\Idea\中国软件杯\SmartMentor
.\mvnw.cmd test

cd D:\Idea\中国软件杯\smartmentor-web
npm run build
```

### 阶段 1：修复核心真实性问题，1-2 天

任务：

- 修复 `LearningService.findCorrectAnswer()`。
- TeachingAgent 生成课程时保存 lesson snapshot。
- 教师作业生成改为题库抽取或 Agent 生成。
- 诊断题目生成后入库或至少保存完整服务端快照。
- 前端不再承担正确答案来源。

交付物：

- `lesson_snapshot` 或等价实体
- `question_bank` 或题目快照机制
- 修复后的练习判分逻辑
- 真实分层作业接口

验收：

- 练习正确答案来自服务端持久化数据。
- 分层作业题干、答案、解析完整。
- 刷新页面后课程和练习仍保持一致。

### 阶段 2：强化诊断-溯源-路径闭环，2-3 天

任务：

- 已完成：路径节点状态机接入前端，学生可以从路径页进入当前节点。
- 已完成：节点页合并 TeachingAgent 内容、AI 路径伴学、练习提交、检查点提交。
- 已完成：检查点通过后解锁下一节点，失败后插入补救节点并把下一步指向补救节点。
- 已完成：AI 聊天绑定路径节点上下文，不再只是独立聊天。
- 待补强：路径完成后自动引导再次诊断，并基于新掌握度生成下一轮最优路径。
- 待补强：把路径完成、补救节点完成等事件同步写入报告和教师端周报。

交付物：

- 掌握度更新服务
- 溯源证据结构
- 路径节点状态机
- 检查点补救逻辑
- 路径节点内嵌 AI 伴学页面
- 测-学-测-再规划入口

验收：

- 一个学生从诊断到课程学习后，知识点掌握度发生可追踪变化。
- 溯源图可以解释每条边。
- 路径进度和报告数据一致。

### 阶段 3：Agent 可观测与质量控制，1-2 天

任务：

- 增加 AgentRunLog。
- Prompt 模板外置并版本化。
- 每个 Agent 增加质量评分。
- 大模型失败时展示 fallback 状态。
- 增加演示模式样例响应。

交付物：

- `agent_run_log` 表
- `resources/prompts/`
- Agent 质量检查规则
- 演示数据和演示脚本

验收：

- 每次诊断/溯源/路径/教学/评估都能查看 Agent 调用记录。
- 模型不可用时，演示链路仍可跑通。

### 阶段 4：前端与教师端打磨，2 天

任务：

- 诊断页增加 AI 分析中和重试状态。
- 溯源图支持节点解释。
- 学习路径页展示解锁规则和掌握度变化。
- 教师热力图支持下钻和一键作业。
- 周报显示数据依据和建议动作。

交付物：

- 学生端闭环演示页面
- 教师端班级干预闭环
- 完整 UI 冒烟测试

验收：

- 学生端 5 分钟内可演示完整学习闭环。
- 教师端 3 分钟内可演示从热力图到分层作业。
- 页面无明显空白、错位、无响应状态。

### 阶段 5：测试、文档与答辩材料，1-2 天

任务：

- 增加核心测试。
- 准备演示数据恢复脚本。
- 编写答辩话术和技术亮点页。
- 准备风险说明：AI 失败、数据隐私、题目质量、安全配置。

交付物：

- 测试报告
- 演示脚本
- 技术架构图
- 核心流程图
- 风险与应对说明

验收：

- 项目能稳定启动。
- 演示账号数据固定。
- 答辩时能解释“为什么不是普通 ChatGPT 套壳”。

## 六、建议里程碑

| 时间 | 目标 | 核心验收 |
|---|---|---|
| 第 1 天 | 安全配置和工程整理 | 无明文密钥；README 可启动 |
| 第 2-3 天 | 修复真实判分和真实作业 | 练习/作业无硬编码答案和占位题 |
| 第 4-5 天 | 闭环强化 | 诊断、溯源、路径、课程、评估数据贯通 |
| 第 6 天 | Agent 可观测 | 可查看 Agent 调用记录和 fallback |
| 第 7 天 | 前端演示打磨 | 学生端和教师端可稳定演示 |
| 第 8 天 | 测试和答辩材料 | 测试通过；演示脚本完整 |

## 七、比赛展示建议

建议不要按功能菜单逐个点，而是按一个学生故事演示。

推荐演示脚本：

1. 学生登录，进入仪表盘，看到近期掌握度和待完成任务。
2. 学生选择“导数”模块开始诊断。
3. 系统生成自适应诊断题，学生故意答错 2-3 道与前置概念相关的题。
4. 诊断完成，系统展示薄弱知识点和错因。
5. 点击“知识溯源”，展示从薄弱点回溯到根因知识点的图谱路径。
6. 点击“生成学习路径”，系统按根因到目标知识点生成节点。
7. 进入第一个学习节点，页面展示个性化讲解、例题、针对练习和右侧 AI 路径伴学。
8. 在节点内向 AI 提问，AI 围绕当前 `pathId + nodeId` 对应知识点给提示和小例题。
9. 完成针对练习后进入节点检查点。
10. 检查点通过时，路径详情页显示下一节点已解锁；检查点未通过时，系统插入补救节点并给出补救动作。
11. 完成一轮路径后再次进入诊断，展示“测-学-测-再规划”的循环。
12. 切换教师端，查看班级热力图，发现同类薄弱点。
13. 教师一键生成分层作业，并查看周报建议。

答辩时强调三句话：

- 我们不是只做 AI 聊天，而是把大模型放入诊断、溯源、规划、教学、评估五个 Agent 中。
- 我们不是只给学生推荐课程，而是基于知识图谱追溯根因，再生成可解释学习路径。
- 我们不是只展示结果，而是持续记录答题、掌握度、路径进度和教师端干预数据，形成闭环。

## 八、风险清单与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 模型 API 不稳定 | 诊断/教学生成失败 | 题库缓存 + 演示模式 + fallback |
| 密钥泄露 | 安全和评审风险 | 立即轮换 + 环境变量 |
| 题目质量不稳定 | 学生体验差 | 质量评分 + 人工精选题库 |
| 数据链路断裂 | 闭环不可信 | 统一掌握度更新服务 |
| 前端旧代码干扰 | 维护困难 | 清理 legacy 入口 |
| 测试不足 | 演示易翻车 | 增加核心链路自动化测试 |

## 九、最终建议

SmartMentor 当前最值得保留和强化的核心定位是：

> 面向高中数学的多 Agent 个性化学习诊断与知识溯源系统。

接下来不要继续横向堆功能，而应集中资源把一条主链路做到非常稳：

```
诊断题生成
  -> 答题分析
  -> 薄弱点识别
  -> 知识图谱溯源
  -> 个性化路径
  -> 课程与练习
  -> 掌握度更新
  -> 教师端干预
```

如果能在安全配置、真实判分、真实作业、Agent 可观测、核心测试这五件事上完成优化，项目的比赛表现会明显提升：从“功能很多的 AI 学习平台”升级为“有技术闭环、有解释能力、有工程质量的智能辅导系统”。



<!-- ============================================================ -->
# 【SmartMentor-接口文档合集】
<!-- 源文件：SmartMentor-接口文档合集.md -->

# SmartMentor 接口文档合集

> 本文档由 SmartMentor/docs 目录下的接口文档合并生成。

## 2026-06-02 当前项目接口更新摘要

本节为当前代码状态的合并修订，覆盖 `SmartMentor` 后端与 `smartmentor-web` 前端最新联调结果。

- 认证采用 JWT，登录/注册均区分 `student` 与 `teacher` 角色。
- 学生端接口仅允许 `STUDENT`：`/api/diagnostic/**`、`/api/learning/**`、`/api/tracing/**`、`/api/report/**`、`/api/engagement/**`、`/api/homework/**`。
- 教师端接口仅允许 `TEACHER`：`/api/teacher/**`。
- 教师端已接入班级学生、知识热力图、知识点下钻、AI 分层作业、作业历史、学生预警、班级周报。
- 学生端已接入学习路径节点闭环：课程内容、练习提交、检查点评估、补救节点、下一节点跳转。
- 新增学生领取老师作业接口：`GET /api/homework`、`GET /api/homework/{homeworkId}`。
- 教师生成作业保存到 `teacher_homework`，学生端根据 `class_student` 班级归属与 `student_assignments` 分配信息读取自己的作业层级。

### 当前权限矩阵

| 角色 | 可访问页面 | 可访问 API |
|---|---|---|
| 学生 | 仪表盘、诊断测试、学习路径、我的作业、AI 对话、学习报告、个人设置 | `/api/diagnostic/**`、`/api/tracing/**`、`/api/learning/**`、`/api/homework/**`、`/api/report/**`、`/api/engagement/**`、通用 `/api/chat/**`、`/api/profile/**` |
| 教师 | 教师面板、AI 对话、个人设置 | `/api/teacher/**`、通用 `/api/chat/**`、`/api/profile/**` |

### 学生端 - 老师布置作业接口

#### 获取我的作业列表

- **路径**：`GET /api/homework`
- **认证**：学生 JWT
- **说明**：获取当前学生所在班级中、老师已经发布且分配给当前学生的作业。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | Integer | 否 | 页码，默认 `1` |
| pageSize | Integer | 否 | 每页数量，默认 `10` |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "homeworks": [
      {
        "homeworkId": "hw_20260602_001",
        "className": "高一1班",
        "knowledgePointIds": ["derivative-definition"],
        "knowledgePoints": ["导数的定义"],
        "assignedLayers": ["基础层"],
        "questionCount": 5,
        "status": "published",
        "deadline": "2026-06-05T23:59:59",
        "note": "重点完成导数概念题",
        "createdAt": "2026-06-02T10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 获取我的作业详情

- **路径**：`GET /api/homework/{homeworkId}`
- **认证**：学生 JWT
- **说明**：返回当前学生被分配到的层级和题目，不返回其他学生分配名单。

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "homeworkId": "hw_20260602_001",
    "className": "高一1班",
    "knowledgePoints": ["导数的定义"],
    "assignedLayers": ["基础层"],
    "questionCount": 5,
    "deadline": "2026-06-05T23:59:59",
    "layers": [
      {
        "layer": "基础层",
        "description": "概念巩固，注重基础计算和定义理解",
        "questions": [
          {
            "questionId": 501,
            "content": "求函数 f(x)=x^2 在 x=1 处的导数。",
            "questionType": "计算题",
            "difficulty": 0.3
          }
        ]
      }
    ]
  }
}
```

## 合并范围

- api-auth-profile.md
- api-diagnostic-tracing.md
- api-learning-chat.md
- api-teacher-report.md

---

<!-- Source: api-auth-profile.md -->

# SmartMentor 认证与学生画像模块 API 接口文档

## 概述

本文档定义了 SmartMentor 系统中认证（Auth）和学生画像（Profile）模块的 RESTful API 接口规范。

- **基础路径**：`/api`
- **认证方式**：JWT（JSON Web Token）
- **Token 传递**：通过请求头 `Authorization: Bearer {token}` 传递
- **内容类型**：`Content-Type: application/json`

---

## 统一响应格式

所有接口均采用以下统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段      | 类型            | 说明                      |
|---------|---------------|-------------------------|
| code    | Integer       | 状态码，200 表示成功，非 200 表示异常 |
| message | String        | 响应描述信息                  |
| data    | Object / null | 响应数据，失败时可为 null         |

### 通用错误码

| 状态码 | 说明               |
|-----|------------------|
| 200 | 请求成功             |
| 400 | 请求参数错误           |
| 401 | 未认证（Token 缺失或无效） |
| 403 | 权限不足             |
| 404 | 资源不存在            |
| 409 | 资源冲突（如用户名已存在）    |
| 500 | 服务器内部错误          |

---

## 一、认证模块（Auth）

### 1.1 学生注册

学生通过用户名和密码进行注册，注册成功后自动创建对应的学生画像记录。

- **路径**：`POST /api/auth/register`
- **认证**：无需认证
- **说明**：学生注册接口，注册成功后返回 JWT Token，可直接进入系统

#### 请求参数

| 参数       | 类型     | 必填 | 说明                        |
|----------|--------|----|---------------------------|
| username | String | 是  | 用户名，3-50 个字符，唯一           |
| password | String | 是  | 密码，6-128 个字符              |
| nickname | String | 否  | 昵称，1-50 个字符               |
| grade    | String | 是  | 年级，可选值：`高一` / `高二` / `高三` |
| school   | String | 否  | 学校名称，最大 100 个字符           |

#### 请求体示例

```json
{
  "username": "zhangsan2024",
  "password": "Abc123456",
  "nickname": "张三",
  "grade": "高二",
  "school": "北京市第一中学"
}
```

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1001,
    "username": "zhangsan2024",
    "nickname": "张三",
    "role": "student",
    "grade": "高二",
    "school": "北京市第一中学"
  }
}
```

**失败 - 用户名已存在（409）**：

```json
{
  "code": 409,
  "message": "用户名已存在",
  "data": null
}
```

**失败 - 参数校验失败（400）**：

```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": {
    "errors": [
      { "field": "username", "message": "用户名长度应为3-50个字符" },
      { "field": "password", "message": "密码长度不能少于6个字符" }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                     |
|-----|------------------------|
| 200 | 注册成功，返回 Token 和用户基本信息  |
| 400 | 请求参数校验失败（缺少必填字段或格式不正确） |
| 409 | 用户名已被占用                |
| 500 | 服务器内部错误                |

---

### 1.2 登录

学生和教师共用登录接口，通过 `role` 字段区分身份。登录成功返回 JWT Token。

- **路径**：`POST /api/auth/login`
- **认证**：无需认证
- **说明**：统一登录接口，支持学生和教师两种角色

#### 请求参数

| 参数       | 类型     | 必填 | 说明                           |
|----------|--------|----|------------------------------|
| username | String | 是  | 用户名                          |
| password | String | 是  | 密码                           |
| role     | String | 是  | 角色，可选值：`student` / `teacher` |

#### 请求体示例

```json
{
  "username": "zhangsan2024",
  "password": "Abc123456",
  "role": "student"
}
```

#### 响应体示例

**成功 - 学生登录（200）**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1001,
    "username": "zhangsan2024",
    "nickname": "张三",
    "role": "student",
    "grade": "高二",
    "school": "北京市第一中学",
    "avatarUrl": null
  }
}
```

**成功 - 教师登录（200）**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 2001,
    "username": "teacher_wang",
    "nickname": "王老师",
    "role": "teacher",
    "school": "北京市第一中学"
  }
}
```

**失败 - 用户名或密码错误（401）**：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

**失败 - 角色不匹配（400）**：

```json
{
  "code": 400,
  "message": "该用户不属于所选角色",
  "data": null
}
```

#### 状态码说明

| 状态码 | 说明                  |
|-----|---------------------|
| 200 | 登录成功，返回 Token 和用户信息 |
| 400 | 请求参数错误或角色不匹配        |
| 401 | 用户名或密码错误            |
| 500 | 服务器内部错误             |

---

### 1.3 获取当前用户信息

通过 JWT Token 获取当前登录用户的详细信息。

- **路径**：`GET /api/auth/me`
- **认证**：需要认证（Bearer Token）
- **说明**：返回当前已认证用户的完整信息，包含角色相关的扩展字段

#### 请求参数

无请求体。通过请求头传递 Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 请求体示例

无（GET 请求无请求体）

#### 响应体示例

**成功 - 学生用户（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "username": "zhangsan2024",
    "nickname": "张三",
    "role": "student",
    "grade": "高二",
    "school": "北京市第一中学",
    "avatarUrl": "https://cdn.smartmentor.com/avatars/1001.png",
    "createdAt": "2025-09-01T08:30:00",
    "profile": {
      "overallMastery": 0.45,
      "level": 3,
      "experiencePoints": 1250,
      "streakDays": 7,
      "totalStudyHours": 28.5,
      "lastDiagnosticAt": "2025-10-15T14:30:00"
    }
  }
}
```

**成功 - 教师用户（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 2001,
    "username": "teacher_wang",
    "nickname": "王老师",
    "role": "teacher",
    "school": "北京市第一中学",
    "createdAt": "2025-08-15T10:00:00",
    "classCount": 2,
    "studentCount": 45
  }
}
```

**失败 - Token 无效或过期（401）**：

```json
{
  "code": 401,
  "message": "Token 无效或已过期，请重新登录",
  "data": null
}
```

#### 状态码说明

| 状态码 | 说明              |
|-----|-----------------|
| 200 | 获取成功，返回用户详细信息   |
| 401 | Token 无效、过期或未提供 |
| 500 | 服务器内部错误         |

---

## 二、学生画像模块（Profile）

> 以下接口均需要认证（Bearer Token），且仅限学生角色访问。

### 2.1 获取学生五维画像总览

获取当前学生的五维画像综合数据，包括知识状态、错误模式、学习行为、认知风格和目标画像。

- **路径**：`GET /api/profile/overview`
- **认证**：需要认证（Bearer Token，学生角色）
- **说明**：返回学生五维画像的完整数据，用于前端仪表盘展示（雷达图等）

#### 请求参数

无请求体。通过请求头传递 Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 请求体示例

无（GET 请求无请求体）

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "overallMastery": 0.45,
    "level": 3,
    "experiencePoints": 1250,
    "streakDays": 7,
    "totalStudyHours": 28.5,
    "dimensions": {
      "knowledgeState": {
        "label": "知识状态",
        "score": 0.45,
        "description": "基于 BKT 模型的知识点掌握概率",
        "moduleMastery": {
          "函数": 0.62,
          "导数": 0.35,
          "三角函数": 0.48,
          "向量": 0.55,
          "数列": 0.60,
          "解析几何": 0.30
        },
        "totalKnowledgePoints": 95,
        "masteredCount": 38,
        "learningCount": 32,
        "weakCount": 25
      },
      "errorPatterns": {
        "label": "错误模式",
        "score": 0.40,
        "description": "常犯错误类型的分布与频率",
        "patterns": [
          { "type": "计算性错误", "subType": "符号错误", "frequency": 0.35, "trend": "decreasing" },
          { "type": "概念性错误", "subType": "概念混淆", "frequency": 0.25, "trend": "stable" },
          { "type": "逻辑性错误", "subType": "分类讨论遗漏", "frequency": 0.20, "trend": "increasing" },
          { "type": "方法性错误", "subType": "方法选择不当", "frequency": 0.12, "trend": "stable" },
          { "type": "审题性错误", "subType": "条件遗漏", "frequency": 0.08, "trend": "decreasing" }
        ],
        "totalErrors": 156,
        "recentErrorRate": 0.42
      },
      "learningBehavior": {
        "label": "学习行为",
        "score": 0.65,
        "description": "学习时段偏好、专注时长与做题速度",
        "preferredTimeSlot": "evening",
        "avgDailyStudyMinutes": 45,
        "avgFocusDuration": 25,
        "avgQuestionTime": 180,
        "weeklyActiveDays": 5,
        "studyPattern": {
          "morning": 0.10,
          "afternoon": 0.25,
          "evening": 0.65
        }
      },
      "cognitiveStyle": {
        "label": "认知风格",
        "score": 0.70,
        "description": "偏好的学习内容呈现方式",
        "primaryStyle": "visual",
        "styleDistribution": {
          "visual": 0.45,
          "logical": 0.25,
          "example": 0.20,
          "formula": 0.10
        },
        "preferredContentTypes": ["图表讲解", "动态演示", "实例分析"]
      },
      "goalProfile": {
        "label": "目标画像",
        "score": 0.50,
        "description": "目标院校与薄弱模块优先级设置",
        "targetSchool": "浙江大学",
        "targetScore": 130,
        "weakModulePriority": ["导数", "解析几何", "三角函数"],
        "examCountdown": 180,
        "studyMode": "systematic"
      }
    },
    "lastUpdatedAt": "2025-10-15T16:30:00"
  }
}
```

**失败 - 画像数据不存在（404）**：

```json
{
  "code": 404,
  "message": "学生画像数据不存在，请先完成诊断测试",
  "data": null
}
```

#### 状态码说明

| 状态码 | 说明                  |
|-----|---------------------|
| 200 | 获取成功，返回五维画像完整数据     |
| 401 | 未认证或 Token 无效       |
| 403 | 非学生角色无权访问           |
| 404 | 画像数据不存在（新用户未完成首次诊断） |
| 500 | 服务器内部错误             |

---

### 2.2 更新学生设置

更新学生的目标画像和学习偏好设置，包括目标学校、学习风格、学习时间等。

- **路径**：`PUT /api/profile/settings`
- **认证**：需要认证（Bearer Token，学生角色）
- **说明**：更新学生个性化设置，支持部分更新（只传需要修改的字段）

#### 请求参数

| 参数                 | 类型              | 必填 | 说明                                                      |
|--------------------|-----------------|----|---------------------------------------------------------|
| targetSchool       | String          | 否  | 目标学校名称，最大 100 个字符                                       |
| targetScore        | Integer         | 否  | 目标数学分数（满分 150）                                          |
| learningStyle      | String          | 否  | 学习风格偏好，可选值：`visual` / `logical` / `example` / `formula` |
| dailyStudyMinutes  | Integer         | 否  | 每日计划学习时长（分钟），范围 15-240                                  |
| preferredTimeSlot  | String          | 否  | 偏好学习时段，可选值：`morning` / `afternoon` / `evening`          |
| weakModulePriority | Array\<String\> | 否  | 薄弱模块优先级列表，如 `["导数", "解析几何"]`                            |
| studyMode          | String          | 否  | 学习模式，可选值：`systematic`（系统提升） / `urgent`（考前突击）            |
| nickname           | String          | 否  | 修改昵称，1-50 个字符                                           |
| avatarUrl          | String          | 否  | 头像 URL                                                  |

#### 请求体示例

```json
{
  "targetSchool": "浙江大学",
  "targetScore": 135,
  "learningStyle": "visual",
  "dailyStudyMinutes": 60,
  "preferredTimeSlot": "evening",
  "weakModulePriority": ["导数", "解析几何", "三角函数"],
  "studyMode": "systematic"
}
```

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "设置更新成功",
  "data": {
    "studentId": 1001,
    "targetSchool": "浙江大学",
    "targetScore": 135,
    "learningStyle": "visual",
    "dailyStudyMinutes": 60,
    "preferredTimeSlot": "evening",
    "weakModulePriority": ["导数", "解析几何", "三角函数"],
    "studyMode": "systematic",
    "updatedAt": "2025-10-15T17:00:00"
  }
}
```

**失败 - 参数校验失败（400）**：

```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": {
    "errors": [
      { "field": "dailyStudyMinutes", "message": "每日学习时长应在15-240分钟之间" },
      { "field": "learningStyle", "message": "学习风格仅支持 visual/logical/example/formula" }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                |
|-----|-------------------|
| 200 | 更新成功，返回更新后的完整设置信息 |
| 400 | 请求参数校验失败          |
| 401 | 未认证或 Token 无效     |
| 403 | 非学生角色无权访问         |
| 500 | 服务器内部错误           |

---

### 2.3 获取知识掌握度图谱数据

获取学生在各知识点上的掌握度数据，用于前端渲染交互式知识图谱（D3.js 力导向图）。

- **路径**：`GET /api/profile/knowledge-map`
- **认证**：需要认证（Bearer Token，学生角色）
- **说明**：返回知识点节点列表及其掌握度、节点间的前置依赖关系边，支持按模块筛选

#### 请求参数（Query Parameters）

| 参数     | 类型      | 必填 | 说明                                                              |
|--------|---------|----|-----------------------------------------------------------------|
| module | String  | 否  | 按模块筛选，可选值：`函数` / `导数` / `三角函数` / `向量` / `数列` / `解析几何`；不传则返回全部模块 |
| depth  | Integer | 否  | 图谱展示深度，默认 3，范围 1-5                                              |

请求示例：

```
GET /api/profile/knowledge-map?module=导数&depth=3
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 请求体示例

无（GET 请求无请求体）

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "module": "导数",
    "totalNodes": 25,
    "masteredNodes": 8,
    "learningNodes": 10,
    "weakNodes": 7,
    "nodes": [
      {
        "id": "math_derivative_application",
        "name": "导数的应用",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.70,
        "importance": 0.90,
        "gaokaoFrequency": 0.85,
        "mastery": 0.35,
        "masteryLevel": "weak",
        "status": "learning",
        "lastPracticeAt": "2025-10-14T20:00:00",
        "errorCount": 12,
        "practiceCount": 18
      },
      {
        "id": "math_derivative_calc",
        "name": "导数的计算",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.50,
        "importance": 0.85,
        "gaokaoFrequency": 0.90,
        "mastery": 0.42,
        "masteryLevel": "weak",
        "status": "learning",
        "lastPracticeAt": "2025-10-13T19:30:00",
        "errorCount": 8,
        "practiceCount": 15
      },
      {
        "id": "math_derivative_concept",
        "name": "导数的概念与几何意义",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.45,
        "importance": 0.80,
        "gaokaoFrequency": 0.60,
        "mastery": 0.58,
        "masteryLevel": "learning",
        "status": "learning",
        "lastPracticeAt": "2025-10-12T21:00:00",
        "errorCount": 5,
        "practiceCount": 12
      },
      {
        "id": "math_rate_of_change",
        "name": "平均变化率与瞬时变化率",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.35,
        "importance": 0.70,
        "gaokaoFrequency": 0.40,
        "mastery": 0.25,
        "masteryLevel": "weak",
        "status": "root_cause",
        "lastPracticeAt": "2025-10-10T18:00:00",
        "errorCount": 6,
        "practiceCount": 8
      },
      {
        "id": "math_function_concept",
        "name": "函数的概念与表示法",
        "module": "函数",
        "chapter": "第二章",
        "difficulty": 0.30,
        "importance": 0.90,
        "gaokaoFrequency": 0.70,
        "mastery": 0.82,
        "masteryLevel": "mastered",
        "status": "completed",
        "lastPracticeAt": "2025-09-20T20:00:00",
        "errorCount": 2,
        "practiceCount": 20
      }
    ],
    "edges": [
      {
        "source": "math_function_concept",
        "target": "math_rate_of_change",
        "type": "prerequisite",
        "weight": 0.9,
        "crossModule": false
      },
      {
        "source": "math_rate_of_change",
        "target": "math_derivative_concept",
        "type": "prerequisite",
        "weight": 0.95,
        "crossModule": false
      },
      {
        "source": "math_derivative_concept",
        "target": "math_derivative_calc",
        "type": "prerequisite",
        "weight": 0.9,
        "crossModule": false
      },
      {
        "source": "math_derivative_calc",
        "target": "math_derivative_application",
        "type": "prerequisite",
        "weight": 0.9,
        "crossModule": false
      }
    ],
    "legend": {
      "masteryLevels": {
        "mastered": { "label": "已掌握", "color": "#52c41a", "threshold": "≥ 0.8" },
        "learning": { "label": "学习中", "color": "#faad14", "threshold": "0.5 - 0.8" },
        "weak": { "label": "薄弱", "color": "#ff4d4f", "threshold": "< 0.5" }
      }
    }
  }
}
```

**成功 - 无数据（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "module": null,
    "totalNodes": 0,
    "masteredNodes": 0,
    "learningNodes": 0,
    "weakNodes": 0,
    "nodes": [],
    "edges": [],
    "legend": {
      "masteryLevels": {
        "mastered": { "label": "已掌握", "color": "#52c41a", "threshold": "≥ 0.8" },
        "learning": { "label": "学习中", "color": "#faad14", "threshold": "0.5 - 0.8" },
        "weak": { "label": "薄弱", "color": "#ff4d4f", "threshold": "< 0.5" }
      }
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明                    |
|-----|-----------------------|
| 200 | 获取成功，返回知识图谱节点和边数据     |
| 400 | 请求参数错误（如 module 值不合法） |
| 401 | 未认证或 Token 无效         |
| 403 | 非学生角色无权访问             |
| 500 | 服务器内部错误               |

---

## 三、数据模型参考

### 3.1 数据库表结构

以下为接口涉及的核心数据库表：

#### student 表（学生基本信息）

| 字段         | 类型           | 说明              |
|------------|--------------|-----------------|
| id         | BIGINT       | 主键，自增           |
| username   | VARCHAR(50)  | 用户名，唯一          |
| password   | VARCHAR(128) | 密码（BCrypt 加密存储） |
| nickname   | VARCHAR(50)  | 昵称              |
| grade      | VARCHAR(20)  | 年级：高一/高二/高三     |
| school     | VARCHAR(100) | 学校名称            |
| avatar_url | VARCHAR(255) | 头像 URL          |
| created_at | DATETIME     | 创建时间            |

#### student_profile 表（五维学生画像）

| 字段                   | 类型            | 说明                                  |
|----------------------|---------------|-------------------------------------|
| id                   | BIGINT        | 主键，自增                               |
| student_id           | BIGINT        | 关联学生 ID                             |
| learning_style       | VARCHAR(20)   | 认知风格：visual/logical/example/formula |
| daily_study_minutes  | INT           | 每日学习时长（分钟）                          |
| preferred_time_slot  | VARCHAR(20)   | 偏好时段：morning/afternoon/evening      |
| target_school        | VARCHAR(100)  | 目标学校                                |
| weak_module_priority | JSON          | 薄弱模块优先级列表                           |
| overall_mastery      | DECIMAL(3,2)  | 总体掌握度 0.00-1.00                     |
| ability_param        | DECIMAL(5,2)  | IRT 能力参数                            |
| error_patterns       | JSON          | 错误模式统计                              |
| knowledge_state      | JSON          | 各知识点掌握度 {kpId: mastery}             |
| streak_days          | INT           | 连续学习天数                              |
| total_study_hours    | DECIMAL(10,1) | 累计学习时长（小时）                          |
| level                | INT           | 等级                                  |
| experience_points    | INT           | 经验值                                 |
| last_diagnostic_at   | DATETIME      | 最近一次诊断时间                            |
| updated_at           | DATETIME      | 最后更新时间                              |

#### teacher 表（教师信息）

| 字段         | 类型           | 说明              |
|------------|--------------|-----------------|
| id         | BIGINT       | 主键，自增           |
| username   | VARCHAR(50)  | 用户名，唯一          |
| password   | VARCHAR(128) | 密码（BCrypt 加密存储） |
| name       | VARCHAR(50)  | 教师姓名            |
| school     | VARCHAR(100) | 学校名称            |
| created_at | DATETIME     | 创建时间            |

### 3.2 五维画像维度说明

| 维度       | 字段来源                                                                  | 数据采集方式          |
|----------|-----------------------------------------------------------------------|-----------------|
| **知识状态** | knowledge_state (JSON)、overall_mastery                                | 作答记录 + BKT 模型计算 |
| **错误模式** | error_patterns (JSON)                                                 | LLM 错因分析 + 分类统计 |
| **学习行为** | daily_study_minutes、preferred_time_slot、streak_days、total_study_hours | 行为日志自动采集        |
| **认知风格** | learning_style                                                        | 交互反馈 + 学生设置     |
| **目标画像** | target_school、weak_module_priority                                    | 学生手动设置          |

---

## 四、JWT Token 说明

### 4.1 Token 结构

JWT Token 的 Payload 中包含以下信息：

```json
{
  "sub": "1001",
  "username": "zhangsan2024",
  "role": "student",
  "iat": 1697356800,
  "exp": 1697443200
}
```

| 字段       | 说明                   |
|----------|----------------------|
| sub      | 用户 ID（字符串形式）         |
| username | 用户名                  |
| role     | 角色：student / teacher |
| iat      | Token 签发时间（Unix 时间戳） |
| exp      | Token 过期时间（Unix 时间戳） |

### 4.2 Token 有效期

- **Access Token**：24 小时
- **Token 刷新**：Token 过期后需重新登录获取新 Token

### 4.3 认证失败统一响应

当 Token 无效或过期时，所有需要认证的接口返回：

```json
{
  "code": 401,
  "message": "Token 无效或已过期，请重新登录",
  "data": null
}
```

---

## 五、接口调用示例

### 5.1 完整调用流程

```
1. 注册 → POST /api/auth/register → 获取 token
2. 登录 → POST /api/auth/login → 获取 token
3. 获取用户信息 → GET /api/auth/me (带 Authorization 头)
4. 获取画像总览 → GET /api/profile/overview (带 Authorization 头)
5. 更新设置 → PUT /api/profile/settings (带 Authorization 头)
6. 获取知识图谱 → GET /api/profile/knowledge-map?module=导数 (带 Authorization 头)
```

### 5.2 cURL 示例

**注册**：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan2024",
    "password": "Abc123456",
    "nickname": "张三",
    "grade": "高二",
    "school": "北京市第一中学"
  }'
```

**登录**：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan2024",
    "password": "Abc123456",
    "role": "student"
  }'
```

**获取当前用户信息**：

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**获取五维画像总览**：

```bash
curl -X GET http://localhost:8080/api/profile/overview \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**更新学生设置**：

```bash
curl -X PUT http://localhost:8080/api/profile/settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "targetSchool": "浙江大学",
    "dailyStudyMinutes": 60,
    "weakModulePriority": ["导数", "解析几何"]
  }'
```

**获取知识掌握度图谱（按模块筛选）**：

```bash
curl -X GET "http://localhost:8080/api/profile/knowledge-map?module=导数&depth=3" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

<!-- Source: api-diagnostic-tracing.md -->

# SmartMentor 智能诊断与溯源分析 接口文档

> 版本：v1.0
> 基础路径：`/api`
> 认证方式：JWT（所有接口均需在请求头携带 `Authorization: Bearer {token}`）
> 统一响应格式：`{ "code": int, "message": string, "data": object | null }`

---

## 目录

1. [POST /api/diagnostic/start — 启动诊断测试](#1-启动诊断测试)
2. [POST /api/diagnostic/submit — 提交答案](#2-提交答案)
3. [POST /api/diagnostic/finish — 结束诊断](#3-结束诊断)
4. [GET /api/diagnostic/history — 历史诊断记录列表](#4-历史诊断记录列表)
5. [GET /api/diagnostic/result/{diagnosticId} — 诊断详细结果](#5-诊断详细结果)
6. [POST /api/tracing/analyze — 溯源分析](#6-溯源分析)
7. [GET /api/tracing/result/{tracingId} — 溯源结果详情](#7-溯源结果详情)

---

## 公共说明

### 认证

所有接口均需要 JWT 认证，请在 HTTP 请求头中携带：

```
Authorization: Bearer {token}
```

Token 通过 `/api/auth/login` 接口获取，此处不再赘述。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段      | 类型     | 说明                         |
|---------|--------|----------------------------|
| code    | int    | 业务状态码，200 表示成功，其余见各接口状态码说明 |
| message | string | 状态描述信息                     |
| data    | object | 响应数据，失败时可能为 `null`         |

### 通用状态码

| 状态码 | 说明             |
|-----|----------------|
| 200 | 请求成功           |
| 400 | 请求参数错误         |
| 401 | 未认证或 Token 已过期 |
| 403 | 无权访问该资源        |
| 404 | 资源不存在          |
| 500 | 服务器内部错误        |

---

## 1. 启动诊断测试

启动一次新的自适应诊断测试。学生选择一个数学模块（如"导数""函数""三角函数"等），系统据此从知识图谱中选取关键知识点，生成第一道诊断题目。通常
5-8 道题即可完成一个模块的诊断。

- **路径**：`/api/diagnostic/start`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名        | 类型     | 必填 | 说明                                                                        |
|------------|--------|----|---------------------------------------------------------------------------|
| module     | string | 是  | 数学模块名称，可选值：`函数`、`导数`、`三角函数`、`向量`、`数列`、`解析几何`                              |
| difficulty | string | 否  | 初始难度偏好，可选值：`easy`(基础)、`medium`(中等，默认)、`hard`(较难)。系统会根据学生画像自动调整，此参数仅作为初始参考 |

### 请求体示例

```json
{
  "module": "导数",
  "difficulty": "medium"
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "module": "导数",
    "totalEstimatedQuestions": 6,
    "currentQuestionIndex": 1,
    "question": {
      "questionId": 1001,
      "knowledgePointId": "math_derivative_concept",
      "knowledgePointName": "导数的概念与几何意义",
      "type": "choice",
      "difficulty": 0.50,
      "content": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1)$ 的几何意义是（　　）",
      "options": [
        { "label": "A", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的切线斜率" },
        { "label": "B", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的纵坐标" },
        { "label": "C", "text": "函数 $f(x) = x^2$ 在 $x = 1$ 处的函数值" },
        { "label": "D", "text": "曲线 $y = x^2$ 过原点的割线斜率" }
      ],
      "timeLimit": 120
    }
  }
}
```

### 响应字段说明

| 字段                               | 类型     | 说明                                            |
|----------------------------------|--------|-----------------------------------------------|
| data.diagnosticId                | string | 本次诊断会话的唯一标识                                   |
| data.module                      | string | 诊断的数学模块                                       |
| data.totalEstimatedQuestions     | int    | 预计总题数（5-8 题，实际可能因自适应调整而变化）                    |
| data.currentQuestionIndex        | int    | 当前题目序号（从 1 开始）                                |
| data.question.questionId         | long   | 题目 ID                                         |
| data.question.knowledgePointId   | string | 所属知识点 ID                                      |
| data.question.knowledgePointName | string | 所属知识点名称                                       |
| data.question.type               | string | 题型：`choice`(选择题)、`fill`(填空题)、`solve`(解答题)     |
| data.question.difficulty         | float  | 难度系数（0.00-1.00，值越大越难）                         |
| data.question.content            | string | 题目内容（支持 LaTeX，使用 `$...$` 行内公式、`$$...$$` 块级公式） |
| data.question.options            | array  | 选项列表（仅选择题有此字段）                                |
| data.question.options[].label    | string | 选项标号（A/B/C/D）                                 |
| data.question.options[].text     | string | 选项内容（支持 LaTeX）                                |
| data.question.timeLimit          | int    | 建议作答时间（秒），仅供前端参考                              |

### 状态码说明

| 状态码 | 说明                      |
|-----|-------------------------|
| 200 | 诊断启动成功，返回第一道题目          |
| 400 | 参数错误（如 module 不在可选值范围内） |
| 401 | 未认证或 Token 已过期          |
| 409 | 存在未完成的诊断会话，请先完成或结束上一次诊断 |

---

## 2. 提交答案

提交当前诊断题目的答案。系统将：

1. 判断答案是否正确
2. 调用 LLM 进行错因分析（错误时返回错误类型与错因详细文本）
3. 根据自适应机制动态调整下一题的难度和知识点：
    - **答对** → 难度上调，跳过同级知识点，向更深层/更难的知识点探测
    - **答错** → 难度下调，深入当前知识点的子节点，进一步诊断薄弱环节
4. 返回下一道题目（若诊断尚未结束）

- **路径**：`/api/diagnostic/submit`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名          | 类型     | 必填 | 说明                                                    |
|--------------|--------|----|-------------------------------------------------------|
| diagnosticId | string | 是  | 诊断会话 ID（由 `/diagnostic/start` 返回）                     |
| questionId   | long   | 是  | 当前题目 ID                                               |
| answer       | string | 是  | 学生提交的答案。选择题为选项标号（如 `"A"`）；填空题为文本（支持 LaTeX）；解答题为完整解答文本 |
| timeSpent    | int    | 否  | 实际作答用时（秒），用于辅助评估                                      |

### 请求体示例

```json
{
  "diagnosticId": "diag_20260523_a1b2c3d4",
  "questionId": 1001,
  "answer": "A",
  "timeSpent": 45
}
```

### 响应体示例（答对，返回下一题）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "result": {
      "questionId": 1001,
      "isCorrect": true,
      "correctAnswer": "A",
      "errorType": null,
      "errorDetail": null,
      "errorAnalysis": null,
      "solution": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1) = 2$，其几何意义为曲线 $y = x^2$ 在点 $(1, 1)$ 处切线的斜率。"
    },
    "adaptiveInfo": {
      "previousDifficulty": 0.50,
      "nextDifficulty": 0.70,
      "adjustmentReason": "答对当前题目，难度上调，跳过同级知识点"
    },
    "isFinished": false,
    "currentQuestionIndex": 2,
    "totalEstimatedQuestions": 6,
    "nextQuestion": {
      "questionId": 1015,
      "knowledgePointId": "math_derivative_calc",
      "knowledgePointName": "导数的计算",
      "type": "choice",
      "difficulty": 0.70,
      "content": "求函数 $f(x) = x^3 - 3x^2 + 2$ 的导函数 $f'(x)$（　　）",
      "options": [
        { "label": "A", "text": "$3x^2 - 6x$" },
        { "label": "B", "text": "$3x^2 - 6x + 2$" },
        { "label": "C", "text": "$x^2 - 6x$" },
        { "label": "D", "text": "$3x^2 - 3x$" }
      ],
      "timeLimit": 90
    }
  }
}
```

### 响应体示例（答错，返回下一题 + 错因分析）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "result": {
      "questionId": 1015,
      "isCorrect": false,
      "correctAnswer": "A",
      "errorType": "计算性错误",
      "errorDetail": "运算法则错误-求导法则应用不当",
      "errorAnalysis": "你选择了 B 选项 $3x^2 - 6x + 2$，错误原因在于对常数项求导的处理：常数 $2$ 的导数应为 $0$，而非保留原值。求导法则要求：$(C)' = 0$（C 为常数）。正确的求导过程为：$f'(x) = (x^3)' - (3x^2)' + (2)' = 3x^2 - 6x + 0 = 3x^2 - 6x$。",
      "solution": "$f(x) = x^3 - 3x^2 + 2$\n\n逐项求导：\n- $(x^3)' = 3x^2$\n- $(3x^2)' = 6x$\n- $(2)' = 0$\n\n因此 $f'(x) = 3x^2 - 6x$"
    },
    "adaptiveInfo": {
      "previousDifficulty": 0.70,
      "nextDifficulty": 0.45,
      "adjustmentReason": "答错当前题目，难度下调，深入子知识点'基本求导法则'进一步诊断"
    },
    "isFinished": false,
    "currentQuestionIndex": 3,
    "totalEstimatedQuestions": 6,
    "nextQuestion": {
      "questionId": 1008,
      "knowledgePointId": "math_derivative_basic_rules",
      "knowledgePointName": "基本求导法则",
      "type": "choice",
      "difficulty": 0.45,
      "content": "下列求导结果正确的是（　　）",
      "options": [
        { "label": "A", "text": "$(x^4)' = 4x^3$" },
        { "label": "B", "text": "$(3x)' = 3x$" },
        { "label": "C", "text": "$(5)' = 5$" },
        { "label": "D", "text": "$(x^2 + 1)' = x^2$" }
      ],
      "timeLimit": 60
    }
  }
}
```

### 响应字段说明

| 字段                                   | 类型      | 说明                                                          |
|--------------------------------------|---------|-------------------------------------------------------------|
| data.diagnosticId                    | string  | 诊断会话 ID                                                     |
| data.result                          | object  | 当前题目的作答结果                                                   |
| data.result.questionId               | long    | 题目 ID                                                       |
| data.result.isCorrect                | boolean | 是否回答正确                                                      |
| data.result.correctAnswer            | string  | 正确答案                                                        |
| data.result.errorType                | string  | 错误类型（答对时为 null）。可选值：`概念性错误`、`计算性错误`、`逻辑性错误`、`方法性错误`、`审题性错误` |
| data.result.errorDetail              | string  | 错误子类描述（答对时为 null），如"符号错误-负号丢失"、"概念混淆-极值与最值"等                |
| data.result.errorAnalysis            | string  | LLM 生成的错因分析文本（答对时为 null），包含具体错在哪一步及原因分析，支持 LaTeX            |
| data.result.solution                 | string  | 完整题目解析（支持 LaTeX），无论对错均返回                                    |
| data.adaptiveInfo                    | object  | 自适应调整信息                                                     |
| data.adaptiveInfo.previousDifficulty | float   | 当前题目的难度                                                     |
| data.adaptiveInfo.nextDifficulty     | float   | 下一题的难度                                                      |
| data.adaptiveInfo.adjustmentReason   | string  | 难度调整原因说明                                                    |
| data.isFinished                      | boolean | 本次诊断是否已自动结束（达到足够题数且已覆盖关键知识点时系统可自动结束）                        |
| data.currentQuestionIndex            | int     | 下一题的序号                                                      |
| data.totalEstimatedQuestions         | int     | 预计总题数（可能动态调整）                                               |
| data.nextQuestion                    | object  | 下一道诊断题目（结构同启动接口中的 question 字段；若 isFinished 为 true 则为 null）  |

### 状态码说明

| 状态码 | 说明                               |
|-----|----------------------------------|
| 200 | 提交成功，返回判断结果和下一题                  |
| 400 | 参数错误（如 answer 为空、questionId 不匹配） |
| 401 | 未认证或 Token 已过期                   |
| 404 | 诊断会话不存在或已过期                      |
| 409 | 该题已提交过答案，不可重复提交                  |

---

## 3. 结束诊断

手动结束当前诊断会话（或系统自动结束后调用此接口获取诊断结论）。系统将汇总所有作答数据，利用 BKT/IRT
模型计算各知识点掌握度，生成完整的诊断结论，包括薄弱知识点列表、各知识点掌握度、错误模式统计以及后续学习建议。

- **路径**：`/api/diagnostic/finish`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名          | 类型     | 必填 | 说明      |
|--------------|--------|----|---------|
| diagnosticId | string | 是  | 诊断会话 ID |

### 请求体示例

```json
{
  "diagnosticId": "diag_20260523_a1b2c3d4"
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "module": "导数",
    "startTime": "2026-05-23T14:00:00",
    "endTime": "2026-05-23T14:12:35",
    "totalQuestions": 6,
    "correctCount": 3,
    "incorrectCount": 3,
    "accuracy": 0.50,
    "overallMastery": 0.45,
    "knowledgePointResults": [
      {
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "mastery": 0.78,
        "status": "基本掌握",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35,
        "status": "薄弱",
        "questionCount": 2,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_monotonicity",
        "knowledgePointName": "导数与单调性",
        "mastery": 0.60,
        "status": "一般",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      }
    ],
    "weakPoints": [
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35
      },
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42
      }
    ],
    "errorPatterns": {
      "概念性错误": 1,
      "计算性错误": 2,
      "逻辑性错误": 0,
      "方法性错误": 0,
      "审题性错误": 0
    },
    "suggestion": "本次诊断发现你在「导数的计算」和「导数的应用」两个知识点上较为薄弱，主要错误集中在计算性错误（求导法则应用不当）。建议先巩固基本求导法则，再逐步过渡到导数的综合应用。系统可为你进行溯源分析，找到更深层的薄弱根因。",
    "canTracing": true
  }
}
```

### 响应字段说明

| 字段                                              | 类型      | 说明                                                        |
|-------------------------------------------------|---------|-----------------------------------------------------------|
| data.diagnosticId                               | string  | 诊断会话 ID                                                   |
| data.module                                     | string  | 诊断的数学模块                                                   |
| data.startTime                                  | string  | 诊断开始时间（ISO 8601 格式）                                       |
| data.endTime                                    | string  | 诊断结束时间                                                    |
| data.totalQuestions                             | int     | 总题数                                                       |
| data.correctCount                               | int     | 答对题数                                                      |
| data.incorrectCount                             | int     | 答错题数                                                      |
| data.accuracy                                   | float   | 正确率（0.00-1.00）                                            |
| data.overallMastery                             | float   | 该模块总体掌握度（0.00-1.00，由 BKT/IRT 模型综合计算）                      |
| data.knowledgePointResults                      | array   | 各知识点诊断结果列表                                                |
| data.knowledgePointResults[].knowledgePointId   | string  | 知识点 ID                                                    |
| data.knowledgePointResults[].knowledgePointName | string  | 知识点名称                                                     |
| data.knowledgePointResults[].mastery            | float   | 掌握度（0.00-1.00）                                            |
| data.knowledgePointResults[].status             | string  | 掌握状态：`薄弱`(<0.5)、`一般`(0.5-0.7)、`基本掌握`(0.7-0.9)、`熟练`(>=0.9) |
| data.knowledgePointResults[].questionCount      | int     | 该知识点出题数量                                                  |
| data.knowledgePointResults[].correctCount       | int     | 该知识点答对数量                                                  |
| data.weakPoints                                 | array   | 薄弱知识点列表（掌握度 < 0.6 的知识点，按掌握度升序排列）                          |
| data.weakPoints[].knowledgePointId              | string  | 知识点 ID                                                    |
| data.weakPoints[].knowledgePointName            | string  | 知识点名称                                                     |
| data.weakPoints[].mastery                       | float   | 掌握度                                                       |
| data.errorPatterns                              | object  | 错误模式统计（五大类错误各出现的次数）                                       |
| data.suggestion                                 | string  | LLM 生成的诊断结论与学习建议文本                                        |
| data.canTracing                                 | boolean | 是否存在薄弱知识点可进行溯源分析                                          |

### 状态码说明

| 状态码 | 说明              |
|-----|-----------------|
| 200 | 诊断结束成功，返回完整诊断结论 |
| 400 | 参数错误            |
| 401 | 未认证或 Token 已过期  |
| 404 | 诊断会话不存在         |
| 409 | 该诊断会话已结束，请勿重复结束 |

---

## 4. 历史诊断记录列表

获取当前登录学生的历史诊断记录列表，支持分页查询。

- **路径**：`/api/diagnostic/history`
- **方法**：`GET`

### 请求参数（Query Parameters）

| 参数名      | 类型     | 必填 | 说明               |
|----------|--------|----|------------------|
| module   | string | 否  | 按模块筛选，可选值同启动接口   |
| page     | int    | 否  | 页码，从 1 开始，默认 1   |
| pageSize | int    | 否  | 每页数量，默认 10，最大 50 |

### 请求示例

```
GET /api/diagnostic/history?module=导数&page=1&pageSize=10
Authorization: Bearer {token}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "pageSize": 10,
    "records": [
      {
        "diagnosticId": "diag_20260523_a1b2c3d4",
        "module": "导数",
        "startTime": "2026-05-23T14:00:00",
        "endTime": "2026-05-23T14:12:35",
        "totalQuestions": 6,
        "correctCount": 3,
        "accuracy": 0.50,
        "overallMastery": 0.45,
        "weakPointCount": 3,
        "status": "completed"
      },
      {
        "diagnosticId": "diag_20260520_e5f6g7h8",
        "module": "函数",
        "startTime": "2026-05-20T10:30:00",
        "endTime": "2026-05-20T10:42:18",
        "totalQuestions": 5,
        "correctCount": 4,
        "accuracy": 0.80,
        "overallMastery": 0.72,
        "weakPointCount": 1,
        "status": "completed"
      }
    ]
  }
}
```

### 响应字段说明

| 字段                            | 类型     | 说明                                       |
|-------------------------------|--------|------------------------------------------|
| data.total                    | int    | 总记录数                                     |
| data.page                     | int    | 当前页码                                     |
| data.pageSize                 | int    | 每页数量                                     |
| data.records                  | array  | 诊断记录列表                                   |
| data.records[].diagnosticId   | string | 诊断会话 ID                                  |
| data.records[].module         | string | 诊断模块                                     |
| data.records[].startTime      | string | 开始时间                                     |
| data.records[].endTime        | string | 结束时间                                     |
| data.records[].totalQuestions | int    | 总题数                                      |
| data.records[].correctCount   | int    | 答对题数                                     |
| data.records[].accuracy       | float  | 正确率                                      |
| data.records[].overallMastery | float  | 模块总体掌握度                                  |
| data.records[].weakPointCount | int    | 薄弱知识点数量                                  |
| data.records[].status         | string | 诊断状态：`in_progress`(进行中)、`completed`(已完成) |

### 状态码说明

| 状态码 | 说明             |
|-----|----------------|
| 200 | 查询成功           |
| 401 | 未认证或 Token 已过期 |

---

## 5. 诊断详细结果

获取某次诊断的完整详细结果，包括每道题的作答记录、错因分析、各知识点掌握度等。

- **路径**：`/api/diagnostic/result/{diagnosticId}`
- **方法**：`GET`

### 路径参数

| 参数名          | 类型     | 必填 | 说明      |
|--------------|--------|----|---------|
| diagnosticId | string | 是  | 诊断会话 ID |

### 请求示例

```
GET /api/diagnostic/result/diag_20260523_a1b2c3d4
Authorization: Bearer {token}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "module": "导数",
    "startTime": "2026-05-23T14:00:00",
    "endTime": "2026-05-23T14:12:35",
    "totalQuestions": 6,
    "correctCount": 3,
    "accuracy": 0.50,
    "overallMastery": 0.45,
    "answerRecords": [
      {
        "index": 1,
        "questionId": 1001,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "type": "choice",
        "difficulty": 0.50,
        "content": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1)$ 的几何意义是（　　）",
        "options": [
          { "label": "A", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的切线斜率" },
          { "label": "B", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的纵坐标" },
          { "label": "C", "text": "函数 $f(x) = x^2$ 在 $x = 1$ 处的函数值" },
          { "label": "D", "text": "曲线 $y = x^2$ 过原点的割线斜率" }
        ],
        "studentAnswer": "A",
        "correctAnswer": "A",
        "isCorrect": true,
        "timeSpent": 45,
        "errorType": null,
        "errorDetail": null,
        "errorAnalysis": null,
        "solution": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1) = 2$，其几何意义为曲线 $y = x^2$ 在点 $(1, 1)$ 处切线的斜率。"
      },
      {
        "index": 2,
        "questionId": 1015,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "type": "choice",
        "difficulty": 0.70,
        "content": "求函数 $f(x) = x^3 - 3x^2 + 2$ 的导函数 $f'(x)$（　　）",
        "options": [
          { "label": "A", "text": "$3x^2 - 6x$" },
          { "label": "B", "text": "$3x^2 - 6x + 2$" },
          { "label": "C", "text": "$x^2 - 6x$" },
          { "label": "D", "text": "$3x^2 - 3x$" }
        ],
        "studentAnswer": "B",
        "correctAnswer": "A",
        "isCorrect": false,
        "timeSpent": 62,
        "errorType": "计算性错误",
        "errorDetail": "运算法则错误-求导法则应用不当",
        "errorAnalysis": "你选择了 B 选项 $3x^2 - 6x + 2$，错误原因在于对常数项求导的处理：常数 $2$ 的导数应为 $0$，而非保留原值。求导法则要求：$(C)' = 0$（C 为常数）。",
        "solution": "$f(x) = x^3 - 3x^2 + 2$\n\n逐项求导：\n- $(x^3)' = 3x^2$\n- $(3x^2)' = 6x$\n- $(2)' = 0$\n\n因此 $f'(x) = 3x^2 - 6x$"
      }
    ],
    "knowledgePointResults": [
      {
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "mastery": 0.78,
        "status": "基本掌握",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35,
        "status": "薄弱",
        "questionCount": 2,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_monotonicity",
        "knowledgePointName": "导数与单调性",
        "mastery": 0.60,
        "status": "一般",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      }
    ],
    "weakPoints": [
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42
      }
    ],
    "errorPatterns": {
      "概念性错误": 1,
      "计算性错误": 2,
      "逻辑性错误": 0,
      "方法性错误": 0,
      "审题性错误": 0
    },
    "suggestion": "本次诊断发现你在「导数的计算」和「导数的应用」两个知识点上较为薄弱，主要错误集中在计算性错误（求导法则应用不当）。建议先巩固基本求导法则，再逐步过渡到导数的综合应用。系统可为你进行溯源分析，找到更深层的薄弱根因。",
    "canTracing": true
  }
}
```

### 响应字段说明

响应中 `knowledgePointResults`、`weakPoints`、`errorPatterns`、`suggestion`、`canTracing`
等字段含义与 [结束诊断](#3-结束诊断) 接口一致。新增字段说明如下：

| 字段                                      | 类型      | 说明                    |
|-----------------------------------------|---------|-----------------------|
| data.answerRecords                      | array   | 完整的作答记录列表（按作答顺序排列）    |
| data.answerRecords[].index              | int     | 题目序号                  |
| data.answerRecords[].questionId         | long    | 题目 ID                 |
| data.answerRecords[].knowledgePointId   | string  | 所属知识点 ID              |
| data.answerRecords[].knowledgePointName | string  | 所属知识点名称               |
| data.answerRecords[].type               | string  | 题型                    |
| data.answerRecords[].difficulty         | float   | 难度系数                  |
| data.answerRecords[].content            | string  | 题目内容（支持 LaTeX）        |
| data.answerRecords[].options            | array   | 选项列表（选择题）             |
| data.answerRecords[].studentAnswer      | string  | 学生提交的答案               |
| data.answerRecords[].correctAnswer      | string  | 正确答案                  |
| data.answerRecords[].isCorrect          | boolean | 是否正确                  |
| data.answerRecords[].timeSpent          | int     | 作答用时（秒）               |
| data.answerRecords[].errorType          | string  | 错误类型（答对时为 null）       |
| data.answerRecords[].errorDetail        | string  | 错误子类描述（答对时为 null）     |
| data.answerRecords[].errorAnalysis      | string  | LLM 错因分析文本（答对时为 null） |
| data.answerRecords[].solution           | string  | 题目完整解析                |

### 状态码说明

| 状态码 | 说明               |
|-----|------------------|
| 200 | 查询成功             |
| 401 | 未认证或 Token 已过期   |
| 403 | 无权查看该诊断记录（非本人数据） |
| 404 | 诊断记录不存在          |

---

## 6. 溯源分析

对薄弱知识点进行溯源分析。这是系统的核心接口。溯源 Agent
将沿知识图谱的前置依赖链递归回溯，从目标薄弱知识点出发，逐层检查前置知识的掌握度，找到真正导致薄弱的根因知识点，并返回完整的溯源路径。

**溯源算法核心逻辑**：

1. 获取目标薄弱知识点的所有前置知识点
2. 对每个前置知识点检查掌握度：
    - 掌握度 < 0.6 → 继续递归溯源
    - 掌握度未知 → 触发快速诊断测试后再判定
3. 回溯终止条件：前置知识均已掌握（当前节点即为根因）、到达图谱叶节点、达到最大回溯深度
4. 支持跨模块溯源（如解析几何的问题根因可能在代数模块）

- **路径**：`/api/tracing/analyze`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名               | 类型     | 必填 | 说明                                               |
|-------------------|--------|----|--------------------------------------------------|
| diagnosticId      | string | 否  | 关联的诊断会话 ID。若提供，将基于该诊断中发现的薄弱知识点进行溯源               |
| knowledgePointIds | array  | 否  | 指定需要溯源的知识点 ID 列表。与 diagnosticId 二选一，若同时提供则以此字段为准 |
| maxDepth          | int    | 否  | 最大溯源深度，默认 5，最大 10。用于防止无限递归                       |
| masteryThreshold  | float  | 否  | 掌握度阈值，低于此值判定为薄弱，默认 0.6                           |

### 请求体示例（基于诊断结果溯源）

```json
{
  "diagnosticId": "diag_20260523_a1b2c3d4",
  "maxDepth": 5,
  "masteryThreshold": 0.6
}
```

### 请求体示例（指定知识点溯源）

```json
{
  "knowledgePointIds": ["math_derivative_application", "math_derivative_calc"],
  "maxDepth": 5,
  "masteryThreshold": 0.6
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tracingId": "trace_20260523_x9y8z7w6",
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "createdAt": "2026-05-23T14:15:00",
    "analyzedPointCount": 2,
    "rootCauseCount": 1,
    "isCrossModule": false,
    "tracingResults": [
      {
        "targetPointId": "math_derivative_application",
        "targetPointName": "导数的应用",
        "targetMastery": 0.30,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 3,
          "reason": "该知识点为当前溯源链的最深层薄弱点：其所有前置知识均已掌握，但其自身掌握度仅为 0.25，说明对变化率核心概念理解不足，导致后续依赖的导数计算、导数应用连锁薄弱。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_application",
            "knowledgePointName": "导数的应用",
            "module": "导数",
            "mastery": 0.30,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_extremum_max",
            "knowledgePointName": "极值与最值",
            "module": "导数",
            "mastery": 0.40,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 2,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 3,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_application",
            "to": "math_extremum_max",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_extremum_max",
            "to": "math_derivative_calc",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      },
      {
        "targetPointId": "math_derivative_calc",
        "targetPointName": "导数的计算",
        "targetMastery": 0.35,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 1,
          "reason": "与「导数的应用」的溯源根因一致，均指向变化率概念的理解不足。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      }
    ],
    "mergedRootCauses": [
      {
        "knowledgePointId": "math_change_rate",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "module": "导数",
        "mastery": 0.25,
        "affectedPoints": ["math_derivative_calc", "math_extremum_max", "math_derivative_application"],
        "affectedPointNames": ["导数的计算", "极值与最值", "导数的应用"],
        "priority": 1
      }
    ],
    "suggestion": "溯源分析发现，你在「导数的应用」和「导数的计算」上的薄弱，根因均指向「平均变化率与瞬时变化率」的理解不足（掌握度仅 0.25）。建议从变化率的概念出发，理解导数的本质含义，再逐步推进到导数计算和应用。建议学习路径：平均变化率与瞬时变化率 → 导数的概念与几何意义 → 导数的计算 → 极值与最值 → 导数的应用。"
  }
}
```

### 响应字段说明

| 字段                                                     | 类型      | 说明                                                   |
|--------------------------------------------------------|---------|------------------------------------------------------|
| data.tracingId                                         | string  | 溯源分析唯一标识                                             |
| data.diagnosticId                                      | string  | 关联的诊断会话 ID（若从诊断结果发起溯源）                               |
| data.createdAt                                         | string  | 溯源分析时间                                               |
| data.analyzedPointCount                                | int     | 分析的薄弱知识点数量                                           |
| data.rootCauseCount                                    | int     | 去重后发现的根因知识点数量                                        |
| data.isCrossModule                                     | boolean | 是否存在跨模块溯源（如解析几何根因在代数模块）                              |
| data.tracingResults                                    | array   | 每个薄弱知识点的溯源结果列表                                       |
| data.tracingResults[].targetPointId                    | string  | 目标薄弱知识点 ID                                           |
| data.tracingResults[].targetPointName                  | string  | 目标薄弱知识点名称                                            |
| data.tracingResults[].targetMastery                    | float   | 目标知识点掌握度                                             |
| data.tracingResults[].rootCause                        | object  | 溯源发现的根因知识点                                           |
| data.tracingResults[].rootCause.knowledgePointId       | string  | 根因知识点 ID                                             |
| data.tracingResults[].rootCause.knowledgePointName     | string  | 根因知识点名称                                              |
| data.tracingResults[].rootCause.module                 | string  | 根因知识点所属模块                                            |
| data.tracingResults[].rootCause.mastery                | float   | 根因知识点掌握度                                             |
| data.tracingResults[].rootCause.depth                  | int     | 溯源深度（从目标知识点到根因的距离）                                   |
| data.tracingResults[].rootCause.reason                 | string  | LLM 生成的根因判定理由说明                                      |
| data.tracingResults[].tracingPath                      | array   | 完整溯源路径节点列表（从目标知识点到根因，按回溯顺序排列）                        |
| data.tracingResults[].tracingPath[].knowledgePointId   | string  | 知识点 ID                                               |
| data.tracingResults[].tracingPath[].knowledgePointName | string  | 知识点名称                                                |
| data.tracingResults[].tracingPath[].module             | string  | 所属模块                                                 |
| data.tracingResults[].tracingPath[].mastery            | float   | 掌握度                                                  |
| data.tracingResults[].tracingPath[].status             | string  | 掌握状态：`薄弱`/`一般`/`基本掌握`/`熟练`                           |
| data.tracingResults[].tracingPath[].depth              | int     | 在溯源路径中的深度（目标知识点为 0）                                  |
| data.tracingResults[].tracingPath[].isTarget           | boolean | 是否为目标薄弱知识点                                           |
| data.tracingResults[].tracingPath[].isRootCause        | boolean | 是否为根因知识点                                             |
| data.tracingResults[].pathEdges                        | array   | 溯源路径上的边（前置依赖关系），用于可视化连线                              |
| data.tracingResults[].pathEdges[].from                 | string  | 起始知识点 ID（较高层知识点）                                     |
| data.tracingResults[].pathEdges[].to                   | string  | 目标知识点 ID（前置知识点）                                      |
| data.tracingResults[].pathEdges[].relation             | string  | 关系类型，固定为 `PREREQUISITE_OF`                           |
| data.tracingResults[].pathEdges[].weight               | float   | 依赖权重（0.0-1.0，值越大依赖越强）                                |
| data.tracingResults[].pathEdges[].type                 | string  | 依赖类型：`strong`(强依赖)、`weak`(弱依赖)、`cross_module`(跨模块依赖) |
| data.mergedRootCauses                                  | array   | 去重合并后的根因知识点列表（多个薄弱点可能指向同一根因），按优先级排序                  |
| data.mergedRootCauses[].knowledgePointId               | string  | 根因知识点 ID                                             |
| data.mergedRootCauses[].knowledgePointName             | string  | 根因知识点名称                                              |
| data.mergedRootCauses[].module                         | string  | 所属模块                                                 |
| data.mergedRootCauses[].mastery                        | float   | 掌握度                                                  |
| data.mergedRootCauses[].affectedPoints                 | array   | 受该根因影响的薄弱知识点 ID 列表                                   |
| data.mergedRootCauses[].affectedPointNames             | array   | 受该根因影响的薄弱知识点名称列表                                     |
| data.mergedRootCauses[].priority                       | int     | 优先修复顺序（1 为最优先）                                       |
| data.suggestion                                        | string  | LLM 生成的综合溯源分析结论与学习建议                                 |

### 状态码说明

| 状态码 | 说明                                            |
|-----|-----------------------------------------------|
| 200 | 溯源分析成功                                        |
| 400 | 参数错误（如 diagnosticId 和 knowledgePointIds 均未提供） |
| 401 | 未认证或 Token 已过期                                |
| 404 | 诊断记录不存在或指定的知识点 ID 在图谱中不存在                     |
| 422 | 诊断尚未完成，无法进行溯源分析                               |

---

## 7. 溯源结果详情

获取某次溯源分析的完整结果详情，包含完整溯源路径及可视化数据。适用于前端进行知识图谱溯源路径的可视化渲染（如 D3.js
力导向图、树形图等）。

- **路径**：`/api/tracing/result/{tracingId}`
- **方法**：`GET`

### 路径参数

| 参数名       | 类型     | 必填 | 说明       |
|-----------|--------|----|----------|
| tracingId | string | 是  | 溯源分析唯一标识 |

### 请求示例

```
GET /api/tracing/result/trace_20260523_x9y8z7w6
Authorization: Bearer {token}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tracingId": "trace_20260523_x9y8z7w6",
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "createdAt": "2026-05-23T14:15:00",
    "analyzedPointCount": 2,
    "rootCauseCount": 1,
    "isCrossModule": false,
    "tracingResults": [
      {
        "targetPointId": "math_derivative_application",
        "targetPointName": "导数的应用",
        "targetMastery": 0.30,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 3,
          "reason": "该知识点为当前溯源链的最深层薄弱点：其所有前置知识均已掌握，但其自身掌握度仅为 0.25，说明对变化率核心概念理解不足，导致后续依赖的导数计算、导数应用连锁薄弱。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_application",
            "knowledgePointName": "导数的应用",
            "module": "导数",
            "mastery": 0.30,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_extremum_max",
            "knowledgePointName": "极值与最值",
            "module": "导数",
            "mastery": 0.40,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 2,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 3,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_application",
            "to": "math_extremum_max",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_extremum_max",
            "to": "math_derivative_calc",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      },
      {
        "targetPointId": "math_derivative_calc",
        "targetPointName": "导数的计算",
        "targetMastery": 0.35,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 1,
          "reason": "与「导数的应用」的溯源根因一致，均指向变化率概念的理解不足。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      }
    ],
    "mergedRootCauses": [
      {
        "knowledgePointId": "math_change_rate",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "module": "导数",
        "mastery": 0.25,
        "affectedPoints": ["math_derivative_calc", "math_extremum_max", "math_derivative_application"],
        "affectedPointNames": ["导数的计算", "极值与最值", "导数的应用"],
        "priority": 1
      }
    ],
    "graphVisualization": {
      "nodes": [
        {
          "id": "math_derivative_application",
          "label": "导数的应用",
          "module": "导数",
          "mastery": 0.30,
          "nodeType": "target",
          "color": "#F56C6C",
          "size": 40
        },
        {
          "id": "math_extremum_max",
          "label": "极值与最值",
          "module": "导数",
          "mastery": 0.40,
          "nodeType": "intermediate",
          "color": "#F56C6C",
          "size": 32
        },
        {
          "id": "math_derivative_calc",
          "label": "导数的计算",
          "module": "导数",
          "mastery": 0.35,
          "nodeType": "target",
          "color": "#F56C6C",
          "size": 40
        },
        {
          "id": "math_change_rate",
          "label": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "nodeType": "rootCause",
          "color": "#FF0000",
          "size": 48
        }
      ],
      "edges": [
        {
          "source": "math_derivative_application",
          "target": "math_extremum_max",
          "label": "前置依赖",
          "weight": 0.9,
          "style": "solid"
        },
        {
          "source": "math_extremum_max",
          "target": "math_derivative_calc",
          "label": "前置依赖",
          "weight": 0.9,
          "style": "solid"
        },
        {
          "source": "math_derivative_calc",
          "target": "math_change_rate",
          "label": "前置依赖",
          "weight": 0.8,
          "style": "solid"
        }
      ],
      "colorLegend": {
        "nodeColors": {
          "#FF0000": "根因知识点",
          "#F56C6C": "薄弱知识点（掌握度 < 0.5）",
          "#E6A23C": "一般知识点（掌握度 0.5-0.7）",
          "#67C23A": "已掌握知识点（掌握度 >= 0.7）"
        },
        "edgeStyles": {
          "solid": "强依赖",
          "dashed": "弱依赖",
          "dotted": "跨模块依赖"
        }
      }
    },
    "suggestion": "溯源分析发现，你在「导数的应用」和「导数的计算」上的薄弱，根因均指向「平均变化率与瞬时变化率」的理解不足（掌握度仅 0.25）。建议从变化率的概念出发，理解导数的本质含义，再逐步推进到导数计算和应用。建议学习路径：平均变化率与瞬时变化率 → 导数的概念与几何意义 → 导数的计算 → 极值与最值 → 导数的应用。",
    "suggestedLearningPath": [
      {
        "order": 1,
        "knowledgePointId": "math_change_rate",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "currentMastery": 0.25,
        "estimatedMinutes": 45,
        "isRootCause": true
      },
      {
        "order": 2,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "currentMastery": 0.78,
        "estimatedMinutes": 15,
        "isRootCause": false
      },
      {
        "order": 3,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "currentMastery": 0.35,
        "estimatedMinutes": 50,
        "isRootCause": false
      },
      {
        "order": 4,
        "knowledgePointId": "math_extremum_max",
        "knowledgePointName": "极值与最值",
        "currentMastery": 0.40,
        "estimatedMinutes": 40,
        "isRootCause": false
      },
      {
        "order": 5,
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "currentMastery": 0.30,
        "estimatedMinutes": 45,
        "isRootCause": false
      }
    ]
  }
}
```

### 响应字段说明

本接口返回数据是 [溯源分析](#6-溯源分析) 接口响应的超集，在其基础上新增了 `graphVisualization`（图可视化数据）和
`suggestedLearningPath`（建议学习路径）两个字段。

| 字段                                              | 类型      | 说明                                                          |
|-------------------------------------------------|---------|-------------------------------------------------------------|
| data.graphVisualization                         | object  | 用于前端可视化渲染的图数据（D3.js / ECharts 等）                            |
| data.graphVisualization.nodes                   | array   | 图中所有节点（知识点）                                                 |
| data.graphVisualization.nodes[].id              | string  | 节点唯一标识（知识点 ID）                                              |
| data.graphVisualization.nodes[].label           | string  | 节点显示名称                                                      |
| data.graphVisualization.nodes[].module          | string  | 所属数学模块                                                      |
| data.graphVisualization.nodes[].mastery         | float   | 掌握度                                                         |
| data.graphVisualization.nodes[].nodeType        | string  | 节点类型：`target`(目标薄弱点)、`intermediate`(中间节点)、`rootCause`(根因节点) |
| data.graphVisualization.nodes[].color           | string  | 建议渲染颜色（HEX 格式），按掌握度和节点类型分配                                  |
| data.graphVisualization.nodes[].size            | int     | 建议节点大小（像素），根因节点最大、中间节点最小                                    |
| data.graphVisualization.edges                   | array   | 图中所有边（依赖关系）                                                 |
| data.graphVisualization.edges[].source          | string  | 起始节点 ID                                                     |
| data.graphVisualization.edges[].target          | string  | 目标节点 ID                                                     |
| data.graphVisualization.edges[].label           | string  | 边的标签说明                                                      |
| data.graphVisualization.edges[].weight          | float   | 依赖权重                                                        |
| data.graphVisualization.edges[].style           | string  | 线条样式：`solid`(实线-强依赖)、`dashed`(虚线-弱依赖)、`dotted`(点线-跨模块)      |
| data.graphVisualization.colorLegend             | object  | 颜色图例说明，供前端渲染图例使用                                            |
| data.graphVisualization.colorLegend.nodeColors  | object  | 节点颜色含义映射                                                    |
| data.graphVisualization.colorLegend.edgeStyles  | object  | 边样式含义映射                                                     |
| data.suggestedLearningPath                      | array   | 根据溯源结果生成的建议学习路径（拓扑排序，先学前置知识）                                |
| data.suggestedLearningPath[].order              | int     | 学习顺序                                                        |
| data.suggestedLearningPath[].knowledgePointId   | string  | 知识点 ID                                                      |
| data.suggestedLearningPath[].knowledgePointName | string  | 知识点名称                                                       |
| data.suggestedLearningPath[].currentMastery     | float   | 当前掌握度                                                       |
| data.suggestedLearningPath[].estimatedMinutes   | int     | 预计学习所需时间（分钟）                                                |
| data.suggestedLearningPath[].isRootCause        | boolean | 是否为根因知识点                                                    |

### 状态码说明

| 状态码 | 说明               |
|-----|------------------|
| 200 | 查询成功             |
| 401 | 未认证或 Token 已过期   |
| 403 | 无权查看该溯源记录（非本人数据） |
| 404 | 溯源记录不存在          |

---

## 附录 A：数据结构汇总

### A.1 诊断题目结构（Question）

```json
{
  "questionId": 1001,
  "knowledgePointId": "math_derivative_concept",
  "knowledgePointName": "导数的概念与几何意义",
  "type": "choice | fill | solve",
  "difficulty": 0.50,
  "content": "题目内容（支持 LaTeX：$...$ 行内公式、$$...$$ 块级公式）",
  "options": [
    { "label": "A", "text": "选项内容（支持 LaTeX）" }
  ],
  "timeLimit": 120
}
```

### A.2 作答结果结构（AnswerResult）

```json
{
  "questionId": 1001,
  "isCorrect": false,
  "correctAnswer": "A",
  "errorType": "概念性错误 | 计算性错误 | 逻辑性错误 | 方法性错误 | 审题性错误",
  "errorDetail": "错误子类描述，如：概念混淆-极值与最值、符号错误-负号丢失、分类讨论遗漏",
  "errorAnalysis": "LLM 生成的详细错因分析文本，说明具体错在哪一步、为什么错",
  "solution": "完整题目解析（支持 LaTeX）"
}
```

### A.3 溯源路径节点结构（TracingPathNode）

```json
{
  "knowledgePointId": "math_change_rate",
  "knowledgePointName": "平均变化率与瞬时变化率",
  "module": "导数",
  "mastery": 0.25,
  "status": "薄弱 | 一般 | 基本掌握 | 熟练",
  "depth": 3,
  "isTarget": false,
  "isRootCause": true
}
```

### A.4 溯源路径边结构（TracingPathEdge）

```json
{
  "from": "math_derivative_calc",
  "to": "math_change_rate",
  "relation": "PREREQUISITE_OF",
  "weight": 0.8,
  "type": "strong | weak | cross_module"
}
```

### A.5 错误类型枚举

| 错误大类  | 子类示例                            |
|-------|---------------------------------|
| 概念性错误 | 定义理解偏差、概念混淆（如混淆"极值"与"最值"）       |
| 计算性错误 | 符号错误（负号丢失）、运算法则错误、精度错误（分数化简不彻底） |
| 逻辑性错误 | 充要条件误判、分类讨论遗漏、推理跳步              |
| 方法性错误 | 方法选择不当、转化方向错误                   |
| 审题性错误 | 条件遗漏（忽略隐含条件）、问题误读               |

### A.6 自适应机制说明

| 作答情况 | 难度调整策略              | 知识点选择策略           |
|------|---------------------|-------------------|
| 答对   | 难度上调（+0.15 ~ +0.25） | 跳过同级知识点，向更高阶知识点探测 |
| 答错   | 难度下调（-0.15 ~ -0.25） | 深入该知识点的子节点/前置知识点  |
| 连续答对 | 加速跳过，减少同模块出题数量      | 快速完成该模块诊断         |
| 连续答错 | 大幅下调，从基础题重新开始       | 切换到更基础的前置知识点      |

每个模块通常 5-8 题完成诊断，系统根据信息增益自动判断何时结束。

---

## 附录 B：跨模块溯源示例

当溯源过程中发现根因知识点属于不同的数学模块时，响应中 `isCrossModule` 字段为 `true`，溯源路径中的 `module` 字段将体现模块切换。

### 请求体示例

```json
{
  "knowledgePointIds": ["math_conic_line_intersection"],
  "maxDepth": 6
}
```

### 响应体（关键片段）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tracingId": "trace_20260523_cross01",
    "isCrossModule": true,
    "tracingResults": [
      {
        "targetPointId": "math_conic_line_intersection",
        "targetPointName": "直线与圆锥曲线联立",
        "targetMastery": 0.28,
        "rootCause": {
          "knowledgePointId": "math_quadratic_equation_roots",
          "knowledgePointName": "一元二次方程求根",
          "module": "代数基础",
          "mastery": 0.30,
          "depth": 3,
          "reason": "溯源发现跨模块根因：解析几何中「直线与圆锥曲线联立」的薄弱根因在代数基础模块的「一元二次方程求根」。联立方程组最终需解一元二次方程，该基础能力不足导致上层知识连锁薄弱。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_conic_line_intersection",
            "knowledgePointName": "直线与圆锥曲线联立",
            "module": "解析几何",
            "mastery": 0.28,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_simultaneous_equations",
            "knowledgePointName": "联立方程组求解",
            "module": "解析几何",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_vieta_discriminant",
            "knowledgePointName": "韦达定理与判别式",
            "module": "代数基础",
            "mastery": 0.40,
            "status": "薄弱",
            "depth": 2,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_quadratic_equation_roots",
            "knowledgePointName": "一元二次方程求根",
            "module": "代数基础",
            "mastery": 0.30,
            "status": "薄弱",
            "depth": 3,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_conic_line_intersection",
            "to": "math_simultaneous_equations",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_simultaneous_equations",
            "to": "math_vieta_discriminant",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "cross_module"
          },
          {
            "from": "math_vieta_discriminant",
            "to": "math_quadratic_equation_roots",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          }
        ]
      }
    ],
    "suggestion": "溯源发现跨模块根因：你在解析几何「直线与圆锥曲线联立」上的困难，根因在代数基础模块的「一元二次方程求根」（掌握度仅 0.30）。建议先从代数基础补起：一元二次方程求根 → 韦达定理与判别式 → 联立方程组求解 → 直线与圆锥曲线联立。"
  }
}
```

---

<!-- Source: api-learning-chat.md -->

# SmartMentor 学习路径与AI对话辅导 接口文档

> 版本：v1.0
> 基础路径：`/api`
> 认证方式：JWT Bearer Token（所有接口均需在请求头中携带 `Authorization: Bearer {token}`）
> 统一响应格式：`{ "code": 200, "message": "success", "data": {...} }`

---

## 目录

1. [POST /api/learning/path/generate - 生成学习路径](#1-生成学习路径)
2. [GET /api/learning/path - 获取当前活跃学习路径](#2-获取当前活跃学习路径)
3. [GET /api/learning/path/{pathId} - 获取指定学习路径详情](#3-获取指定学习路径详情)
4. [GET /api/learning/lesson/{pathId}/{nodeId} - 获取个性化课程内容](#4-获取个性化课程内容)
5. [POST /api/learning/exercise/submit - 提交练习答案](#5-提交练习答案)
6. [POST /api/learning/checkpoint/submit - 提交检查点测试](#6-提交检查点测试)
7. [GET /api/chat/stream - SSE流式AI对话辅导](#7-sse流式ai对话辅导)
8. [GET /api/chat/history - 获取对话历史记录](#8-获取对话历史记录)

---

## 通用说明

### 认证方式

所有接口均需要在 HTTP 请求头中携带 JWT Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 通用状态码

| 状态码 | 说明          |
|-----|-------------|
| 200 | 请求成功        |
| 400 | 请求参数错误      |
| 401 | 未认证或Token过期 |
| 403 | 无权限访问       |
| 404 | 资源不存在       |
| 500 | 服务器内部错误     |

---

## 1. 生成学习路径

基于溯源Agent的分析结果，通过DAG拓扑排序生成从根因知识点到目标知识点的有序学习路径。

### 基本信息

| 项目               | 内容                                                                  |
|------------------|---------------------------------------------------------------------|
| **路径**           | `/api/learning/path/generate`                                       |
| **方法**           | `POST`                                                              |
| **说明**           | 根据溯源结果中的薄弱知识点集合，构建有向无环图（DAG），通过拓扑排序确定学习顺序，结合学生画像分配学习时间，生成完整的个性化学习路径 |
| **认证**           | 需要 JWT Token                                                        |
| **Content-Type** | `application/json`                                                  |

### 请求参数

#### 请求体（Request Body）

| 参数名                    | 类型      | 必填 | 说明                                               |
|------------------------|---------|----|--------------------------------------------------|
| tracingResultId        | Long    | 是  | 溯源结果ID，关联溯源Agent的输出                              |
| targetKnowledgePointId | String  | 是  | 目标知识点ID（学生最终要掌握的知识点）                             |
| mode                   | String  | 否  | 学习模式：`urgent`（考试临近，紧凑计划）/ `systematic`（系统提升，默认值） |
| dailyStudyMinutes      | Integer | 否  | 每日学习时长（分钟），默认从学生画像读取                             |

### 请求体示例

```json
{
  "tracingResultId": 1001,
  "targetKnowledgePointId": "math_derivative_application",
  "mode": "systematic",
  "dailyStudyMinutes": 60
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "studentId": 10001,
    "targetKnowledgePoint": {
      "id": "math_derivative_application",
      "name": "导数的应用"
    },
    "rootCausePoint": {
      "id": "math_average_rate_of_change",
      "name": "平均变化率与瞬时变化率"
    },
    "mode": "systematic",
    "totalEstimatedMinutes": 180,
    "totalNodes": 4,
    "nodes": [
      {
        "nodeId": "node_001",
        "order": 1,
        "knowledgePointId": "math_average_rate_of_change",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "currentMastery": 0.25,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "status": "pending",
        "prerequisites": [],
        "subtasks": [
          { "type": "lesson", "title": "概念讲解", "minutes": 15 },
          { "type": "exercise", "title": "基础练习", "minutes": 20 },
          { "type": "checkpoint", "title": "检查点测试", "minutes": 10 }
        ]
      },
      {
        "nodeId": "node_002",
        "order": 2,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "currentMastery": 0.38,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 40,
        "status": "locked",
        "prerequisites": ["node_001"],
        "subtasks": [
          { "type": "lesson", "title": "概念讲解", "minutes": 15 },
          { "type": "exercise", "title": "强化练习", "minutes": 20 },
          { "type": "checkpoint", "title": "检查点测试", "minutes": 5 }
        ]
      },
      {
        "nodeId": "node_003",
        "order": 3,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "currentMastery": 0.42,
        "teachingStrategy": "strengthening",
        "estimatedMinutes": 50,
        "status": "locked",
        "prerequisites": ["node_002"],
        "subtasks": [
          { "type": "lesson", "title": "错因针对性讲解", "minutes": 15 },
          { "type": "exercise", "title": "变式训练", "minutes": 25 },
          { "type": "checkpoint", "title": "检查点测试", "minutes": 10 }
        ]
      },
      {
        "nodeId": "node_004",
        "order": 4,
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "currentMastery": 0.30,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "status": "locked",
        "prerequisites": ["node_003"],
        "subtasks": [
          { "type": "lesson", "title": "综合讲解", "minutes": 15 },
          { "type": "exercise", "title": "综合练习", "minutes": 20 },
          { "type": "checkpoint", "title": "综合测试（含原始错题变式）", "minutes": 10 }
        ]
      }
    ],
    "tracingPath": [
      "math_derivative_application",
      "math_derivative_calc",
      "math_derivative_concept",
      "math_average_rate_of_change"
    ],
    "status": "active",
    "progress": 0.00,
    "createdAt": "2026-05-23T10:30:00"
  }
}
```

### 状态码说明

| 状态码 | 说明                                |
|-----|-----------------------------------|
| 200 | 学习路径生成成功                          |
| 400 | 参数错误（如tracingResultId无效、目标知识点不存在） |
| 401 | 未认证或Token过期                       |
| 404 | 溯源结果不存在                           |
| 409 | 已存在进行中的同目标学习路径（需先完成或暂停现有路径）       |
| 500 | 服务器内部错误（如知识图谱查询失败）                |

---

## 2. 获取当前活跃学习路径

获取当前学生正在进行中（status=active）的学习路径列表。

### 基本信息

| 项目     | 内容                                     |
|--------|----------------------------------------|
| **路径** | `/api/learning/path`                   |
| **方法** | `GET`                                  |
| **说明** | 获取当前登录学生所有状态为active的学习路径概览信息，按创建时间倒序排列 |
| **认证** | 需要 JWT Token                           |

### 请求参数

#### Query参数

| 参数名    | 类型      | 必填 | 说明                                                  |
|--------|---------|----|-----------------------------------------------------|
| status | String  | 否  | 路径状态筛选：`active`（默认）/ `completed` / `paused` / `all` |
| page   | Integer | 否  | 页码，默认1                                              |
| size   | Integer | 否  | 每页条数，默认10                                           |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/learning/path?status=active&page=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "page": 1,
    "size": 10,
    "paths": [
      {
        "pathId": 2001,
        "targetKnowledgePoint": {
          "id": "math_derivative_application",
          "name": "导数的应用",
          "module": "导数"
        },
        "rootCausePoint": {
          "id": "math_average_rate_of_change",
          "name": "平均变化率与瞬时变化率"
        },
        "totalNodes": 4,
        "completedNodes": 1,
        "currentNode": {
          "nodeId": "node_002",
          "knowledgePointName": "导数的概念与几何意义"
        },
        "progress": 0.25,
        "status": "active",
        "mode": "systematic",
        "totalEstimatedMinutes": 180,
        "actualStudyMinutes": 48,
        "createdAt": "2026-05-20T10:30:00",
        "lastStudyAt": "2026-05-22T19:15:00"
      },
      {
        "pathId": 2002,
        "targetKnowledgePoint": {
          "id": "math_trig_identity",
          "name": "三角恒等变换",
          "module": "三角函数"
        },
        "rootCausePoint": {
          "id": "math_angle_addition",
          "name": "和差角公式"
        },
        "totalNodes": 3,
        "completedNodes": 0,
        "currentNode": {
          "nodeId": "node_010",
          "knowledgePointName": "和差角公式"
        },
        "progress": 0.00,
        "status": "active",
        "mode": "urgent",
        "totalEstimatedMinutes": 90,
        "actualStudyMinutes": 0,
        "createdAt": "2026-05-22T08:00:00",
        "lastStudyAt": null
      }
    ]
  }
}
```

### 状态码说明

| 状态码 | 说明          |
|-----|-------------|
| 200 | 请求成功        |
| 401 | 未认证或Token过期 |
| 500 | 服务器内部错误     |

---

## 3. 获取指定学习路径详情

根据路径ID获取学习路径的完整详细信息，包括所有节点的状态和学习进度。

### 基本信息

| 项目     | 内容                                          |
|--------|---------------------------------------------|
| **路径** | `/api/learning/path/{pathId}`               |
| **方法** | `GET`                                       |
| **说明** | 获取指定学习路径的完整详情，包含所有知识点节点、各节点学习状态、掌握度变化、溯源路径等 |
| **认证** | 需要 JWT Token                                |

### 请求参数

#### 路径参数（Path Parameters）

| 参数名    | 类型   | 必填 | 说明     |
|--------|------|----|--------|
| pathId | Long | 是  | 学习路径ID |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/learning/path/2001
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "studentId": 10001,
    "targetKnowledgePoint": {
      "id": "math_derivative_application",
      "name": "导数的应用",
      "module": "导数",
      "difficulty": 0.7,
      "importance": 0.9
    },
    "rootCausePoint": {
      "id": "math_average_rate_of_change",
      "name": "平均变化率与瞬时变化率",
      "module": "导数",
      "difficulty": 0.4
    },
    "mode": "systematic",
    "status": "active",
    "progress": 0.25,
    "totalEstimatedMinutes": 180,
    "actualStudyMinutes": 48,
    "totalNodes": 4,
    "completedNodes": 1,
    "nodes": [
      {
        "nodeId": "node_001",
        "order": 1,
        "knowledgePointId": "math_average_rate_of_change",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "module": "导数",
        "currentMastery": 0.82,
        "masteryBefore": 0.25,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "actualMinutes": 48,
        "status": "completed",
        "completedAt": "2026-05-21T20:30:00",
        "checkpointPassed": true,
        "checkpointScore": 0.85,
        "prerequisites": []
      },
      {
        "nodeId": "node_002",
        "order": 2,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "module": "导数",
        "currentMastery": 0.38,
        "masteryBefore": 0.38,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 40,
        "actualMinutes": 0,
        "status": "in_progress",
        "completedAt": null,
        "checkpointPassed": null,
        "checkpointScore": null,
        "prerequisites": ["node_001"]
      },
      {
        "nodeId": "node_003",
        "order": 3,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "module": "导数",
        "currentMastery": 0.42,
        "masteryBefore": 0.42,
        "teachingStrategy": "strengthening",
        "estimatedMinutes": 50,
        "actualMinutes": 0,
        "status": "locked",
        "completedAt": null,
        "checkpointPassed": null,
        "checkpointScore": null,
        "prerequisites": ["node_002"]
      },
      {
        "nodeId": "node_004",
        "order": 4,
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "module": "导数",
        "currentMastery": 0.30,
        "masteryBefore": 0.30,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "actualMinutes": 0,
        "status": "locked",
        "completedAt": null,
        "checkpointPassed": null,
        "checkpointScore": null,
        "prerequisites": ["node_003"]
      }
    ],
    "tracingResult": {
      "tracingId": 1001,
      "tracingPath": [
        {
          "knowledgePointId": "math_derivative_application",
          "name": "导数的应用",
          "mastery": 0.30,
          "role": "target"
        },
        {
          "knowledgePointId": "math_derivative_calc",
          "name": "导数的计算",
          "mastery": 0.42,
          "role": "intermediate"
        },
        {
          "knowledgePointId": "math_derivative_concept",
          "name": "导数的概念与几何意义",
          "mastery": 0.38,
          "role": "intermediate"
        },
        {
          "knowledgePointId": "math_average_rate_of_change",
          "name": "平均变化率与瞬时变化率",
          "mastery": 0.25,
          "role": "root_cause"
        }
      ]
    },
    "createdAt": "2026-05-20T10:30:00",
    "lastStudyAt": "2026-05-22T19:15:00",
    "completedAt": null
  }
}
```

### 状态码说明

| 状态码 | 说明               |
|-----|------------------|
| 200 | 请求成功             |
| 401 | 未认证或Token过期      |
| 403 | 无权访问该学习路径（非本人路径） |
| 404 | 指定的学习路径不存在       |
| 500 | 服务器内部错误          |

---

## 4. 获取个性化课程内容

根据学生当前掌握度，采用三级教学策略（基础夯实/强化突破/拓展提升）生成个性化课程内容。

### 基本信息

| 项目     | 内容                                                                                                                            |
|--------|-------------------------------------------------------------------------------------------------------------------------------|
| **路径** | `/api/learning/lesson/{pathId}/{nodeId}`                                                                                      |
| **方法** | `GET`                                                                                                                         |
| **说明** | 获取学习路径中某个知识点节点的个性化课程内容。系统根据学生掌握度自动选择教学策略：掌握度<0.4采用"基础夯实"策略，0.4-0.7采用"强化突破"策略，>0.7采用"拓展提升"策略。内容由教学Agent结合学生画像（认知风格、错误模式等）动态生成。 |
| **认证** | 需要 JWT Token                                                                                                                  |

### 请求参数

#### 路径参数（Path Parameters）

| 参数名    | 类型     | 必填 | 说明                  |
|--------|--------|----|---------------------|
| pathId | Long   | 是  | 学习路径ID              |
| nodeId | String | 是  | 节点ID（学习路径中的知识点节点标识） |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/learning/lesson/2001/node_001
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "nodeId": "node_001",
    "knowledgePoint": {
      "id": "math_average_rate_of_change",
      "name": "平均变化率与瞬时变化率",
      "module": "导数",
      "difficulty": 0.4
    },
    "studentMastery": 0.25,
    "teachingStrategy": "basic_consolidation",
    "teachingStrategyLabel": "基础夯实",
    "cognitiveStyle": "visual",
    "lesson": {
      "title": "平均变化率与瞬时变化率 — 从生活现象理解变化",
      "sections": [
        {
          "type": "concept",
          "title": "核心概念",
          "content": "## 什么是变化率？\n\n想象你骑自行车上学，从家到学校共3公里，用了15分钟。\n\n**平均速度** = 总路程 ÷ 总时间 = $\\frac{3}{15}$ = 0.2 km/min\n\n这就是一个**平均变化率**。\n\n用函数语言表达：设路程函数为 $s(t)$，则在时间段 $[t_1, t_2]$ 上的平均变化率为：\n\n$$\\frac{\\Delta s}{\\Delta t} = \\frac{s(t_2) - s(t_1)}{t_2 - t_1}$$\n\n**瞬时变化率**就是当 $\\Delta t \\to 0$ 时的极限值，它描述的是某一瞬间的变化快慢。",
          "mediaType": "text_with_latex"
        },
        {
          "type": "visualization",
          "title": "图形理解",
          "content": "观察下面的函数图像，割线斜率就是平均变化率，切线斜率就是瞬时变化率。",
          "chartConfig": {
            "type": "function_graph",
            "function": "x^2",
            "highlights": [
              { "type": "secant", "x1": 1, "x2": 3, "label": "平均变化率=割线斜率" },
              { "type": "tangent", "x": 2, "label": "瞬时变化率=切线斜率" }
            ]
          },
          "mediaType": "interactive_chart"
        },
        {
          "type": "example",
          "title": "典型例题",
          "content": "**例题**：已知函数 $f(x) = x^2$，求在区间 $[1, 3]$ 上的平均变化率。\n\n**解答**：\n\n$$\\frac{f(3) - f(1)}{3 - 1} = \\frac{9 - 1}{2} = 4$$\n\n所以 $f(x) = x^2$ 在 $[1,3]$ 上的平均变化率为 **4**。",
          "mediaType": "text_with_latex"
        },
        {
          "type": "key_points",
          "title": "易错提醒",
          "content": "1. 平均变化率不等于中点处的瞬时变化率（只有线性函数才相等）\n2. 变化率可以是负值，表示函数在递减\n3. 注意分母是自变量的变化量 $\\Delta x$，不是函数值的变化量",
          "mediaType": "text"
        }
      ],
      "exercises": [
        {
          "exerciseId": "ex_001",
          "type": "fill",
          "difficulty": 0.3,
          "content": "函数 $f(x) = 2x + 1$ 在区间 $[0, 2]$ 上的平均变化率为 ______。",
          "answer": "2",
          "hint": "代入公式 $\\frac{f(2)-f(0)}{2-0}$"
        },
        {
          "exerciseId": "ex_002",
          "type": "choice",
          "difficulty": 0.4,
          "content": "函数 $f(x) = x^2$ 在区间 $[1, 1+\\Delta x]$ 上的平均变化率为：",
          "options": [
            "A. $2 + \\Delta x$",
            "B. $2\\Delta x$",
            "C. $1 + \\Delta x$",
            "D. $2$"
          ],
          "answer": "A",
          "solution": "$\\frac{(1+\\Delta x)^2 - 1}{\\Delta x} = \\frac{2\\Delta x + (\\Delta x)^2}{\\Delta x} = 2 + \\Delta x$"
        },
        {
          "exerciseId": "ex_003",
          "type": "solve",
          "difficulty": 0.4,
          "content": "已知物体运动路程函数 $s(t) = 3t^2 + 2t$（单位：米，秒），求在 $t=1$ 到 $t=3$ 时间段内的平均速度。",
          "answer": "14 m/s",
          "solution": "平均速度 = $\\frac{s(3)-s(1)}{3-1} = \\frac{(27+6)-(3+2)}{2} = \\frac{28}{2} = 14$ m/s"
        }
      ],
      "totalExercises": 3,
      "ragSources": [
        { "source": "人教版高中数学选择性必修第二册", "chapter": "第五章 一元函数的导数及其应用", "page": "P2-P5" }
      ]
    },
    "generatedAt": "2026-05-23T10:35:00"
  }
}
```

### 三级教学策略说明

| 策略   | 掌握度范围     | 策略标识                  | 内容特点                         |
|------|-----------|-----------------------|------------------------------|
| 基础夯实 | < 0.4     | `basic_consolidation` | 概念重讲 + 直观案例 + 基础练习题，注重从零建立理解 |
| 强化突破 | 0.4 - 0.7 | `strengthening`       | 错因针对性讲解 + 变式训练，聚焦学生具体错误模式    |
| 拓展提升 | > 0.7     | `expansion`           | 综合应用 + 高考真题 + 能力迁移，提升综合运用能力  |

### 状态码说明

| 状态码 | 说明                |
|-----|-------------------|
| 200 | 课程内容生成成功          |
| 401 | 未认证或Token过期       |
| 403 | 无权访问该学习路径         |
| 404 | 路径或节点不存在          |
| 423 | 该节点尚未解锁（前置节点未完成）  |
| 500 | 服务器内部错误（如LLM调用失败） |

---

## 5. 提交练习答案

提交练习题答案，系统进行即时批改并通过LLM进行错因分析。

### 基本信息

| 项目               | 内容                                                                      |
|------------------|-------------------------------------------------------------------------|
| **路径**           | `/api/learning/exercise/submit`                                         |
| **方法**           | `POST`                                                                  |
| **说明**           | 提交学生的练习题答案，系统即时批改并调用LLM对错误答案进行错因分析（分析错在哪一步、属于哪种错误类型）。同时更新BKT模型中的知识点掌握度。 |
| **认证**           | 需要 JWT Token                                                            |
| **Content-Type** | `application/json`                                                      |

### 请求参数

#### 请求体（Request Body）

| 参数名              | 类型              | 必填 | 说明                     |
|------------------|-----------------|----|------------------------|
| pathId           | Long            | 是  | 学习路径ID                 |
| nodeId           | String          | 是  | 节点ID                   |
| exerciseId       | String          | 是  | 练习题ID                  |
| answer           | String          | 是  | 学生提交的答案                |
| solvingSteps     | Array\<String\> | 否  | 学生的解题步骤记录（用于LLM分析解题过程） |
| timeSpentSeconds | Integer         | 否  | 答题用时（秒）                |

### 请求体示例

```json
{
  "pathId": 2001,
  "nodeId": "node_001",
  "exerciseId": "ex_002",
  "answer": "B",
  "solvingSteps": [
    "f(1+Δx) = (1+Δx)^2 = 1 + 2Δx + Δx^2",
    "f(1) = 1",
    "平均变化率 = (f(1+Δx) - f(1)) / Δx = (2Δx + Δx^2) / Δx",
    "= 2Δx"
  ],
  "timeSpentSeconds": 120
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "exerciseId": "ex_002",
    "isCorrect": false,
    "correctAnswer": "A",
    "studentAnswer": "B",
    "score": 0,
    "errorAnalysis": {
      "errorType": "calculation",
      "errorSubType": "约分错误",
      "errorStep": 4,
      "errorDescription": "在最后一步约分时，将 $(2\\Delta x + (\\Delta x)^2) / \\Delta x$ 错误地化简为 $2\\Delta x$。正确做法是分子每一项都除以 $\\Delta x$：$\\frac{2\\Delta x}{\\Delta x} + \\frac{(\\Delta x)^2}{\\Delta x} = 2 + \\Delta x$。",
      "suggestion": "约分时注意分子的每一项都需要除以分母，不能只对部分项进行约分。建议逐项展开后再约分。"
    },
    "correctSolution": "$$\\frac{(1+\\Delta x)^2 - 1}{\\Delta x} = \\frac{1 + 2\\Delta x + (\\Delta x)^2 - 1}{\\Delta x} = \\frac{2\\Delta x + (\\Delta x)^2}{\\Delta x} = 2 + \\Delta x$$\n\n答案选A。",
    "masteryUpdate": {
      "before": 0.25,
      "after": 0.22,
      "delta": -0.03
    },
    "encouragement": "这道题的关键在于多项式除以单项式要逐项相除，下次注意这个细节就好了！"
  }
}
```

### 状态码说明

| 状态码 | 说明                            |
|-----|-------------------------------|
| 200 | 提交成功，批改完成                     |
| 400 | 参数错误（如exerciseId不存在、answer为空） |
| 401 | 未认证或Token过期                   |
| 403 | 无权访问该学习路径                     |
| 404 | 路径、节点或练习题不存在                  |
| 423 | 该节点尚未解锁                       |
| 500 | 服务器内部错误                       |

---

## 6. 提交检查点测试

提交检查点测试答案（3-5题），判断学生是否达标（掌握度>=0.8），未通过将触发重新诊断。

### 基本信息

| 项目               | 内容                                                                                     |
|------------------|----------------------------------------------------------------------------------------|
| **路径**           | `/api/learning/checkpoint/submit`                                                      |
| **方法**           | `POST`                                                                                 |
| **说明**           | 提交知识点学习后的检查点测试（3-5题）。系统综合评估学生掌握度，判断是否达标（掌握度≥0.8）。通过则解锁下一节点；未通过则触发评估Agent重新诊断，可能调整学习路径。 |
| **认证**           | 需要 JWT Token                                                                           |
| **Content-Type** | `application/json`                                                                     |

### 请求参数

#### 请求体（Request Body）

| 参数名                        | 类型              | 必填 | 说明         |
|----------------------------|-----------------|----|------------|
| pathId                     | Long            | 是  | 学习路径ID     |
| nodeId                     | String          | 是  | 节点ID       |
| answers                    | Array\<Object\> | 是  | 答案数组（3-5题） |
| answers[].questionId       | String          | 是  | 题目ID       |
| answers[].answer           | String          | 是  | 学生答案       |
| answers[].solvingSteps     | Array\<String\> | 否  | 解题步骤       |
| answers[].timeSpentSeconds | Integer         | 否  | 答题用时（秒）    |
| totalTimeSeconds           | Integer         | 否  | 测试总用时（秒）   |

### 请求体示例

```json
{
  "pathId": 2001,
  "nodeId": "node_001",
  "answers": [
    {
      "questionId": "cp_001",
      "answer": "4",
      "solvingSteps": ["f(3)=9, f(1)=1", "(9-1)/(3-1)=4"],
      "timeSpentSeconds": 60
    },
    {
      "questionId": "cp_002",
      "answer": "A",
      "timeSpentSeconds": 45
    },
    {
      "questionId": "cp_003",
      "answer": "v(2)=12",
      "solvingSteps": ["s(t)=3t^2", "平均变化率=(s(2+Δt)-s(2))/Δt", "=(3(2+Δt)^2-12)/Δt", "=12+3Δt", "当Δt→0时，瞬时速度=12"],
      "timeSpentSeconds": 180
    },
    {
      "questionId": "cp_004",
      "answer": "C",
      "timeSpentSeconds": 50
    }
  ],
  "totalTimeSeconds": 335
}
```

### 响应体示例（通过）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "nodeId": "node_001",
    "knowledgePointId": "math_average_rate_of_change",
    "knowledgePointName": "平均变化率与瞬时变化率",
    "passed": true,
    "score": 0.85,
    "masteryAfter": 0.82,
    "masteryThreshold": 0.80,
    "totalQuestions": 4,
    "correctCount": 3,
    "results": [
      {
        "questionId": "cp_001",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_002",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_003",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_004",
        "isCorrect": false,
        "score": 0.0,
        "errorAnalysis": {
          "errorType": "concept",
          "errorSubType": "概念混淆",
          "errorDescription": "混淆了平均变化率和瞬时变化率的区别，在区间足够小时二者近似但不相等。"
        }
      }
    ],
    "nextAction": {
      "type": "unlock_next_node",
      "nextNodeId": "node_002",
      "nextKnowledgePointName": "导数的概念与几何意义",
      "message": "恭喜！你已掌握「平均变化率与瞬时变化率」，接下来学习「导数的概念与几何意义」。"
    },
    "pathProgress": 0.25,
    "completedAt": "2026-05-23T15:20:00"
  }
}
```

### 响应体示例（未通过）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "nodeId": "node_001",
    "knowledgePointId": "math_average_rate_of_change",
    "knowledgePointName": "平均变化率与瞬时变化率",
    "passed": false,
    "score": 0.50,
    "masteryAfter": 0.55,
    "masteryThreshold": 0.80,
    "totalQuestions": 4,
    "correctCount": 2,
    "results": [
      {
        "questionId": "cp_001",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_002",
        "isCorrect": false,
        "score": 0.0,
        "errorAnalysis": {
          "errorType": "concept",
          "errorSubType": "定义理解偏差",
          "errorDescription": "对平均变化率的几何意义（割线斜率）理解不清。"
        }
      },
      {
        "questionId": "cp_003",
        "isCorrect": false,
        "score": 0.0,
        "errorAnalysis": {
          "errorType": "calculation",
          "errorSubType": "运算法则错误",
          "errorDescription": "展开 $(2+\\Delta t)^2$ 时遗漏了交叉项 $2 \\cdot 2 \\cdot \\Delta t$。"
        }
      },
      {
        "questionId": "cp_004",
        "isCorrect": true,
        "score": 1.0
      }
    ],
    "nextAction": {
      "type": "re_diagnose",
      "message": "检查点未通过，系统将重新诊断该知识点的薄弱环节，为你调整学习内容。",
      "reDiagnosisReason": "掌握度0.55未达到阈值0.80，主要问题集中在几何意义理解和代数展开计算。",
      "suggestedActions": [
        "重新学习割线与切线的几何直观",
        "加强完全平方公式的计算练习"
      ]
    },
    "pathProgress": 0.00,
    "completedAt": null
  }
}
```

### 状态码说明

| 状态码 | 说明                          |
|-----|-----------------------------|
| 200 | 提交成功（无论是否通过检查点）             |
| 400 | 参数错误（如答案数组为空、题目数量不满足3-5题要求） |
| 401 | 未认证或Token过期                 |
| 403 | 无权访问该学习路径                   |
| 404 | 路径或节点不存在                    |
| 423 | 该节点尚未解锁或尚未完成课程学习            |
| 500 | 服务器内部错误                     |

---

## 7. SSE流式AI对话辅导

通过Server-Sent Events实现流式AI对话辅导，结合学生画像和当前学习上下文提供个性化回答。

### 基本信息

| 项目                   | 内容                                                                                                                                      |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **路径**               | `/api/chat/stream`                                                                                                                      |
| **方法**               | `GET`                                                                                                                                   |
| **说明**               | 基于SSE（Server-Sent Events）实现流式AI对话辅导。系统结合学生五维画像（知识状态、错误模式、认知风格等）和当前学习上下文（正在学习的知识点、最近的错题等），生成个性化的辅导回答。每个SSE事件携带一个token，前端逐token渲染实现打字机效果。 |
| **认证**               | 需要 JWT Token                                                                                                                            |
| **Content-Type（响应）** | `text/event-stream`                                                                                                                     |

### 请求参数

#### Query参数

| 参数名       | 类型     | 必填 | 说明                       |
|-----------|--------|----|--------------------------|
| message   | String | 是  | 学生发送的消息内容                |
| sessionId | String | 否  | 会话ID（用于多轮对话，首次对话不传则自动创建） |
| pathId    | Long   | 否  | 当前学习路径ID（提供学习上下文）        |
| nodeId    | String | 否  | 当前学习节点ID（提供知识点上下文）       |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/chat/stream?message=什么是瞬时变化率？和平均变化率有什么区别？&sessionId=sess_abc123&pathId=2001&nodeId=node_001
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Accept: text/event-stream
```

### 响应格式说明

响应为 `text/event-stream` 格式，每个事件包含一个token或控制信息：

**事件类型：**

| 事件类型       | 说明                         |
|------------|----------------------------|
| `message`  | 正常的文本token                 |
| `metadata` | 对话元数据（会话ID、上下文信息等，在流开始时发送） |
| `done`     | 流式输出结束信号                   |
| `error`    | 错误信息                       |

### 响应体示例（SSE流）

```
event: metadata
data: {"sessionId":"sess_abc123","knowledgeContext":"平均变化率与瞬时变化率","studentLevel":"basic_consolidation"}

event: message
data: {"token":"好"}

event: message
data: {"token":"问题"}

event: message
data: {"token":"！"}

event: message
data: {"token":"我"}

event: message
data: {"token":"用"}

event: message
data: {"token":"一个"}

event: message
data: {"token":"生活"}

event: message
data: {"token":"中的"}

event: message
data: {"token":"例子"}

event: message
data: {"token":"来帮"}

event: message
data: {"token":"你"}

event: message
data: {"token":"理解"}

event: message
data: {"token":"：\n\n"}

event: message
data: {"token":"想象"}

event: message
data: {"token":"你"}

event: message
data: {"token":"坐"}

event: message
data: {"token":"高铁"}

event: message
data: {"token":"从"}

event: message
data: {"token":"北京"}

event: message
data: {"token":"到"}

event: message
data: {"token":"上海"}

event: message
data: {"token":"，"}

event: message
data: {"token":"全程"}

event: message
data: {"token":"1318"}

event: message
data: {"token":"公里"}

event: message
data: {"token":"用了"}

event: message
data: {"token":"4.5"}

event: message
data: {"token":"小时"}

event: message
data: {"token":"。\n\n"}

event: message
data: {"token":"**平均变化率**"}

event: message
data: {"token":"就是"}

event: message
data: {"token":"整段"}

event: message
data: {"token":"旅程的"}

event: message
data: {"token":"平均"}

event: message
data: {"token":"速度"}

event: message
data: {"token":"：$\\frac{1318}{4.5} \\approx 293$ km/h\n\n"}

event: message
data: {"token":"**瞬时变化率**"}

event: message
data: {"token":"就是"}

event: message
data: {"token":"车厢"}

event: message
data: {"token":"显示屏上"}

event: message
data: {"token":"实时"}

event: message
data: {"token":"显示的"}

event: message
data: {"token":"速度"}

event: message
data: {"token":"，比如"}

event: message
data: {"token":"某一刻"}

event: message
data: {"token":"显示"}

event: message
data: {"token":"350 km/h"}

event: message
data: {"token":"。\n\n"}

event: message
data: {"token":"数学"}

event: message
data: {"token":"上的"}

event: message
data: {"token":"区别"}

event: message
data: {"token":"：\n- "}

event: message
data: {"token":"平均变化率"}

event: message
data: {"token":"= $\\frac{f(b)-f(a)}{b-a}$，描述一段区间上的整体变化趋势\n- "}

event: message
data: {"token":"瞬时变化率"}

event: message
data: {"token":"= $\\lim_{\\Delta x \\to 0}\\frac{f(x+\\Delta x)-f(x)}{\\Delta x}$，描述某一点的精确变化快慢"}

event: done
data: {"sessionId":"sess_abc123","totalTokens":256,"messageId":"msg_20260523_001"}

```

### 前端处理示例

```javascript
const eventSource = new EventSource(
  '/api/chat/stream?message=什么是瞬时变化率&sessionId=sess_abc123&pathId=2001&nodeId=node_001',
  { headers: { 'Authorization': 'Bearer ' + token } }
);

eventSource.addEventListener('message', (event) => {
  const data = JSON.parse(event.data);
  appendToChat(data.token); // 逐token追加到对话界面
});

eventSource.addEventListener('done', (event) => {
  eventSource.close();
});

eventSource.addEventListener('error', (event) => {
  console.error('SSE连接出错');
  eventSource.close();
});
```

### 状态码说明

| 状态码 | 说明                 |
|-----|--------------------|
| 200 | SSE连接建立成功，开始流式输出   |
| 400 | 参数错误（如message为空）   |
| 401 | 未认证或Token过期        |
| 429 | 请求过于频繁（限流）         |
| 500 | 服务器内部错误（如LLM服务不可用） |

### 注意事项

- 客户端需要设置 `Accept: text/event-stream` 请求头
- 连接超时时间建议设为60秒以上（LLM生成可能较慢）
- 异常断开时，可通过 `sessionId` 恢复上下文继续对话
- 每个SSE事件的 `data` 字段为JSON格式字符串
- 对话内容支持LaTeX数学公式（以 `$...$` 或 `$$...$$` 包裹）

---

## 8. 获取对话历史记录

获取学生的AI对话辅导历史记录，支持按会话查询。

### 基本信息

| 项目     | 内容                                                          |
|--------|-------------------------------------------------------------|
| **路径** | `/api/chat/history`                                         |
| **方法** | `GET`                                                       |
| **说明** | 获取学生与AI对话辅导的历史记录。支持按会话ID查询特定对话，也可获取所有会话列表。对话记录保存用于复习和上下文关联。 |
| **认证** | 需要 JWT Token                                                |

### 请求参数

#### Query参数

| 参数名       | 类型      | 必填 | 说明                             |
|-----------|---------|----|--------------------------------|
| sessionId | String  | 否  | 会话ID，传入则返回该会话的完整对话记录；不传则返回会话列表 |
| pathId    | Long    | 否  | 学习路径ID筛选（只返回与指定学习路径相关的对话）      |
| page      | Integer | 否  | 页码，默认1                         |
| size      | Integer | 否  | 每页条数，默认20                      |

### 请求体示例

无请求体（GET请求）。

请求示例（获取特定会话详情）：

```
GET /api/chat/history?sessionId=sess_abc123
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

请求示例（获取会话列表）：

```
GET /api/chat/history?page=1&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例（会话列表，不传sessionId时）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 15,
    "page": 1,
    "size": 20,
    "sessions": [
      {
        "sessionId": "sess_abc123",
        "title": "关于瞬时变化率的疑问",
        "relatedPath": {
          "pathId": 2001,
          "knowledgePointName": "平均变化率与瞬时变化率"
        },
        "messageCount": 6,
        "lastMessage": "谢谢老师，我明白了！",
        "createdAt": "2026-05-23T10:30:00",
        "lastActiveAt": "2026-05-23T10:45:00"
      },
      {
        "sessionId": "sess_def456",
        "title": "导数计算公式推导",
        "relatedPath": {
          "pathId": 2001,
          "knowledgePointName": "导数的计算"
        },
        "messageCount": 4,
        "lastMessage": "那复合函数的求导法则呢？",
        "createdAt": "2026-05-22T19:00:00",
        "lastActiveAt": "2026-05-22T19:20:00"
      }
    ]
  }
}
```

### 响应体示例（特定会话详情，传入sessionId时）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "sess_abc123",
    "title": "关于瞬时变化率的疑问",
    "relatedPath": {
      "pathId": 2001,
      "targetKnowledgePoint": "导数的应用",
      "currentNode": "平均变化率与瞬时变化率"
    },
    "messages": [
      {
        "messageId": "msg_001",
        "role": "student",
        "content": "什么是瞬时变化率？和平均变化率有什么区别？",
        "timestamp": "2026-05-23T10:30:00"
      },
      {
        "messageId": "msg_002",
        "role": "ai",
        "content": "好问题！我用一个生活中的例子来帮你理解：\n\n想象你坐高铁从北京到上海，全程1318公里用了4.5小时。\n\n**平均变化率**就是整段旅程的平均速度：$\\frac{1318}{4.5} \\approx 293$ km/h\n\n**瞬时变化率**就是车厢显示屏上实时显示的速度，比如某一刻显示350 km/h。\n\n数学上的区别：\n- 平均变化率 = $\\frac{f(b)-f(a)}{b-a}$，描述一段区间上的整体变化趋势\n- 瞬时变化率 = $\\lim_{\\Delta x \\to 0}\\frac{f(x+\\Delta x)-f(x)}{\\Delta x}$，描述某一点的精确变化快慢",
        "timestamp": "2026-05-23T10:30:15"
      },
      {
        "messageId": "msg_003",
        "role": "student",
        "content": "那瞬时变化率是不是就是导数？",
        "timestamp": "2026-05-23T10:32:00"
      },
      {
        "messageId": "msg_004",
        "role": "ai",
        "content": "没错！你的直觉很好。\n\n**瞬时变化率就是导数的本质含义。**\n\n准确地说：函数 $f(x)$ 在 $x=x_0$ 处的导数 $f'(x_0)$ 就是函数在该点的瞬时变化率。\n\n$$f'(x_0) = \\lim_{\\Delta x \\to 0}\\frac{f(x_0+\\Delta x)-f(x_0)}{\\Delta x}$$\n\n几何意义上：\n- 平均变化率 → 割线的斜率\n- 瞬时变化率（导数）→ 切线的斜率\n\n当割线的两个点无限接近时，割线就变成了切线，平均变化率就变成了瞬时变化率（导数）。",
        "timestamp": "2026-05-23T10:32:20"
      },
      {
        "messageId": "msg_005",
        "role": "student",
        "content": "明白了，那导数为0代表什么？",
        "timestamp": "2026-05-23T10:35:00"
      },
      {
        "messageId": "msg_006",
        "role": "ai",
        "content": "很好的追问！\n\n导数为0（即 $f'(x_0) = 0$）意味着**函数在 $x_0$ 处的瞬时变化率为零**——函数值在这一瞬间"停止变化"了。\n\n几何上：切线斜率为0，即**切线是水平的**。\n\n这种点通常出现在：\n1. **极大值点**：函数从递增变为递减，"山顶"\n2. **极小值点**：函数从递减变为递增，"山谷"\n3. **拐点（驻点但非极值）**：如 $f(x)=x^3$ 在 $x=0$ 处\n\n⚠️ 注意：$f'(x_0)=0$ 是极值的**必要不充分条件**——导数为0不一定是极值点（如 $x^3$ 在原点），但极值点的导数一定为0（在导数存在的情况下）。\n\n这个知识点在后面学"导数与单调性""极值与最值"时会深入学习！",
        "timestamp": "2026-05-23T10:35:25"
      }
    ],
    "totalMessages": 6,
    "createdAt": "2026-05-23T10:30:00",
    "lastActiveAt": "2026-05-23T10:35:25"
  }
}
```

### 状态码说明

| 状态码 | 说明              |
|-----|-----------------|
| 200 | 请求成功            |
| 401 | 未认证或Token过期     |
| 404 | 指定的sessionId不存在 |
| 500 | 服务器内部错误         |

---

## 附录

### A. 节点状态（Node Status）枚举

| 状态值           | 说明           |
|---------------|--------------|
| `locked`      | 锁定状态，前置节点未完成 |
| `pending`     | 待学习，已解锁但尚未开始 |
| `in_progress` | 学习中          |
| `completed`   | 已完成（检查点通过）   |
| `failed`      | 检查点未通过，待重新学习 |

### B. 教学策略（Teaching Strategy）枚举

| 策略标识                  | 中文名称 | 触发条件（掌握度） | 教学方式               |
|-----------------------|------|-----------|--------------------|
| `basic_consolidation` | 基础夯实 | < 0.4     | 概念重讲 + 直观案例 + 基础练习 |
| `strengthening`       | 强化突破 | 0.4 - 0.7 | 错因针对性讲解 + 变式训练     |
| `expansion`           | 拓展提升 | > 0.7     | 综合应用 + 高考真题 + 能力迁移 |

### C. 错误类型（Error Type）枚举

| 错误大类  | 标识              | 子类示例               |
|-------|-----------------|--------------------|
| 概念性错误 | `concept`       | 定义理解偏差、概念混淆        |
| 计算性错误 | `calculation`   | 符号错误、运算法则错误、精度错误   |
| 逻辑性错误 | `logic`         | 充要条件误判、分类讨论遗漏、推理跳步 |
| 方法性错误 | `method`        | 方法选择不当、转化方向错误      |
| 审题性错误 | `comprehension` | 条件遗漏、问题误读          |

### D. 学习路径状态（Path Status）枚举

| 状态值         | 说明             |
|-------------|----------------|
| `active`    | 进行中            |
| `completed` | 已完成（所有节点通过检查点） |
| `paused`    | 已暂停            |

### E. 认知风格（Cognitive Style）枚举

| 标识        | 说明  | 教学内容适配           |
|-----------|-----|------------------|
| `visual`  | 视觉型 | 多用图表、函数图像、几何图形辅助 |
| `logical` | 逻辑型 | 多用严格推导、形式化证明     |
| `example` | 案例型 | 多用生活实例、具体数值计算引入  |
| `formula` | 公式型 | 多用公式归纳、结构化总结     |

---

<!-- Source: api-teacher-report.md -->

# SmartMentor API 文档 —— 教师端、效果报告与学习激励模块

> 本文档覆盖教师端班级管理、班级热力图、分层作业、学生预警、周报，以及学生端效果报告和学习激励相关接口。

---

## 全局约定

### 基础路径

```
https://{host}/api
```

### 认证方式

所有接口均需在请求头中携带 JWT Token：

```
Authorization: Bearer {token}
```

- 教师端接口（`/api/teacher/**`）要求用户角色为 **TEACHER**，否则返回 `403 Forbidden`。
- 学生端接口（`/api/report/**`、`/api/engagement/**`）要求用户角色为 **STUDENT**。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 通用错误码

| 状态码 | code | 说明             |
|-----|------|----------------|
| 200 | 200  | 请求成功           |
| 400 | 400  | 请求参数错误         |
| 401 | 401  | 未认证 / Token 过期 |
| 403 | 403  | 角色权限不足         |
| 404 | 404  | 资源不存在          |
| 500 | 500  | 服务器内部错误        |

---

## 一、教师端接口

---

### 1. 获取班级学生列表

获取当前教师所管理班级的学生列表及其学习概况。

- **路径**：`/api/teacher/class/students`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型      | 必填 | 说明                   |
|-----------|---------|----|----------------------|
| className | String  | 否  | 班级名称，不传则返回该教师所有班级的学生 |
| page      | Integer | 否  | 页码，默认 1              |
| pageSize  | Integer | 否  | 每页条数，默认 20，最大 100    |
| keyword   | String  | 否  | 搜索关键词，匹配学生姓名或用户名     |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 42,
    "page": 1,
    "pageSize": 20,
    "students": [
      {
        "studentId": 1001,
        "username": "xiaoming",
        "nickname": "小明",
        "grade": "高二",
        "school": "市第一中学",
        "className": "高二3班",
        "overallMastery": 0.65,
        "streakDays": 12,
        "level": 5,
        "lastActiveAt": "2026-05-22T18:30:00",
        "weakModules": ["导数", "解析几何"]
      },
      {
        "studentId": 1002,
        "username": "xiaohong",
        "nickname": "小红",
        "grade": "高二",
        "school": "市第一中学",
        "className": "高二3班",
        "overallMastery": 0.82,
        "streakDays": 30,
        "level": 8,
        "lastActiveAt": "2026-05-23T09:15:00",
        "weakModules": ["数列"]
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明         |
|-----|------------|
| 200 | 成功返回学生列表   |
| 401 | 未认证        |
| 403 | 非教师角色，无权访问 |

---

### 2. 添加学生到班级

将一个或多个学生添加到教师管理的班级中。

- **路径**：`/api/teacher/class/students`
- **方法**：`POST`
- **角色要求**：TEACHER
- **Content-Type**：`application/json`

#### 请求参数（Body）

| 参数名        | 类型       | 必填 | 说明                             |
|------------|----------|----|--------------------------------|
| className  | String   | 是  | 目标班级名称                         |
| studentIds | Long[]   | 否  | 学生ID列表（与 usernames 二选一，至少提供一个） |
| usernames  | String[] | 否  | 学生用户名列表（与 studentIds 二选一）      |

#### 请求体示例 JSON

```json
{
  "className": "高二3班",
  "studentIds": [1001, 1002, 1003],
  "usernames": []
}
```

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "added": 2,
    "skipped": 1,
    "details": [
      { "studentId": 1001, "status": "added" },
      { "studentId": 1002, "status": "added" },
      { "studentId": 1003, "status": "already_exists" }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                              |
|-----|---------------------------------|
| 200 | 添加完成（含部分已存在的情况）                 |
| 400 | 参数缺失，studentIds 和 usernames 均为空 |
| 401 | 未认证                             |
| 403 | 非教师角色，无权操作                      |
| 404 | 指定的学生ID不存在                      |

---

### 3. 班级薄弱知识热力图

返回各知识点在班级中的平均掌握度，用于可视化薄弱知识热力图。颜色编码规则：红色（< 0.5）、黄色（0.5 - 0.8）、绿色（>= 0.8）。

- **路径**：`/api/teacher/class/heatmap`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型     | 必填 | 说明                                         |
|-----------|--------|----|--------------------------------------------|
| className | String | 否  | 班级名称，不传则汇总该教师所有班级                          |
| module    | String | 否  | 数学模块筛选，如"导数""解析几何"，不传返回全部模块                |
| groupBy   | String | 否  | 分组方式：`none`（默认，班级整体）/ `level`（按优秀/中等/薄弱分组） |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "className": "高二3班",
    "totalStudents": 42,
    "generatedAt": "2026-05-23T10:00:00",
    "colorRule": {
      "red": "< 0.5",
      "yellow": "0.5 - 0.8",
      "green": ">= 0.8"
    },
    "modules": [
      {
        "module": "函数",
        "avgMastery": 0.72,
        "color": "yellow",
        "knowledgePoints": [
          {
            "knowledgePointId": "math_function_concept",
            "name": "函数的概念与表示法",
            "avgMastery": 0.85,
            "color": "green",
            "weakStudentCount": 3
          },
          {
            "knowledgePointId": "math_function_monotonicity",
            "name": "单调性与最值",
            "avgMastery": 0.68,
            "color": "yellow",
            "weakStudentCount": 12
          }
        ]
      },
      {
        "module": "导数",
        "avgMastery": 0.43,
        "color": "red",
        "knowledgePoints": [
          {
            "knowledgePointId": "math_derivative_concept",
            "name": "导数的概念与几何意义",
            "avgMastery": 0.55,
            "color": "yellow",
            "weakStudentCount": 18
          },
          {
            "knowledgePointId": "math_derivative_application",
            "name": "导数的应用",
            "avgMastery": 0.35,
            "color": "red",
            "weakStudentCount": 28
          }
        ]
      },
      {
        "module": "三角函数",
        "avgMastery": 0.38,
        "color": "red",
        "knowledgePoints": [
          {
            "knowledgePointId": "math_trig_definition",
            "name": "三角函数定义",
            "avgMastery": 0.60,
            "color": "yellow",
            "weakStudentCount": 15
          },
          {
            "knowledgePointId": "math_trig_sum_diff",
            "name": "和差角公式",
            "avgMastery": 0.30,
            "color": "red",
            "weakStudentCount": 32
          }
        ]
      }
    ],
    "groups": [
      {
        "groupName": "A组(优秀)",
        "studentCount": 10,
        "moduleMasteries": {
          "函数": 0.91,
          "导数": 0.72,
          "三角函数": 0.65
        }
      },
      {
        "groupName": "B组(中等)",
        "studentCount": 20,
        "moduleMasteries": {
          "函数": 0.70,
          "导数": 0.42,
          "三角函数": 0.38
        }
      },
      {
        "groupName": "C组(薄弱)",
        "studentCount": 12,
        "moduleMasteries": {
          "函数": 0.48,
          "导数": 0.22,
          "三角函数": 0.18
        }
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明         |
|-----|------------|
| 200 | 成功返回热力图数据  |
| 401 | 未认证        |
| 403 | 非教师角色，无权访问 |

---

### 4. AI 生成分层作业

根据班级学生掌握度自动生成基础 / 提高 / 拓展三层难度的作业。系统根据每位学生的知识点掌握度自动将其分到对应层级。

分层规则：

- **基础层**：掌握度 < 0.5，题目侧重概念巩固
- **提高层**：掌握度 0.5 - 0.8，题目侧重综合应用
- **拓展层**：掌握度 > 0.8，题目侧重高考真题 / 竞赛变式

- **路径**：`/api/teacher/homework/generate`
- **方法**：`POST`
- **角色要求**：TEACHER
- **Content-Type**：`application/json`

#### 请求参数（Body）

| 参数名               | 类型       | 必填 | 说明               |
|-------------------|----------|----|------------------|
| className         | String   | 是  | 班级名称             |
| knowledgePointIds | String[] | 是  | 目标知识点ID列表，至少1个   |
| questionsPerLayer | Integer  | 否  | 每层题目数，默认 5       |
| deadline          | String   | 否  | 截止时间，ISO 8601 格式 |
| note              | String   | 否  | 教师备注 / 附加要求      |

#### 请求体示例 JSON

```json
{
  "className": "高二3班",
  "knowledgePointIds": [
    "math_derivative_calc",
    "math_derivative_application"
  ],
  "questionsPerLayer": 5,
  "deadline": "2026-05-26T23:59:59",
  "note": "请注意导数计算中链式法则的考查"
}
```

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "homeworkId": "hw_20260523_001",
    "className": "高二3班",
    "knowledgePoints": ["导数的计算", "导数的应用"],
    "deadline": "2026-05-26T23:59:59",
    "generatedAt": "2026-05-23T10:05:00",
    "layers": [
      {
        "layer": "基础层",
        "description": "概念巩固，注重基础计算和定义理解",
        "masteryRange": "< 0.5",
        "assignedStudents": [
          { "studentId": 1005, "nickname": "小刚", "mastery": 0.32 },
          { "studentId": 1008, "nickname": "小丽", "mastery": 0.41 }
        ],
        "questions": [
          {
            "questionId": 501,
            "type": "choice",
            "difficulty": 0.3,
            "knowledgePointId": "math_derivative_calc",
            "content": "求函数 $f(x) = x^3 - 2x + 1$ 的导数 $f'(x)$ 为：",
            "options": [
              "A. $3x^2 - 2$",
              "B. $3x^2 + 2$",
              "C. $x^2 - 2$",
              "D. $3x - 2$"
            ],
            "answer": "A",
            "source": "textbook"
          },
          {
            "questionId": 502,
            "type": "fill",
            "difficulty": 0.35,
            "knowledgePointId": "math_derivative_calc",
            "content": "已知 $f(x) = \\sin x + \\cos x$，则 $f'(\\frac{\\pi}{4})$ = ______。",
            "answer": "0",
            "source": "textbook"
          }
        ]
      },
      {
        "layer": "提高层",
        "description": "综合应用，注重方法选择和规范解题",
        "masteryRange": "0.5 - 0.8",
        "assignedStudents": [
          { "studentId": 1001, "nickname": "小明", "mastery": 0.65 },
          { "studentId": 1003, "nickname": "小华", "mastery": 0.58 }
        ],
        "questions": [
          {
            "questionId": 503,
            "type": "solve",
            "difficulty": 0.6,
            "knowledgePointId": "math_derivative_application",
            "content": "已知函数 $f(x) = x^3 - 3x^2 + 4$，求 $f(x)$ 的单调递增区间和单调递减区间。",
            "answer": "单调递增区间为 $(-\\infty, 0)$ 和 $(2, +\\infty)$；单调递减区间为 $(0, 2)$。",
            "source": "mock"
          }
        ]
      },
      {
        "layer": "拓展层",
        "description": "高考真题与竞赛变式，注重创新思维与综合能力",
        "masteryRange": "> 0.8",
        "assignedStudents": [
          { "studentId": 1002, "nickname": "小红", "mastery": 0.88 },
          { "studentId": 1010, "nickname": "小强", "mastery": 0.92 }
        ],
        "questions": [
          {
            "questionId": 504,
            "type": "solve",
            "difficulty": 0.85,
            "knowledgePointId": "math_derivative_application",
            "content": "已知函数 $f(x) = e^x - ax - a$，讨论 $f(x)$ 零点的个数。（2024年全国甲卷）",
            "answer": "当 $a \\leq 0$ 时，$f(x)$ 有且仅有一个零点；当 $0 < a < 1$ 时无零点；当 $a = 1$ 时有一个零点；当 $a > 1$ 时有两个零点。",
            "source": "gaokao",
            "gaokaoYear": 2024
          }
        ]
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                |
|-----|-------------------|
| 200 | 分层作业生成成功          |
| 400 | 参数错误，如知识点ID列表为空   |
| 401 | 未认证               |
| 403 | 非教师角色，无权操作        |
| 404 | 指定班级不存在           |
| 500 | AI 生成失败（如大模型调用超时） |

---

### 5. 学生预警列表

获取需要教师关注的异常学生列表，包括连续不登录、成绩下滑、学习路径停滞等情况。

- **路径**：`/api/teacher/alerts`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型      | 必填 | 说明                                                                 |
|-----------|---------|----|--------------------------------------------------------------------|
| className | String  | 否  | 班级名称筛选                                                             |
| alertType | String  | 否  | 预警类型筛选：`inactive`（不活跃）/ `decline`（成绩下滑）/ `stagnant`（学习停滞）/ 不传则返回全部 |
| page      | Integer | 否  | 页码，默认 1                                                            |
| pageSize  | Integer | 否  | 每页条数，默认 20                                                         |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "pageSize": 20,
    "alerts": [
      {
        "alertId": "alert_001",
        "studentId": 1005,
        "nickname": "小刚",
        "className": "高二3班",
        "alertType": "inactive",
        "severity": "high",
        "title": "连续7天未登录",
        "description": "该学生上次登录时间为2026-05-16，已连续7天未登录系统，学习路径中断。",
        "lastActiveAt": "2026-05-16T20:30:00",
        "suggestion": "建议联系该学生了解情况，并鼓励其恢复学习。",
        "createdAt": "2026-05-23T08:00:00"
      },
      {
        "alertId": "alert_002",
        "studentId": 1008,
        "nickname": "小丽",
        "className": "高二3班",
        "alertType": "decline",
        "severity": "medium",
        "title": "导数模块掌握度下滑",
        "description": "该学生导数模块近两周掌握度从0.62下降至0.45，下降幅度27.4%。",
        "masteryBefore": 0.62,
        "masteryAfter": 0.45,
        "declineRate": 0.274,
        "suggestion": "建议关注其导数模块学习情况，可安排针对性辅导。",
        "createdAt": "2026-05-23T08:00:00"
      },
      {
        "alertId": "alert_003",
        "studentId": 1012,
        "nickname": "小伟",
        "className": "高二3班",
        "alertType": "stagnant",
        "severity": "low",
        "title": "学习路径停滞5天",
        "description": "该学生当前学习路径'三角函数突破'已5天无进展，进度停留在40%。",
        "pathName": "三角函数突破",
        "pathProgress": 0.40,
        "stagnantDays": 5,
        "suggestion": "建议了解学习卡点，该学生可能在'和差角公式'处遇到困难。",
        "createdAt": "2026-05-23T08:00:00"
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明         |
|-----|------------|
| 200 | 成功返回预警列表   |
| 401 | 未认证        |
| 403 | 非教师角色，无权访问 |

---

### 6. 班级学习情况周报

获取 AI 生成的班级学习情况周报，包含本周学习数据汇总、进步学生、需关注学生、班级薄弱点及教学建议。

- **路径**：`/api/teacher/report/weekly`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型     | 必填 | 说明                          |
|-----------|--------|----|-----------------------------|
| className | String | 是  | 班级名称                        |
| weekStart | String | 否  | 周起始日期，格式 `yyyy-MM-dd`，默认本周一 |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "className": "高二3班",
    "weekStart": "2026-05-18",
    "weekEnd": "2026-05-24",
    "generatedAt": "2026-05-23T10:30:00",
    "summary": {
      "totalStudents": 42,
      "activeStudents": 38,
      "activeRate": 0.905,
      "totalStudyHours": 156.5,
      "avgStudyHoursPerStudent": 4.12,
      "totalQuestionsAnswered": 1260,
      "avgAccuracy": 0.68,
      "avgMasteryChange": 0.05
    },
    "topProgressStudents": [
      {
        "studentId": 1001,
        "nickname": "小明",
        "masteryBefore": 0.55,
        "masteryAfter": 0.72,
        "improvement": 0.17,
        "highlights": "导数模块掌握度从0.40提升至0.68，进步显著"
      },
      {
        "studentId": 1015,
        "nickname": "小芳",
        "masteryBefore": 0.60,
        "masteryAfter": 0.75,
        "improvement": 0.15,
        "highlights": "完成三角函数学习路径，连续打卡7天"
      }
    ],
    "needAttentionStudents": [
      {
        "studentId": 1005,
        "nickname": "小刚",
        "issue": "本周未登录",
        "suggestion": "联系了解情况"
      },
      {
        "studentId": 1008,
        "nickname": "小丽",
        "issue": "导数掌握度下滑0.17",
        "suggestion": "安排课后辅导"
      }
    ],
    "classWeakPoints": [
      {
        "knowledgePointId": "math_trig_sum_diff",
        "name": "和差角公式",
        "avgMastery": 0.30,
        "color": "red",
        "suggestion": "建议课堂安排专项复习"
      },
      {
        "knowledgePointId": "math_derivative_application",
        "name": "导数的应用",
        "avgMastery": 0.38,
        "color": "red",
        "suggestion": "建议分层布置作业强化训练"
      }
    ],
    "aiTeachingSuggestion": "本周班级在导数和三角函数两个模块的表现较弱，建议下周课堂重点讲解'和差角公式'的推导与应用，并通过分层作业对导数应用进行强化训练。活跃度方面有4名学生本周未登录，建议及时了解情况。"
  }
}
```

#### 状态码说明

| 状态码 | 说明            |
|-----|---------------|
| 200 | 成功返回周报数据      |
| 400 | 参数错误，如日期格式不正确 |
| 401 | 未认证           |
| 403 | 非教师角色，无权访问    |
| 404 | 指定班级不存在       |

---

## 二、学生端 —— 效果报告接口

---

### 7. 学习效果报告

获取学生的学习效果报告，包含前测-后测对比、掌握度变化趋势、错误消除率和能力雷达图数据。对应项目设计中"四维效果评估"
体系（掌握提升、学习曲线、错误消除、能力雷达）。

- **路径**：`/api/report/effectiveness`
- **方法**：`GET`
- **角色要求**：STUDENT

#### 请求参数（Query）

| 参数名    | 类型     | 必填 | 说明                                                   |
|--------|--------|----|------------------------------------------------------|
| module | String | 否  | 数学模块筛选，如"导数""三角函数"，不传返回全部                            |
| period | String | 否  | 时间范围：`week`（近一周）/ `month`（近一月）/ `all`（全部），默认 `month` |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "generatedAt": "2026-05-23T11:00:00",
    "period": "month",
    "overallSummary": {
      "masteryBefore": 0.28,
      "masteryAfter": 0.82,
      "improvementRate": 1.928,
      "totalStudyHours": 18.5,
      "totalQuestionsAnswered": 156,
      "accuracy": 0.74
    },
    "masteryComparison": [
      {
        "knowledgePointId": "math_derivative_application",
        "name": "导数的应用",
        "module": "导数",
        "preTestMastery": 0.25,
        "postTestMastery": 0.82,
        "improvement": 0.57
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "name": "导数的计算",
        "module": "导数",
        "preTestMastery": 0.40,
        "postTestMastery": 0.88,
        "improvement": 0.48
      },
      {
        "knowledgePointId": "math_change_rate",
        "name": "平均变化率与瞬时变化率",
        "module": "导数",
        "preTestMastery": 0.20,
        "postTestMastery": 0.75,
        "improvement": 0.55
      }
    ],
    "masteryCurve": [
      { "date": "2026-04-23", "mastery": 0.28 },
      { "date": "2026-04-30", "mastery": 0.38 },
      { "date": "2026-05-07", "mastery": 0.52 },
      { "date": "2026-05-14", "mastery": 0.68 },
      { "date": "2026-05-21", "mastery": 0.82 }
    ],
    "errorElimination": [
      {
        "errorType": "计算性错误",
        "subType": "符号错误",
        "countBefore": 15,
        "countAfter": 3,
        "eliminationRate": 0.80
      },
      {
        "errorType": "概念性错误",
        "subType": "概念混淆",
        "countBefore": 8,
        "countAfter": 1,
        "eliminationRate": 0.875
      },
      {
        "errorType": "计算性错误",
        "subType": "运算法则错误",
        "countBefore": 6,
        "countAfter": 2,
        "eliminationRate": 0.667
      }
    ],
    "abilityRadar": {
      "dimensions": ["函数", "导数", "三角函数", "向量", "数列", "解析几何"],
      "before": [0.55, 0.28, 0.42, 0.60, 0.70, 0.35],
      "after":  [0.72, 0.82, 0.55, 0.68, 0.75, 0.50]
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明       |
|-----|----------|
| 200 | 成功返回效果报告 |
| 401 | 未认证      |
| 403 | 非学生角色    |

---

### 8. 学生首页仪表盘数据

获取学生首页仪表盘所需的全部数据，包含今日任务概要、连续打卡天数、各模块掌握度（雷达图数据）和最近学习动态。

- **路径**：`/api/report/dashboard`
- **方法**：`GET`
- **角色要求**：STUDENT

#### 请求参数（Query）

无。

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "nickname": "小明",
    "level": 5,
    "levelTitle": "数学进阶者",
    "experiencePoints": 2350,
    "nextLevelExp": 3000,
    "streakDays": 12,
    "todayStudyMinutes": 35,
    "totalStudyHours": 48.5,
    "todayTasks": {
      "total": 3,
      "completed": 1,
      "tasks": [
        {
          "missionId": "mission_daily_001",
          "title": "完成1组诊断测试",
          "type": "diagnostic",
          "status": "completed",
          "rewardExp": 50
        },
        {
          "missionId": "mission_daily_002",
          "title": "学习路径推进1个知识点",
          "type": "learning",
          "status": "pending",
          "rewardExp": 80
        },
        {
          "missionId": "mission_daily_003",
          "title": "复习昨日错题",
          "type": "review",
          "status": "pending",
          "rewardExp": 30
        }
      ]
    },
    "moduleMastery": {
      "dimensions": ["函数", "导数", "三角函数", "向量", "数列", "解析几何"],
      "values": [0.72, 0.65, 0.55, 0.68, 0.75, 0.42]
    },
    "recentActivities": [
      {
        "date": "2026-05-23",
        "activities": [
          {
            "type": "diagnostic",
            "description": "完成导数模块诊断测试",
            "duration": "15分钟",
            "result": "掌握度从0.60提升至0.65"
          }
        ]
      },
      {
        "date": "2026-05-22",
        "activities": [
          {
            "type": "lesson",
            "description": "学习知识点：导数的计算",
            "duration": "25分钟",
            "result": "通过检查点测试"
          },
          {
            "type": "exercise",
            "description": "完成导数计算练习",
            "duration": "20分钟",
            "result": "正确率 4/5"
          }
        ]
      }
    ],
    "currentPath": {
      "pathId": 101,
      "name": "导数突破之路",
      "progress": 0.60,
      "currentNode": "导数与单调性",
      "totalNodes": 5,
      "completedNodes": 3
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明        |
|-----|-----------|
| 200 | 成功返回仪表盘数据 |
| 401 | 未认证       |
| 403 | 非学生角色     |

---

## 三、学生端 —— 学习激励接口

---

### 9. 每日任务列表

获取当日的任务清单。每天系统自动生成3个可选任务（诊断 / 练习 / 复习），完成后可获得经验值。

- **路径**：`/api/engagement/missions`
- **方法**：`GET`
- **角色要求**：STUDENT

#### 请求参数（Query）

| 参数名  | 类型     | 必填 | 说明                        |
|------|--------|----|---------------------------|
| date | String | 否  | 查询日期，格式 `yyyy-MM-dd`，默认今天 |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "date": "2026-05-23",
    "streakDays": 12,
    "todayExpEarned": 50,
    "todayExpAvailable": 160,
    "missions": [
      {
        "missionId": "mission_20260523_diag",
        "title": "完成1组诊断测试",
        "description": "选择任意模块完成一次诊断测试（5-8题）",
        "type": "diagnostic",
        "rewardExp": 50,
        "status": "completed",
        "completedAt": "2026-05-23T09:30:00"
      },
      {
        "missionId": "mission_20260523_learn",
        "title": "学习路径推进1个知识点",
        "description": "在当前学习路径中完成一个知识点的学习并通过检查点测试",
        "type": "learning",
        "rewardExp": 80,
        "status": "pending",
        "completedAt": null
      },
      {
        "missionId": "mission_20260523_review",
        "title": "复习昨日错题",
        "description": "重做昨天做错的题目，巩固薄弱点",
        "type": "review",
        "rewardExp": 30,
        "status": "pending",
        "completedAt": null
      }
    ],
    "streakBonus": {
      "currentStreak": 12,
      "nextMilestone": 14,
      "nextMilestoneReward": "额外100经验值 + '两周坚持'成就勋章"
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明       |
|-----|----------|
| 200 | 成功返回任务列表 |
| 401 | 未认证      |
| 403 | 非学生角色    |

---

### 10. 完成任务获得经验值

标记指定任务为已完成，系统验证完成条件后发放经验值奖励，并更新学生成长等级。

- **路径**：`/api/engagement/missions/{missionId}/complete`
- **方法**：`POST`
- **角色要求**：STUDENT

#### 请求参数

**路径参数**：

| 参数名       | 类型     | 必填 | 说明   |
|-----------|--------|----|------|
| missionId | String | 是  | 任务ID |

**请求体参数（Body）**：

| 参数名                       | 类型     | 必填 | 说明                         |
|---------------------------|--------|----|----------------------------|
| evidence                  | Object | 否  | 完成凭据，系统也会自动校验，此字段用于客户端辅助上报 |
| evidence.sessionId        | String | 否  | 关联的诊断/学习会话ID               |
| evidence.knowledgePointId | String | 否  | 关联的知识点ID                   |

#### 请求体示例 JSON

```json
{
  "evidence": {
    "sessionId": "diag_session_20260523_001",
    "knowledgePointId": "math_derivative_calc"
  }
}
```

#### 响应体示例 JSON

**成功完成**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "missionId": "mission_20260523_learn",
    "status": "completed",
    "completedAt": "2026-05-23T14:20:00",
    "rewardExp": 80,
    "expSummary": {
      "previousExp": 2350,
      "earnedExp": 80,
      "currentExp": 2430,
      "nextLevelExp": 3000
    },
    "levelUp": false,
    "currentLevel": 5,
    "currentLevelTitle": "数学进阶者",
    "streakDays": 12,
    "newAchievements": [],
    "todayProgress": {
      "completedMissions": 2,
      "totalMissions": 3,
      "todayTotalExp": 130
    }
  }
}
```

**触发升级和成就**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "missionId": "mission_20260523_review",
    "status": "completed",
    "completedAt": "2026-05-23T20:10:00",
    "rewardExp": 30,
    "expSummary": {
      "previousExp": 2980,
      "earnedExp": 30,
      "bonusExp": 100,
      "currentExp": 3110,
      "nextLevelExp": 4500
    },
    "levelUp": true,
    "previousLevel": 5,
    "currentLevel": 6,
    "currentLevelTitle": "数学挑战者",
    "streakDays": 14,
    "newAchievements": [
      {
        "achievementId": "ach_streak_14",
        "title": "两周坚持",
        "description": "连续学习14天",
        "icon": "streak_14",
        "bonusExp": 100
      }
    ],
    "todayProgress": {
      "completedMissions": 3,
      "totalMissions": 3,
      "todayTotalExp": 260
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明                    |
|-----|-----------------------|
| 200 | 任务完成，经验值发放成功          |
| 400 | 任务已完成，不可重复提交；或完成条件不满足 |
| 401 | 未认证                   |
| 403 | 非学生角色                 |
| 404 | 任务ID不存在               |

---

## 附录：字段枚举值参考

### 预警类型（alertType）

| 值          | 说明         |
|------------|------------|
| `inactive` | 连续多天未登录    |
| `decline`  | 掌握度 / 成绩下滑 |
| `stagnant` | 学习路径停滞无进展  |

### 预警严重程度（severity）

| 值        | 说明              |
|----------|-----------------|
| `high`   | 高危（如连续7天以上未登录）  |
| `medium` | 中度（如掌握度下滑超过15%） |
| `low`    | 低度（如学习路径停滞3-5天） |

### 任务类型（mission type）

| 值            | 说明     |
|--------------|--------|
| `diagnostic` | 完成诊断测试 |
| `learning`   | 学习路径推进 |
| `review`     | 错题复习   |

### 任务状态（mission status）

| 值           | 说明  |
|-------------|-----|
| `pending`   | 待完成 |
| `completed` | 已完成 |

### 热力图颜色编码

| 颜色         | 掌握度区间     | 含义       |
|------------|-----------|----------|
| 红色（red）    | < 0.5     | 薄弱，需重点关注 |
| 黄色（yellow） | 0.5 - 0.8 | 一般，有提升空间 |
| 绿色（green）  | >= 0.8    | 良好，已基本掌握 |

### 分层作业层级

| 层级  | 掌握度区间     | 题目特点             |
|-----|-----------|------------------|
| 基础层 | < 0.5     | 概念巩固题，注重基础       |
| 提高层 | 0.5 - 0.8 | 综合应用题，注重方法       |
| 拓展层 | > 0.8     | 高考真题 / 竞赛变式，注重创新 |

### 成长等级体系

| 等级   | 称号    | 所需经验值 |
|------|-------|-------|
| Lv1  | 数学学徒  | 0     |
| Lv2  | 数学入门者 | 200   |
| Lv3  | 数学探索者 | 500   |
| Lv4  | 数学实践者 | 1000  |
| Lv5  | 数学进阶者 | 2000  |
| Lv6  | 数学挑战者 | 3000  |
| Lv7  | 数学突破者 | 5000  |
| Lv8  | 数学达人  | 8000  |
| Lv9  | 数学高手  | 12000 |
| Lv10 | 数学学霸  | 20000 |

---


