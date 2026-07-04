---
version: alpha
scope: "仅首页（Landing.vue）—— 本文件只规范落地页，不触及 App 内 14 个页面（那套沿用暖奶油编辑风的旧 DESIGN，见文末「边界」）。"
name: SmartMentor-landing-aura-webgl
theme-source: "getdesign.md → Aura Premium WebGL & Iconify（Futurista & Tech, 2025–2026），融合 Dark SaaS Hero / Liquid Glass 的折射玻璃。血统：Stripe / Vercel / Linear —— 高级软件不喊叫，只低鸣（doesn't scream, it hums）。"
description: "智导师首页的高级深空 WebGL 落地页设计系统。画布是近黑深空 #08080C，其上悬浮一层缓慢流动的 WebGL 极光雾（violet→blue→teal 三色 mesh，12–18s 呼吸循环，绝不脉冲），作为让玻璃材质可读的『光学引擎』而非装饰。核心表面是液态玻璃卡（backdrop-blur + 1px 渐变描边 via mask-composite + 内高光），深度来自折射而非投影（depth through refraction, not shadow）。唯一的动态语言是『光』：pill 按钮 hover 时 1px border-beam 沿边流动，卡片跟随鼠标的 flashlight 高光，区块编号 01/02/03，段落 motion-blur 交错入场。强调靠辉光的位置（你的视线跟着光走），而非铺满颜色。铁律：一屏只有一处发光主体（one moving element per viewport），两处互搏的辉光会互相抵消；shader 是氛围不是内容，掉帧到 60fps 以下直接摘除 WebGL 层降级为静态 CSS mesh。这不是花哨的赛博朋克霓虹，是『感觉像被工程出来的，而非被设计出来的』克制深空高级感。"

colors:
  # —— 深空画布与表面阶梯（近黑，靠明度与玻璃透明度分层）——
  void: "#08080C"                 # 全站画布：近黑深空底，比纯黑略带一丝冷紫
  surface-deep: "#0E0E16"         # 次级底：区块交替带、页脚
  surface-navy: "#141422"         # 深navy面：非玻璃的实体卡（feature 卡底）
  surface-elevated: "#1A1A2E"     # 抬升面：navbar pill 实体、卡内内嵌块
  # —— 液态玻璃（半透明，靠 backdrop-blur 取底色）——
  glass-fill: "rgba(20,20,34,0.55)"      # 玻璃卡填充：半透 navy，让极光透上来
  glass-fill-strong: "rgba(20,20,34,0.72)" # 更实的玻璃（hero 主卡、需读文字处）
  glass-border-from: "rgba(255,255,255,0.24)" # 渐变描边起点（左上高光）
  glass-border-to: "rgba(255,255,255,0.04)"   # 渐变描边终点（右下隐没）
  glass-highlight: "rgba(255,255,255,0.10)"   # 玻璃内顶部 1px 高光
  # —— 文字（冷白四阶，深底靠明度分层）——
  on-void: "#F4F5FA"              # 主标题 / 主文字：冷白
  body: "#C7C9D6"                 # 默认正文：柔和冷灰白
  muted: "#8A8DA0"                # 次级 / 元信息 / 眉题
  muted-soft: "#5B5E72"           # 脚注 / 版权 / 禁用文字
  # —— 强调辉光（violet 为主嗓音，blue/teal 为和声，克制）——
  glow-violet: "#8B5CF6"          # 主辉光：品牌色、主 CTA border-beam、根因节点、极光主色
  glow-violet-soft: "rgba(139,92,246,0.14)" # 紫辉光铺底 / 光晕
  glow-blue: "#3B82F6"            # 第二辉光：极光和声、活跃连接、图表第二序列
  glow-teal: "#2DD4BF"            # 第三辉光：已掌握节点、成功、图表第三序列
  glow-amber: "#F59E0B"           # 点缀：徽章、行内高亮、图表第四序列（罕用）
  # —— 极光 mesh 三停靠色（WebGL/CSS 渐变共用）——
  aurora-1: "#6D28D9"             # 极光深紫（mesh 停靠一）
  aurora-2: "#1D4ED8"             # 极光深蓝（mesh 停靠二）
  aurora-3: "#0D9488"            # 极光深青（mesh 停靠三）
  # —— 深底文字反色 ——
  on-glow: "#0A0A0F"              # 实色辉光按钮上的字（罕用，主 CTA 是描边式）
  # —— 语义色（深底适配，低饱和辉光）——
  success: "#2DD4BF"              # 已掌握 / 正确
  warning: "#F59E0B"              # 待加强
  error: "#F43F5E"                # 薄弱 / 错误
  hairline: "rgba(255,255,255,0.08)" # 深底上的分隔线（1px 冷白极淡）
  hairline-soft: "rgba(255,255,255,0.04)"

