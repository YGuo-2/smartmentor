# SmartMentor 前端开发提示词总集（给 Gemini）

> 用法：先把【第 0 部分 · 全局铁律】**完整复制**到每一次对话最前面，再接上你要做的那个页面的小节。全局铁律是所有页面都必须遵守的硬约束，专门用来防止 AI 跑偏（脱离项目、改导航、瞎编功能、用错语言）。

---

# 第 0 部分 · 全局铁律（每次都要带上）

你是一名资深前端工程师，正在为一个**已存在的真实项目** SmartMentor 开发/重构页面。这不是从零做一个新产品，你的产出必须**无缝融入现有代码库**。下面是不可违反的硬性约束。

## 0.1 项目是什么（不要改变它的定位）

SmartMentor 是一个**面向高校多专业课程的 AI 学习辅导系统**。核心叙事：基于「学生五维画像 + 多智能体协同」，为学生生成**个性化学习路径、诊断测评、溯因分析和学习报告**。

- 这是一个**中文产品**，所有界面文案、标题、按钮、提示**必须是简体中文**。技术术语和代码标识符保留英文。
- 它的领域是「数学/计算机/电子等高校课程的学习辅导」。**严禁**把它做成哲学阅读器、读书 App、笔记软件、番茄钟等任何别的东西。**严禁**自己编造产品名（如 "Scholar's Desk" / "Academic Workspace"），产品名就叫 **SmartMentor**。
- 所有展示内容要贴合"学习辅导"语境：诊断、知识点掌握度、学习路径、错因分析、学习报告。不要出现 "Advanced Philosophy" "Logic and Epistemology" 这类与项目无关的假内容。

## 0.2 技术栈（已锁定，禁止替换或新增依赖）

- Vue 3.4，**一律用 `<script setup>`** 组合式 API。
- 路由：`vue-router` 4，`createWebHashHistory`。跳转用 `import { useRouter } from 'vue-router'` → `router.push('/xxx')`。
- 动画：**GSAP 3.15**，**必须**从项目封装导入，禁止 `import 'gsap'` 或 `import { gsap } from 'gsap'`：
  ```js
  import { gsap, ScrollTrigger, prefersReducedMotion } from '../lib/gsap.js'
  ```
- 图标：**remixicon**（`<i class="ri-xxx-line"></i>`，已全局引入，直接用）。
- 公式：`katex`；Markdown：`marked`；这两个通常通过下面的自定义组件间接使用。
- **禁止引入任何新依赖**：不要用 three.js、lottie、tailwind、element-plus、axios、echarts 等。图表用 SVG 手绘或复用已有组件，请求用项目已有的 `api`。

## 0.3 导航结构（不要重做导航！）

项目已有一个**全局顶部横向导航栏组件 `src/components/AppNav.vue`**，它在 App 布局里统一渲染。**你写的任何业务页面都不要自己画导航栏 / 侧边栏 / 顶部菜单**，那会和全局导航重复。

真实导航项（仅供你理解信息架构，不要复制进页面）：
- 学生端：仪表盘 `/dashboard`、诊断测试 `/diagnostic`、学习路径 `/learning`、学习报告 `/report`、AI对话 `/chat`
- 右上角用户菜单：个人设置 `/profile`、退出登录

> ⚠️ 反面教材：**不要**把功能入口做成左侧竖向菜单列表（如把"平板/书本/相机"列成侧边栏条目）。也不要自己编 "Library / Progress / Archive" 这种顶部 tab。

## 0.4 设计变量（来自 `src/assets/css/variables.css`，直接用 `var(--xxx)`，不要硬编码颜色）

```
配色（学院派）：
--bg #fdfcf9 羊皮纸白   --bg-alt #f4f1e9 浅米色   --card-bg #ffffff
--text #0b1829 深墨    --text-secondary #4a5568   --text-muted #8b98a5
--primary #112240 牛津深蓝   --accent #c5a059 学者金   --accent-hover #a88748
--success #276749 常春藤绿   --danger #9b2c2c 绯红   --info #2b6cb0   --purple #553c9a
--border #e2e0d8
字体：--font-serif（衬线，标题/书页用）  --font（无衬线 Inter，正文用）
圆角：--radius-sm 6 / --radius-md 10 / --radius-xl 16 / --radius-full 50px
阴影：--shadow-sm / --shadow-md / --shadow-lg / --shadow-xl
过渡：--transition (0.3s)  --transition-fast (0.15s)
```
整体气质：**学院派、克制、温暖、有纸张和衬线质感**。避免荧光色、避免廉价科技蓝、避免浮夸弹跳。