typography:
  # display 用几何无衬线（Space Grotesk），大字负字距 + 克制字重，工程感而非编辑感
  display-hero:
    fontFamily: "Space Grotesk, Inter, system-ui, sans-serif"
    fontSize: 76px
    fontWeight: 600
    lineHeight: 1.02
    letterSpacing: -2.5px
  display-lg:
    fontFamily: "Space Grotesk, Inter, sans-serif"
    fontSize: 52px
    fontWeight: 600
    lineHeight: 1.06
    letterSpacing: -1.5px
  display-md:
    fontFamily: "Space Grotesk, Inter, sans-serif"
    fontSize: 36px
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: -0.8px
  title-lg:
    fontFamily: "Inter, sans-serif"
    fontSize: 22px
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: -0.2px
  title-md:
    fontFamily: "Inter, sans-serif"
    fontSize: 18px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: 0
  body-lg:
    fontFamily: "Inter, sans-serif"
    fontSize: 18px
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: 0
  body-md:
    fontFamily: "Inter, sans-serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: 0
  body-sm:
    fontFamily: "Inter, sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: 0
  section-number:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.0
    letterSpacing: 2px
  eyebrow:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 3px
    textTransform: uppercase
  code:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: 0
  button:
    fontFamily: "Inter, sans-serif"
    fontSize: 15px
    fontWeight: 500
    lineHeight: 1.0
    letterSpacing: 0

rounded:
  sm: 8px
  md: 12px
  lg: 18px
  xl: 24px
  pill: 9999px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 40px
  xxl: 64px
  section: 140px

effects:
  # 玻璃折射
  glass-blur: "blur(16px) saturate(1.4)"
  glass-blur-light: "blur(8px) saturate(1.2)"
  glass-border: "1px gradient via mask-composite: {colors.glass-border-from} → {colors.glass-border-to}"
  glass-inner-highlight: "inset 0 1px 0 {colors.glass-highlight}"
  # 辉光
  glow-soft: "0 0 40px 0 {colors.glow-violet-soft}"
  glow-focus: "0 0 0 3px rgba(139,92,246,0.35)"
  border-beam: "1px conic/linear beam sweeping the border on hover, 2.5s"
  flashlight: "radial 320px at mouse (var(--mx),var(--my)) of rgba(139,92,246,0.12), follows pointer on card"
  # 极光背景
  aurora-mesh: "WebGL fragment shader — 3-color simplex-noise mesh (aurora-1/2/3), 14s breathe; CSS fallback = 3 radial-gradient blobs blur(80px) drifting"
  # 元信息
  shadow-depth: "0 24px 60px -20px rgba(0,0,0,0.7)"   # 玻璃卡与底的分离（极淡，非主要深度手段）

motion:
  duration-fast: "200ms"
  duration-base: "360ms"
  duration-slow: "700ms"
  aurora-cycle: "14s"        # 极光呼吸循环，8–18s 区间，「呼吸而非脉冲」
  beam-cycle: "2.5s"
  easing: "cubic-bezier(0.22, 1, 0.36, 1)"
  easing-drift: "cubic-bezier(0.45, 0, 0.55, 1)"  # 极光漂移用正弦感缓动

components:
  navbar-pill:
    backgroundColor: "{colors.glass-fill}"
    backdropFilter: "{effects.glass-blur}"
    border: "{effects.glass-border}"
    textColor: "{colors.body}"
    rounded: "{rounded.pill}"
    height: 56px
    padding: "0 24px"
  button-primary:
    # 主 CTA：玻璃 pill + hover 时紫色 border-beam 流动（描边式发光，非实色块）
    backgroundColor: "{colors.glass-fill-strong}"
    backdropFilter: "{effects.glass-blur-light}"
    textColor: "{colors.on-void}"
    border: "{effects.glass-border}"
    hover: "{effects.border-beam} + {effects.glow-soft}"
    rounded: "{rounded.pill}"
    typography: "{typography.button}"
    padding: "14px 28px"
    height: 52px
  button-secondary:
    backgroundColor: transparent
    textColor: "{colors.body}"
    border: "1px solid {colors.hairline}"
    hover: "border → {colors.glass-border-from}, text → {colors.on-void}"
    rounded: "{rounded.pill}"
    typography: "{typography.button}"
    padding: "14px 28px"
    height: 52px
  glass-card:
    backgroundColor: "{colors.glass-fill}"
    backdropFilter: "{effects.glass-blur}"
    border: "{effects.glass-border}"
    innerHighlight: "{effects.glass-inner-highlight}"
    rounded: "{rounded.lg}"
    padding: "{spacing.xl}"
    hover: "{effects.flashlight}"
  feature-card:
    backgroundColor: "{colors.surface-navy}"
    border: "1px solid {colors.hairline}"
    textColor: "{colors.body}"
    rounded: "{rounded.lg}"
    padding: "{spacing.xl}"
    hover: "border-beam edge + icon glow"
  hero-graph-card:
    # hero 右侧知识图谱：玻璃画布，节点自带辉光，连线流光
    backgroundColor: "{colors.glass-fill-strong}"
    backdropFilter: "{effects.glass-blur}"
    border: "{effects.glass-border}"
    rounded: "{rounded.xl}"
    padding: "{spacing.lg}"
  badge-glow:
    backgroundColor: "{colors.glow-violet-soft}"
    textColor: "{colors.glow-violet}"
    border: "1px solid rgba(139,92,246,0.3)"
    typography: "{typography.eyebrow}"
    rounded: "{rounded.pill}"
    padding: "6px 14px"
  eyebrow-label:
    textColor: "{colors.muted}"
    typography: "{typography.eyebrow}"
  section-number:
    textColor: "{colors.glow-violet}"
    typography: "{typography.section-number}"
  footer:
    backgroundColor: "{colors.surface-deep}"
    textColor: "{colors.muted}"
    border-top: "1px solid {colors.hairline}"
    typography: "{typography.body-sm}"
    padding: "{spacing.xxl}"
---

> 本文件遵循 Google Stitch 的 `DESIGN.md` 开放格式：顶部 YAML 是机器可读设计令牌（“是什么”），下方 Markdown 是设计理由与首页逐区块规范（“为什么”）。AI 编码代理应**严格遵循本文件**，用 `{tokens}` 引用而非硬编码 hex。
>
> **本次改造只针对首页（`src/views/Landing.vue`）。** App 内 14 个页面（Dashboard/Diagnostic/Chat…）不在本文件范围，继续沿用旧的「暖奶油编辑风」设计（见文末「边界与已知差距」）。首页与 App 内页刻意采用两套视觉：落地页是深空高级感的「第一印象系统」，App 内是长时间使用的「陪伴系统」——这是 Aura 主题原文的判断：*a first-impression system, not a living-with-it system*。

## Overview

SmartMentor（智导师）首页要做的事只有一件：**让第一眼相信这是一个被工程出来的、严肃的 AI 系统**。旧首页大面积米白底 + 藏青金彩色卡片 + emoji 图标 + 手绘 SVG 图谱，读起来像小学生课件——本次彻底推翻。