## 0.5 数据接口（用项目已有的 `api`，不要自己写 fetch/axios）

```js
import { api } from '../api/index.js'
```
每个接口调用都要 `.catch(() => 兜底值)`，**接口失败时页面不能白屏**，要显示合理的空态/占位。各页面具体用哪些接口见对应小节。进度类数值可能是 0~1 或 0~100，统一归一化：
```js
const toPercent = v => { const n = Number(v||0); return Math.round(n <= 1 ? n*100 : n) }
```

## 0.6 动画规范（GSAP）

1. 一切从 `../lib/gsap.js` 导入。
2. 无障碍：动画入口先判断 `if (prefersReducedMotion()) { /* 直接 gsap.set 到终态 */ return }`。
3. 用 `gsap.context(() => {...}, rootEl.value)` 包裹，`onUnmounted` 时 `ctx?.revert()`。
4. 只动 `transform` / `opacity`（GPU 友好），避免动 `width/top/left`（进度条可用 `scaleX`）。
5. 入场动画用 stagger 错峰，时长 0.4~0.65s，ease 用 `power3.out`。

## 0.7 交付格式

- 产出**完整的单文件组件**：`<template>` + `<script setup>` + `<style scoped>`，可直接保存为对应 `.vue` 替换。
- 注释用中文，关键动画/数据处理段落要说明意图。
- 不要输出脚手架、不要改路由表、不要动其它文件。
- **响应式**：≥1024 桌面、≤768 手机都要可用。

## 0.8 布局铁律 —— 禁止卡片套卡片

**严禁卡片堆叠卡片（nested cards）**：不要在一个有边框/阴影/底色的卡片里，再塞一堆同样有边框/阴影/底色的小卡片。这会造成"框里套框、影子叠影子"的廉价层叠感。

- 一个区块**只有一层**视觉容器。要表达内部条目时，用**留白、分隔线（`1px var(--border)`）、网格间距、字号层级**来区分，而不是再给每个条目套一层卡片外壳。
- 列表项、统计项、表单行等，默认是**扁平**的（透明背景或仅 hover 时浅色高亮），不要默认就带 `box-shadow` + `border` + 圆角的三件套。
- 同级内容用**统一的网格/列表**平铺，靠 `gap` 拉开间距；不要给整组再包一个大卡片、再给每个又包小卡片。
- 阴影只用在"真正浮起来的一层"（如悬浮物品、弹窗、置顶操作栏），普通内容区优先用边框或背景色块，**避免多层阴影叠加**。
- 判断标准：如果一个元素的祖先链上已经有一层卡片样式（border/shadow/card-bg），它自己就不要再做成卡片。

---
---

# 第 1 部分 · Dashboard 学生首页（书桌交互，重点页面）

> 路由 `/dashboard`。这是登录后的门户，也是整个产品的"创意招牌"。下面这版提示词修正了之前 AI 跑偏的所有问题。

## 1.1 核心创意（务必准确理解，别做歪）

这是一张**俯视视角的书桌**，桌上**散落着几件可点击的实体物品**。每件物品是一个功能入口，**点击后播放一段呼应该物品物理属性的动效**，动效结束再跳转到对应功能页。

> 这不是"侧边栏导航 + 主内容区"的后台布局！物品就是漂在桌面上的、有旋转角度、有投影、可点击的实体。整张图就是一张桌面，没有侧栏、没有顶部 tab（全局 AppNav 已在页面之上，你不用管）。

## 1.2 桌面背景：普通亮色（不要皮革、不要深色）

```css
/* 亮色书桌：米白基调 + 中心微亮柔和渐变 */
.desktop-dashboard{
  position: relative;
  width: 100%;
  min-height: calc(100vh - 64px); /* 减去顶部 AppNav 高度 */
  background-color: var(--bg);
  background-image: radial-gradient(circle at 50% 40%, #ffffff 0%, var(--bg) 55%, var(--bg-alt) 100%);
  overflow: hidden;
  perspective: 1500px;
}
```
- **禁止** `dark_leather_desk.png`、禁止任何深色覆盖层。
- 物品的悬浮立体感全靠 `drop-shadow` / `box-shadow`（亮底上用偏暖浅灰投影，如 `rgba(17,34,64,0.12)`）。
- 可选：≤4% 透明度的极淡纸纹或网格，增加桌面质感但不抢物品。