新首页对标 getdesign.md 的 **Aura Premium WebGL & Iconify**，血统是 Stripe / Vercel / Linear：**高级软件不喊叫，只低鸣**。它的高级感不来自堆特效，来自三件事的精确执行：

1. **深空画布 + WebGL 极光是氛围引擎。** 画布是近黑 `{colors.void}`（#08080C），其上悬浮一层缓慢流动的三色极光雾（`{colors.aurora-1/2/3}`，violet→blue→teal，`{motion.aurora-cycle}` 14s 呼吸循环）。这层雾**不是装饰，是让玻璃材质可读的光学引擎**——玻璃折射的正是它。极光「呼吸而非脉冲」（breathe, not pulse）。
2. **液态玻璃是唯一的表面语言，深度来自折射而非投影。** 所有主表面是 `{components.glass-card}`：`backdrop-filter` 取底下的极光 + 1px 渐变描边（`mask-composite` 实现，左上高光→右下隐没）+ 内顶 1px 高光。**Depth through refraction, not shadow**——玻璃扭曲身后的极光来制造纵深，而不是靠投影漂浮。
3. **辉光是唯一的动态语言，且一屏只有一处。** 强调靠**光的位置**（你的视线跟着光走），不靠铺满颜色。主 CTA 的 border-beam 沿 pill 边框流动、卡片跟随鼠标的 flashlight 高光、知识图谱根因节点的紫色辉光——但**一个视口内只允许一处发光主体**（one moving element per viewport），两处互搏的辉光会互相抵消。

**唯一的主嗓音**是紫色辉光 `{colors.glow-violet}`（#8B5CF6）——品牌色、主 CTA、图谱根因节点、极光主色。blue/teal 是和声（图谱连接、已掌握节点、图表序列），amber 是罕用点缀。

**情绪关键词：** 深空、克制、精密、被工程出来的、高级、值得信赖、像 Linear/Vercel 的落地页。
**反面（必须避免）：** 浅底/白底、彩色实心卡堆砌、emoji 图标、手绘幼稚 SVG、赛博朋克双霓虹互搏、满屏发光、快速脉冲动画、把紫色铺到处都是、shader 当主角抢文字、掉帧还硬上 WebGL。

## Colors

调色板：**近黑深空画布 + 液态玻璃半透表面 + 冷白文字四阶 + 紫色主辉光（blue/teal 和声）+ 三色极光 mesh**。

### 画布与表面 · 深空四阶
- `{colors.void}`（#08080C）—— 全站默认底，近黑略带冷紫。**绝不用纯黑#000**（太死）、**绝不用任何浅底**。
- `{colors.surface-deep}`（#0E0E16）—— 区块交替带、页脚，比画布抬一档。
- `{colors.surface-navy}`（#141422）—— 非玻璃的实体特性卡底。
- `{colors.surface-elevated}`（#1A1A2E）—— navbar 实体、卡内内嵌块。
- 相邻区块靠 `void ↔ surface-deep` 的极微明度差 + 极光透出的浓淡切换分节奏，**不靠色块硬切**。

### 液态玻璃 · 折射表面
玻璃卡 = `{colors.glass-fill}`（rgba(20,20,34,0.55) 半透 navy）+ `{effects.glass-blur}`（blur16 saturate1.4）+ 1px 渐变描边（`{colors.glass-border-from}` #fff24% → `{colors.glass-border-to}` #fff4%，`mask-composite: exclude` 只留边框）+ 内顶高光 `{effects.glass-inner-highlight}`。需要读大段文字处用 `{colors.glass-fill-strong}`（0.72 更实），保证对比。

### 文字 · 冷白四阶
靠明度分层：`{colors.on-void}`（#F4F5FA 标题）→ `{colors.body}`（#C7C9D6 正文）→ `{colors.muted}`（#8A8DA0 次级/眉题）→ `{colors.muted-soft}`（#5B5E72 脚注）。**深底 + 玻璃上正文最低 4.5:1，玻璃上的关键文字锁 7:1**（对齐最亮极光帧，见「无障碍」）。

### 辉光 · 紫为主，蓝青为和声
- `{colors.glow-violet}`（#8B5CF6）—— 主嗓音：品牌标记、主 CTA border-beam、图谱根因节点、极光主色。**辉光的点状面积一屏 ≤ 一处主体**。
- `{colors.glow-blue}`（#3B82F6）/ `{colors.glow-teal}`（#2DD4BF）—— 和声：图谱连接线、已掌握节点、数据序列。
- `{colors.glow-amber}`（#F59E0B）—— 罕用点缀，勿滥用。

### 极光 mesh · 三色
`{colors.aurora-1}`（#6D28D9 深紫）/ `{colors.aurora-2}`（#1D4ED8 深蓝）/ `{colors.aurora-3}`（#0D9488 深青）。**只用 3 色**——Aurora 主题铁律：*more than 3–4 colors creates mud, not magic*。

### 强制约束
- **禁止任何浅底 / 白底 / 米白底**（这是与旧首页的根本决裂）。
- **禁止彩色实心卡**（旧首页的藏青/金/绿卡片）——表面只有玻璃与深 navy。
- **禁止 emoji 图标**——图标用 remixicon（已装）单色线性图标，光学对齐文字字重；一旦图标「显得装饰化」高级感即破。
- 极光渐变**只在 3 色内做同氛围过渡**，禁止跨到红/黄等暖色破坏深空冷调。
- 掉帧到 60fps 以下**直接摘除 WebGL 层**，降级为 CSS radial-gradient blob mesh（见 effects.aurora-mesh），绝不 ship jank。

## Typography

**几何无衬线大标题（Space Grotesk）+ 人文无衬线正文（Inter）+ 等宽做技术标记（JetBrains Mono）。** 工程感来自 Space Grotesk 的几何骨架 + 大负字距，而非衬线的编辑感——这是与旧「奶油编辑风」的关键分野。

### 字族
- **Space Grotesk（几何无衬线）** —— 承载 `display-hero/lg/md` 全部大标题。字重 600、大负字距（-0.8 至 -2.5px）。几何感 + 紧字距 = Linear/Vercel 那种「被工程出来」的嗓音。需引入（Google Fonts）。
- **Inter** —— 正文、副标、按钮、卡片文字。已是项目现有字体。
- **JetBrains Mono** —— 区块编号（01/02/03）、眉题、代码窗、数据 ID。等宽字是「技术产品」的信号。

### 层级

| Token | 字号 | 字重 | 字距 | 用途 |
|---|---|---|---|---|
| `{typography.display-hero}` | 76px | 600 | -2.5px | Hero 主标题（Space Grotesk）|
| `{typography.display-lg}` | 52px | 600 | -1.5px | 区块大标题 |
| `{typography.display-md}` | 36px | 600 | -0.8px | 子区块标题 / 卡组标题 |
| `{typography.title-lg}` | 22px | 600 | -0.2px | 卡片标题 |
| `{typography.title-md}` | 18px | 600 | 0 | 小卡标题 |
| `{typography.body-lg}` | 18px | 400 | 0 | Hero 副标 |
| `{typography.body-md}` | 16px | 400 | 0 | 默认正文 |
| `{typography.body-sm}` | 14px | 400 | 0 | 卡片正文 / 页脚 |
| `{typography.section-number}` | 14px | 500 | +2px | 区块编号 01/02/03（mono）|
| `{typography.eyebrow}` | 12px | 500 | +3px | 眉题（大写 mono）|
| `{typography.code}` | 14px | 400 | 0 | 代码 / 公式 |
| `{typography.button}` | 15px | 500 | 0 | 按钮 |

### 原则
- **大标题几何无衬线 600 + 大负字距**，永不用衬线（衬线是旧编辑风的嗓音，会跑偏）。关键词用 `{colors.glow-violet}` 着色**或**极光 mesh 做文字裁切填充（`background-clip: text`），二选一，不叠加。
- **眉题与区块编号用 mono + 正字距 + 大写**，标记「技术层级」，替代彩色徽章。
- 正文 Inter 400，玻璃上正文用 `{colors.body}` 保证读感，勿用比 `{colors.muted}` 更暗的色当正文。