## 1.3 五件物品 → 功能 → 路由 → 物理化动效

素材在 `src/assets/`，相对路径 `../assets/xxx.png` 引用：`desk_book.png`、`desk_camera.png`、`desk_coffee.png`、`desk_tablet.png`（`dark_leather_desk.png` 本次不用）。

| 物品 | 标签(中文) | 路由 | 点击动效（呼应物理属性） |
|------|-----------|------|------------------------|
| 平板 Tablet | 学习路径 | `/learning` | 屏幕**亮屏点亮**（暗→亮）、进度条充能填充，再跳转。平板屏内显示**真实数据**：当前路径标题 + 进度% + 连续学习天数 + 平均掌握度 |
| 书本 Book | AI 对话辅导 | `/chat` | **翻页动画**：书本翻开→连续翻几页→定格在写着"AI 伴学"字样的一页，再跳转 |
| 拍立得相机 Camera | 学情画像 | `/profile` | **快门闪光 + 吐照片**：闪一下白光，底部吐出一张拍立得（照片上是雷达图剪影），再跳转 |
| 咖啡杯 Coffee | 学习报告 | `/report` | **热气升腾**：悬停杯口飘热气，点击时杯身轻晃、热气加速，再跳转 |
| 记事本 Notebook | 水平诊断 | `/diagnostic` | **翻开 + 落笔书写**：封面掀开、笔尖划出一行"开始诊断"，再跳转 |

> 记事本注意：之前的版本把 `desk_book.png` 加 `hue-rotate` 当记事本，很廉价。请**用纯 CSS 画一个独立记事本**（螺旋线圈 + 横线纸 + 斜放钢笔），或注释标注「需 `desk_notebook.png` 素材」并先 CSS 占位。不要 hue-rotate 复用书本图。

## 1.4 布局要求

- 五件物品**疏密有致地散落**在整张桌面上，各自有轻微旋转角度（±6°~±12°），彼此**不重叠**、留出呼吸空间。先规划坐标再写代码。
- 物品体量有主次：书本最大且最居中（招牌动效），其余环绕。
- 物品下方的**功能中文标签 tooltip**，默认隐藏，悬停时淡入上浮。

## 1.5 数据

```js
import { api } from '../api/index.js'
const me   = await api.auth.me().catch(() => ({}))          // { nickname, username }
const ov   = await api.profile.overview().catch(() => ({})) // { streakDays, overallMastery }
const dash = await api.report.dashboard().catch(() => ({}))  // dash.currentPath: { pathId, title, progress }
```
平板屏显示：`currentPath.title`、`toPercent(currentPath.progress)`%、`ov.streakDays` 天、`toPercent(ov.overallMastery)`%。无路径时显示「暂无攻坚路径，点击去选择」空态，不能白屏。

## 1.6 动效编排（重点修复项）

之前用 CSS 硬编码 + `setTimeout` 串联，僵硬不可控。**改用 GSAP Timeline**：
- 进入页面：物品逐件淡入 + 轻微位移/旋转 + stagger，像"被一件件摆上桌"。
- 点击：用 `isAnimating` ref 加锁防重复点击；物理动效用 Timeline 编排；Timeline 的 `onComplete` 里再 `router.push`，让用户看清"物品响应了点击"的反馈闭环（约 0.8~1.3s）。
- 悬停：物品微抬 + 投影变浓 + 标签淡入，组合反馈。
- `prefersReducedMotion()` 为真时跳过所有动效，点击直接跳转。

## 1.7 交付

先用一段话写出**布局规划**（每件物品的大致位置/旋转/层级），再给完整 `Dashboard.vue`。五件物品的物理化动效都要落地，不能只做书本翻页一个。

---
---

# 第 2 部分 · 其余页面提示词

> 每节都先带上【第 0 部分 全局铁律】再用。这些页面是**常规后台/学习页面**，用顶部已有的 AppNav，不需要书桌创意，重点是信息清晰、学院派配色、数据正确、动效克制。

## 2.1 Auth 登录/注册页（`/login`、`/register`）

居中表单卡片，`mode` prop 切换登录/注册。
- 登录：用户名 + 密码，调 `api.auth.login()`。
- 注册：用户名 + 邮箱 + 验证码（按钮带 60s 倒计时，调 `api.auth.sendCaptcha()`）+ 密码 + 确认密码 + 昵称 + 学历层次 + 学校。调 `api.auth.register()`。
- 顶部有"返回首页"链接，底部有登录/注册互相切换链接。
- 视觉：羊皮纸背景 + 白色卡片 + 学者金主按钮，衬线标题。表单校验要有错误提示。