## Layout

**深空画布 + 极光 + 大留白 = 高级呼吸感；密集堆砌 = 廉价。**

### 间距
- 基准 4px。落地区块间距 `{spacing.section}` **140px**（比旧的更舒展，深空需要更大留白）。卡内 padding `{spacing.xl}` 40px。
- 内容最大宽度 **1200px** 居中；Hero 可放宽到 1320px 让图谱卡呼吸。

### 骨架（自上而下，区块交替 void ↔ surface-deep）
1. **浮动 navbar pill** —— 顶部居中悬浮的玻璃胶囊（`{components.navbar-pill}`），不贴边、不通栏；滚动时保持。左 logo + 中锚点 + 右「登录 / 免费开始」。
2. **Hero** —— 全屏高度，WebGL 极光铺满背景。6/6 分栏：左 `{typography.display-hero}` 主标题（关键词紫色辉光/mesh 裁切）+ `body-lg` 副标 + 主次 CTA（pill）+ 下方一行社会认同（social proof，玻璃细条）；右 `{components.hero-graph-card}` 玻璃知识图谱画布——节点自带辉光、连线流光、根因节点紫色强调。
3. **核心能力（01）** —— eyebrow + 区块编号 + `display-lg` 标题；下方 `{components.feature-card}` 或 `glass-card` 3-up 网格（五维画像 / 溯因分析 / DAG 路径 / AI 导师 / 效果追踪），图标用 remixicon 单色线性，hover 触发 border-beam 边光 + flashlight。
4. **实时伴学演示（02）** —— 玻璃「工作台」：左讲义玻璃卡（含公式 KaTeX + 高亮）+ 右 AI 对话玻璃卡（流式气泡）。
5. **溯源式学习原理（03）** —— 可选：一张宽玻璃卡展示「错题 → 前置链回溯 → 根因 → 自底向上路径」的横向流程，节点辉光。
6. **CTA 收束** —— 居中大标题 + 主 CTA pill，背景极光在此处最浓（一屏一处发光主体的「主体」放这）。
7. **页脚** —— `{components.footer}`，`surface-deep` 底 + 1px hairline 顶边，mono 链接。

### 极光分布铁律
**一个视口只有一处发光主体。** Hero 视口主体 = 知识图谱卡的根因节点；能力区视口主体 = 当前 hover 的卡；CTA 视口主体 = 中央 CTA 光晕。滚动时极光整体缓慢漂移，但「亮点」始终只有一个。

## Elevation & Depth

**深度靠「玻璃折射 + 极光浓淡 + 1px 渐变描边」，几乎不用投影（refraction first, shadow rare）。**

| 层级 | 处理 | 用途 |
|---|---|---|
| 0 深空 | 纯 `{colors.void}` + 极光雾 | 区块底、Hero 背景 |
| 1 玻璃 | `{effects.glass-blur}` + 渐变描边 + 内高光 | 所有主卡、navbar、CTA |
| 1 实体 | `{colors.surface-navy}` + 1px hairline | 非透明特性卡 |
| 2 辉光抬升 | `{effects.glow-soft}` + border-beam | hover / 主 CTA / 根因节点 |
| 聚焦 | `{effects.glow-focus}`（3px 紫 35% 环）| 聚焦输入/按钮 |
| 分离阴影 | `{effects.shadow-depth}`（极淡）| 玻璃卡与底的轻微分离，非主要手段 |

**关键质感：**
- **1px 渐变描边**（mask-composite）是玻璃的灵魂——左上取高光、右下隐没，模拟真实玻璃棱边受光。缺了它玻璃会「糊成一坨半透明色块」。
- **border-beam**：一段亮线沿 pill/卡片边框顺时针扫，`{motion.beam-cycle}` 2.5s，仅 hover 或主 CTA 常驻（且全屏仅一处常驻）。
- **flashlight**：卡片监听 `pointermove`，把鼠标坐标写进 `--mx/--my`，一个 320px 半径的紫色 radial 高光跟随，制造「玻璃被手电照亮」的折射感。
- **禁止**：彩色重投影、霓虹外发光描边（glow ≠ neon outline）、单层硬投影、多处同时 border-beam。