## 2.2 Chat AI 对话（`/chat`）

经典对话布局：**左侧会话侧栏**（可收起）+ **右侧对话区**。注意这里的左侧栏是"历史会话列表"，是这个页面自己的，不是全局导航。
- 数据：`api.chat.history()` 取历史会话与消息；流式回复走 SSE 端点 `/api/chat/stream`（解析 message/resources/metadata/done/error 事件）。
- 右侧：顶部 AI 信息条（头像、SmartMentor、在线状态）；中间消息区用 **`MarkdownMessage` 组件**（`import MarkdownMessage from '../components/MarkdownMessage.vue'`，props: `content`、`streaming`）渲染 Markdown+LaTeX+Mermaid；首条消息前显示欢迎屏 + 4 个快速建议按钮；AI 消息下可附学习资源卡片（视频/链接，带时长、播放数）。
- 底部：自动调高的 textarea + 发送/停止按钮 + "AI 可能会犯错，请核实"提示。流式输出要有光标动画、等待时有三点打字指示器。

## 2.3 Diagnostic 自适应诊断（`/diagnostic`）

两阶段单页：
- **阶段一 选择**：三步流程指示器（选课程→作答→看结果）；学习画像卡片（专业/学历/课程/目标/基础 等下拉）；3 个预设科目卡（AI 基础 / Java Web / 数字电路）+ "其他科目"自定义输入卡；开始按钮调 `api.diagnostic.start()`。
- **阶段二 作答**：顶部进度条 + 题号/总数(~8) + 难度徽章（简单/中等/困难）；题目卡片（HTML + LaTeX，用 KaTeX 渲染）；选项列表（选择题）或 textarea（填空/主观）；提交调 `api.diagnostic.submit()` 返回反馈（正确/错误 + 参考答案 + AI 错因分析）和下一题；可中途 `api.diagnostic.finish()`，带确认弹窗。

## 2.4 DiagnosticHistory 诊断历史（`/diagnostic/history`）

- `api.diagnostic.history()` 分页 + 模块筛选。
- 顶部 3 张统计卡（诊断次数 / 平均正确率 / 最多诊断模块）；筛选工具栏；记录表格（日期/模块/正确率带色/题数/掌握度/状态/查看详情）；分页器。新建诊断按钮跳 `/diagnostic`。

## 2.5 DiagnosticResult 诊断结果（`/diagnostic/result/:diagnosticId`）

- `api.diagnostic.detail()` 取详情；若 `aiAnalysisPending` 为真，每 3s 轮询直到完成（轮询期间显示"AI 分析中"指示器）。
- 深色 Hero 卡：模块名 + 大号正确率% + 元数据（题数/正确数/日期）+ 两个甜甜圈（正确率、掌握度，SVG 手绘）。
- AI 学习建议卡（文字）；薄弱知识点网格（小卡 + 掌握度圆环）；逐题详情列表（题号 + 对错徽章 + 知识点标签 + 题面 + 学生答vs正确答 + AI 错因分析）。
- 固定底部行动栏：生成路径（`api.learning.generate()`）/ 溯因分析（`api.tracing.analyze()`）/ 返回历史。

## 2.6 TracingResult 溯因分析（`/tracing/:tracingId`）

- `api.tracing.detail()` 取根因、受影响知识点、溯源链路、知识图谱。
- 深色 Hero（根因数 + 分析知识点数 + 三统计盒）；AI 溯因报告卡；根本原因列表（优先级标签 + 知识点 + 模块 + 掌握度条 + 影响知识点标签）；溯因路径列表（目标知识点 + 深度 + 圆点连接的知识点链，标注根因/目标/掌握度）；知识图谱可视化（可用 **`MindMap` 组件**，props: `markdown`，或 SVG 网络图）。

## 2.7 LearningPaths 学习路径列表（`/learning`）

- `api.learning.paths()` 取路径列表 + 学生画像。
- 学生画像信息条（专业/学历/课程/目标 + 推荐资源类型 chips）；首个路径做成**特色大卡**（绿色调 + 大标题 + 进度条 + 大甜甜圈 + 继续学习）；其余路径响应式网格卡（模块/标题/进度/完成数/日期/状态徽章/继续）。空态：图标 + 提示 + 开始诊断按钮。

## 2.8 LearningPathDetail 路径详情（`/learning/:pathId`）

- `api.learning.pathDetail()` 取节点列表、进度、画像。
- 路径信息卡（模块标签 + 大标题 + 元数据：完成数/完成率/总耗时/节点数 + 画像 chips + 进度条 + 状态徽章 + 继续学习/重新诊断 + 大甜甜圈）。
- **DAG 学习路线 SVG**：节点按拓扑排列、曲线带箭头连接；节点状态用颜色区分（已完成绿 / 当前黄 / 可学蓝 / 未解锁灰 / 补救红），补救节点下层展示，配图例；点击节点跳 `/learning/:pathId/:nodeId`。
- 节点明细时间线（左竖轴圆点 + 右卡片：标题/类型徽章/知识点/时长/状态）。

## 2.9 Lesson 节点学习页（`/learning/:pathId/:nodeId`）

四阶段 Tab：**先学 → 练习 → 检查点 → 总结**。
- 数据：`api.learning.lesson()`（节点详情）、`lessonVideo()`（B站视频）、`lessonResourceDetails()`（文档/论文）、`lessonSlides()`（PPT）、`submitExercise()`、`submitCheckpoint()`（≥60% 通过解锁下一节点）；AI 讲解走 SSE 流式。
- 先学：标题 + 知识点 + 教学策略徽章 + "让 AI 带我学"按钮；视频卡（可折叠，iframe 内嵌）；资源卡网格；讲解内容（用 `MarkdownMessage`）；例题分步解答。
- PPT 用 **`PptViewer` 组件**（props: `slidesDoc`、`downloading`，事件 `close`/`download`，基于 Reveal.js，可导出 .pptx）。

## 2.10 Report 学习报告（`/report`）

- `api.report.effectiveness()`（时间段 7/30/90 天可选）。
- 期间切换 tab；4 张统计卡（总时长/完成题数/平均正确率/能力提升%）；前后对比卡（各模块 诊断前 vs 学习后 掌握度双进度条 + 增长差）；课程效果评估卡（掌握度/目标达成/路径完成/练习正确率/资源命中率 + 资源偏好 chips）；掌握度趋势图（SVG 柱状或折线）。

## 2.11 Profile 个人设置（`/profile`）

- `api.auth.me()` / `api.profile.overview()` / `api.profile.updateSettings()`。
- 左侧本页 tab 菜单 + 右侧内容区，三个 tab：基本信息（昵称/专业/学历/当前课程）、学习偏好（目标/基础/兴趣/学习风格/每日时长/时段/模式）、资源与模块（资源偏好复选 + 薄弱模块优先级复选）。每 tab 有保存按钮。
- 可选展示 **`ProfileRadar`**（props: `dimensions`，五维雷达）和 **`ProfileDimensionCharts`**（props: `cognitiveStyle`/`learningBehavior`/`errorPatterns`，三组条形图）。

## 2.12 ProfileOnboarding 画像访谈（`/onboarding`，新用户首次）

- AI 流式对话访谈建立初始画像。对话卡：顶部 AI 头像 + "AI 学习画像访谈" + 跳过按钮；消息区用 `MarkdownMessage`；底部 textarea + 发送/完成。结束后显示画像结果（成功图标 + 抽取到的特征标签 + 查看画像/进入学习按钮）。

## 2.13 Landing 营销首页（`/`，已有较完整实现）

未登录的营销页，纯静态 + GSAP 滚动动画：Hero（英文大标题 + 浮动技术词 + 知识图谱 SVG）+ 6 个 Bento 功能卡 + 演示区 + 页脚。**这个页面已基本完成，除非要改风格，一般不用重做。**

---

## 复用组件速查（import 自 `../components/`）

| 组件 | 用途 | 关键 props |
|------|------|-----------|
| `MarkdownMessage` | 渲染 Markdown+LaTeX+Mermaid 消息 | `content`、`streaming` |
| `PptViewer` | Reveal.js 演示 + 导出 pptx | `slidesDoc`、`downloading`；事件 `close`/`download` |
| `ProfileRadar` | 五维能力雷达图(SVG) | `dimensions` |
| `ProfileDimensionCharts` | 认知风格/学习行为/错误模式 条形图 | `cognitiveStyle`、`learningBehavior`、`errorPatterns` |
| `MindMap` | markmap 思维导图 | `markdown` |
| `AppNav` | 全局顶部导航（已全局渲染，业务页勿重复） | — |