## Motion

- 缓动 `{motion.easing}`；极光漂移用 `{motion.easing-drift}` 正弦感。
- **极光呼吸** `{motion.aurora-cycle}` 14s（8–18s 区间）——色停靠位缓慢位移，`breathe, not pulse`。
- **入场**：Hero 元素 `display-hero` 逐字/逐行 clip-slide 上移 + 极淡 motion-blur（对齐 Aura 的「letter by letter clip slide」「motion blur on staggered scroll」），`{motion.duration-slow}` 700ms 交错。
- **滚动**：GSAP ScrollTrigger（已装）区块入场，`once: true`，位移 ≤ 28px + autoAlpha。
- **hover**：按钮/链接 `{motion.duration-fast}` 200ms（对齐 Dark SaaS 的「200ms hover」）。
- **性能硬门**：WebGL 层实测帧率 < 60fps 或 `prefers-reduced-motion` → 摘除 shader，降级为静态 CSS radial-gradient blob mesh（3 个 blur(80px) 的极光色团缓慢 drift 或完全静止）。**Motion earns attention, stillness earns trust。**

## Components（首页专用）

- **`navbar-pill`** —— 顶部居中悬浮玻璃胶囊。玻璃填充 + blur + 渐变描边 + pill。logo 用紫色辉光节点 glyph（非文字堆），右侧「免费开始」= `button-primary`。
- **`button-primary`** —— 主 CTA。玻璃 pill + hover 时紫色 border-beam 流动 + `{effects.glow-soft}`。**这是描边式发光按钮，不是实色紫块**（实色紫块太重、太喊叫）。全屏仅一个常驻 beam。
- **`button-secondary`** —— 透明 + 1px hairline 边，hover 边框转白高光。次级动作（「了解更多」）。
- **`glass-card` / `feature-card`** —— 能力卡。玻璃或深 navy，图标 remixicon 单色线性，hover 触发边光 + flashlight。**绝不用 emoji、绝不用彩色实心卡。**
- **`hero-graph-card`** —— Hero 右侧知识图谱玻璃画布。节点圆自带 `{colors.glow-*}` 柔辉光（非旧的 feGaussianBlur 廉价发光），连线流光（stroke-dasharray 流动），**根因/焦点节点用 `{colors.glow-violet}` 强调**（一屏发光主体）。图例用 mono 小字。
- **`badge-glow`** —— 眉题徽章：紫辉光铺底 + 紫字 + 细紫边 + pill。用于「NEW」「A3 赛道」等标记。
- **`eyebrow-label` / `section-number`** —— mono 眉题 + 紫色区块编号 01/02/03（对齐 Aura 的 section numbering）。
- **AI 对话演示卡** —— 玻璃卡内嵌 `surface-elevated` 气泡，流式光标用 `{colors.glow-violet}` 细条，用户/AI 气泡靠左右 + 明度区分。
- **`footer`** —— surface-deep 底 + 顶 hairline，mono 链接，紫色 hover。

## Illustrations & Assets（美术资源 · 禁止手写）

> **铁律：任何美术资源绝不手写代码。** 旧首页的手绘 SVG 图谱、emoji、CSS 画的装饰都属于「小孩过家家」，本次全部替换。

- **极光背景**：WebGL fragment shader（simplex/curl noise 驱动 3 色 mesh）。轻量实现用 **OGL**（~10kb，需 `npm i ogl`）或裸 WebGL；不引 three.js 全家桶（过重）。降级 CSS mesh 见 motion 节。
- **图标**：一律 **remixicon**（项目已装 `remixicon`）单色线性图标，光学对齐文字字重。禁止 emoji、禁止彩色图标字体。
- **知识图谱**：数据驱动的 SVG/Canvas 节点图（d3 已装），节点辉光用真实 `filter`/`box-shadow` 辉光或 SVG `feDropShadow`，**不手绘装饰性乱线**。
- **3D/产品 mockup（可选）**：若做 hero 产品截图 3D 倾斜卡，用 CSS 3D transform + 玻璃反射（`::after` 渐隐倒影），或引 spline/`<model-viewer>` 承载真实 3D 资源——**不手 CSS 拼 3D 立方体**。
- **纹理/噪点**：极淡 film-grain 噪点叠加（SVG feTurbulence 或一张 tiling PNG，opacity ≤ 0.04）压深空的 banding，这是高级深色页的标配细节。

## Do's and Don'ts

### ✅ Do
- 用 `{colors.void}`（#08080C）近黑深空做画布，WebGL 极光做氛围引擎。
- 所有主表面用液态玻璃：backdrop-blur + 1px 渐变描边（mask-composite）+ 内高光。**深度靠折射不靠投影。**
- 一屏只留**一处发光主体**，紫色辉光是主嗓音，blue/teal 是和声。
- 大标题用 Space Grotesk 600 + 大负字距；眉题/编号用 JetBrains Mono 大写正字距。
- 图标一律 remixicon 单色线性，光学对齐文字。
- 极光「呼吸而非脉冲」，14s 慢循环；hover 200ms。
- 掉帧 < 60fps 或 reduce-motion → 摘 WebGL，降级静态 CSS mesh。
- 区块编号 01/02/03，间距 140px，卡内 40px。
- 美术资源全部用 WebGL/SVG 数据驱动/真实 3D 资源，**绝不手绘**。

### ❌ Don't
- 不用任何浅底/白底/米白底（与旧首页彻底决裂）。
- 不用彩色实心卡堆砌（旧首页藏青/金/绿卡片全删）。
- 不用 emoji 图标、不用手绘幼稚 SVG。
- 不用赛博朋克双霓虹互搏、不满屏发光、不快速脉冲。
- 不把紫色铺到处都是——辉光靠**位置**制造焦点，不靠面积。
- 不用衬线大标题（那是 App 内页的旧编辑风嗓音）。
- 不用实色紫块做主 CTA（用描边式 border-beam pill）。
- shader 不抢文字——氛围永远退到内容之后（atmosphere, never content）。
- 不多处同时 border-beam、不掉帧还硬上 WebGL。

## 边界与已知差距

- **范围**：本文件**仅**规范首页 `src/views/Landing.vue`。改造需替换 Landing.vue 的模板与 `<style>`，并新增极光 WebGL 组件（如 `AuroraBackground.vue`）+ 引入 `ogl` 与 Space Grotesk 字体。首页不复用 `src/assets/css/variables.css` 的旧暖色令牌，改用本文件令牌（建议以 Landing 作用域 CSS 变量或独立 class 命名空间落地，避免污染 App 内页）。
- **App 内 13 个页面**（Dashboard/Diagnostic/DiagnosticResult/Tracing/LearningPaths/Lesson/Chat/Profile/Report 等）**不在本次范围**，继续用现有样式；本文件不与之统一。若日后要统一为深空风，另开工单。
- **主题来源**：getdesign.md「Aura Premium WebGL & Iconify」融合「Dark SaaS Hero / Liquid Glass」。原主题页的 DESIGN.md 正文与字体规格为空，本文件的字体（Space Grotesk / Inter / JetBrains Mono）、间距、圆角、具体 hex 为按主题氛围与项目现状（remixicon/gsap/d3/katex 已装）补全的等价实现，落地时以真实渲染审查迭代。
- **无障碍**：玻璃上文字对比随极光帧变化，须对齐**最亮极光帧**验 WCAG AA（正文 4.5:1，关键文字 7:1）；动画须尊重 `prefers-reduced-motion`。完整合规需真机 + 辅助技术人工核验。
- **性能**：WebGL 极光是渐进增强，非必需层；低端设备/掉帧一律降级 CSS mesh，首屏内容不依赖 shader 加载完成。
