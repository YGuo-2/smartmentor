<template>
<div class="landing-page mesh-background" ref="pageRoot">
  <nav class="landing-nav">
    <div class="landing-nav-logo">SmartMentor</div>
    <ul class="landing-nav-links">
      <li><a href="#features">功能</a></li>
      <li><a href="#demo">体验</a></li>
      <li><a href="#contact">联系</a></li>
    </ul>
    <div style="display:flex;gap:12px">
      <router-link to="/login" class="btn btn-outline">登录</router-link>
      <router-link to="/register" class="btn btn-dark">免费开始</router-link>
    </div>
  </nav>

  <section class="hero-section">
    <!-- 课程能力浮动背景（精简，仅置于四角安全区，避开中央内容） -->
    <div class="math-bg-elements">
      <span class="math-float math-amber" style="top:12%;left:5%;animation-delay:0s;font-size:2.6rem">AI</span>
      <span class="math-float math-purple" style="top:78%;left:7%;animation-delay:1.8s;font-size:2.2rem">RAG</span>
      <span class="math-float math-green" style="top:14%;right:7%;animation-delay:1.2s;font-size:2.2rem">DAG</span>
      <span class="math-float math-blue" style="top:82%;right:6%;animation-delay:0.8s;font-size:2.4rem">Agent</span>
    </div>
    <div class="hero-left">
      <h1>GROWING<br>SMARTER<br>WITH AI</h1>
      <p class="subtitle">面向高校多专业课程，基于学生画像与多智能体协同生成个性化学习路径、资源包和评估反馈。</p>
      <div class="hero-buttons">
        <router-link :to="{ path: '/register', query: { redirect: '/onboarding' } }" class="btn btn-dark btn-lg">开始学习之旅</router-link>
        <a href="#features" class="btn btn-outline btn-lg">了解更多</a>
      </div>
    </div>
    <div class="hero-right">
      <div class="hero-knowledge-graph">
        <svg viewBox="38 28 340 320" class="academic-graph">
          <defs>
            <linearGradient id="edgeGrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="var(--accent)" />
              <stop offset="100%" stop-color="var(--primary)" />
            </linearGradient>
            <filter id="glow">
              <feGaussianBlur stdDeviation="1.5" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
          </defs>
          <!-- Edges -->
          <path d="M 95,135 L 205,90" class="graph-edge active" />
          <path d="M 205,90 L 315,160" class="graph-edge" />
          <path d="M 95,135 L 160,255" class="graph-edge" />
          <path d="M 160,255 L 285,290" class="graph-edge active" />
          <path d="M 205,90 L 285,290" class="graph-edge" />
          <path d="M 315,160 L 285,290" class="graph-edge" />

          <!-- Nodes：实心知识球，标签置于圈内；当前学习焦点最大 -->
          <g class="graph-node completed" transform="translate(95, 135)">
            <circle r="34" />
            <text dy="0.35em">高等数学</text>
          </g>
          <g class="graph-node active" transform="translate(205, 90)">
            <circle r="50" class="pulse-ring" />
            <circle r="50" />
            <text dy="0.35em">线性代数</text>
          </g>
          <g class="graph-node locked" transform="translate(315, 160)">
            <circle r="32" />
            <text dy="0.35em">机器学习</text>
          </g>
          <g class="graph-node completed" transform="translate(160, 255)">
            <circle r="36" />
            <text dy="0.35em">数据结构</text>
          </g>
          <g class="graph-node active" transform="translate(285, 290)">
            <circle r="40" class="pulse-ring" />
            <circle r="40" />
            <text dy="0.35em">算法分析</text>
          </g>
        </svg>
        <div class="graph-legend">
          <span class="legend-item"><span class="dot completed"></span> 已掌握</span>
          <span class="legend-item"><span class="dot active"></span> 学习中</span>
          <span class="legend-item"><span class="dot locked"></span> 待解锁</span>
        </div>
      </div>
    </div>
    <div class="hero-bottom">
      <div class="tagline">帮助不同专业、不同学历层次的学生，获得匹配自身目标和进度的学习资源。</div>
      <div class="scroll-hint">
        <span>SCROLL</span>
        <div class="scroll-dot"></div>
      </div>
    </div>
  </section>

  <section class="landing-section" id="features">
    <div class="section-head">
      <span class="section-index">01 — 核心能力</span>
      <h2>多智能体协同<br>生成你的个性化课程资源</h2>
    </div>
    <div class="feature-narrative">
      <!-- 主焦点：深色强调块 -->
      <div class="feature-hero">
        <div class="feature-hero-icon">🎯</div>
        <h3>自适应诊断模型</h3>
        <p>基于项目响应理论 (IRT)，5-8 题精准定位薄弱环节，按专业、学历和课程动态调节题目难度。</p>
        <div class="feature-hero-watermark">DIAGNOSTIC</div>
      </div>
      <!-- 右侧：3D 卡片堆叠轮播，循环展示其余核心能力 -->
      <div class="feature-swap-stage">
        <CardSwap
          :width="420"
          :height="280"
          :card-distance="56"
          :vertical-distance="64"
          :delay="3800"
          :skew-amount="5"
          pause-on-hover
        >
          <Card v-for="f in swapFeatures" :key="f.title">
            <div class="swap-card-inner">
              <span class="swap-card-icon">{{ f.icon }}</span>
              <h4>{{ f.title }}</h4>
              <p>{{ f.desc }}</p>
            </div>
          </Card>
        </CardSwap>
      </div>
    </div>
  </section>

  <section class="demo-section" id="demo">
    <div class="section-head">
      <span class="section-index">02 — 实时伴学</span>
      <h2>像导师一样思考<br>比学长更懂你</h2>
    </div>
    <div class="study-desk-layout">
      <!-- Left: Academic Document -->
      <div class="desk-document">
        <div class="doc-header">
          <span class="doc-tab">Chapter 4.2</span>
          <span class="doc-title">组合逻辑电路设计</span>
        </div>
        <div class="doc-content">
          <h4>译码器与数据分配器</h4>
          <p>在数字电路中，译码器能够将输入的二进制代码转换为特定的输出信号。这是构成复杂系统模块的基础...</p>
          <mark class="doc-highlight">此处为历年考试难点：级联设计</mark>
          <div class="doc-formula">Y_i = m_i \quad (i = 0, 1, \dots, 2^n - 1)</div>
          <p>对于电子信息工程专业的学生，掌握译码器的级联设计尤为重要，这在微机原理的地址译码中经常用到。</p>
        </div>
      </div>

      <!-- Right: AI Tutor -->
      <div class="desk-tutor">
        <div class="tutor-header">
          <div class="tutor-avatar">S</div>
          <div class="tutor-info">
            <div class="name">SmartMentor</div>
            <div class="status">在线辅导中</div>
          </div>
        </div>
        <div class="tutor-chat">
          <div class="chat-msg bot">我已经将你在上一章的易错点融合进这份讲义了。注意左侧的高亮部分，需要我为你演示一个 3-8 译码器的级联案例吗？</div>
          <div class="chat-msg user">好，我想看看怎么组成 4-16 译码器。</div>
          <div class="chat-msg bot">关键在于利用使能端 (Enable)。当最高位 A3 为 0 时选通第一片，为 1 时选通第二片...</div>
        </div>
        <div class="tutor-input">
          <input type="text" placeholder="向导师提问..." disabled>
          <button class="btn btn-dark btn-sm">发送</button>
        </div>
      </div>
    </div>
  </section>

  <footer class="landing-footer" id="contact">
    <div class="landing-footer-links">
      <a href="#">关于</a><a href="#">文档</a><a href="#">隐私</a><a href="#">条款</a>
    </div>
    <p>© 2026 SmartMentor</p>
  </footer>
</div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { gsap, ScrollTrigger, prefersReducedMotion } from '../lib/gsap.js'
import CardSwap from '../components/CardSwap.vue'
import Card from '../components/Card.vue'

const pageRoot = ref(null)
let ctx

// 右侧 3D 卡片轮播展示的核心能力（主焦点"自适应诊断"已单列在左侧深色块）
const swapFeatures = [
  { icon: '🧠', title: '五维画像', desc: '知识状态、错误模式、学习行为、认知风格、目标画像全方位图谱。' },
  { icon: '🔍', title: '溯因分析', desc: '沿课程图谱前驱链递归追溯，找到问题根因。' },
  { icon: '📐', title: 'DAG 个性化路径', desc: '拓扑排序生成最优学习路径，三级教学策略因材施教，前置牢固后再进阶。' },
  { icon: '💬', title: 'AI 导师答疑', desc: 'SSE 流式对话提供全天候导师级课程辅导。' },
  { icon: '📊', title: '效果追踪', desc: '前测-后测对比，错误消除率可视化呈现。' }
]

onMounted(() => {
  if (!pageRoot.value) return

  ctx = gsap.context(() => {
    if (prefersReducedMotion()) {
      gsap.set('.landing-nav, .hero-left > *, .hero-right, .hero-bottom, .math-float, .section-head, .feature-hero, .feature-swap-stage, .desk-document, .desk-tutor', {
        autoAlpha: 1,
        clearProps: 'transform,visibility'
      })
      return
    }

    const hero = gsap.timeline({ defaults: { duration: 0.62, ease: 'power3.out' } })
    hero
      .from('.landing-nav', { autoAlpha: 0, y: -18, duration: 0.45 })
      .from('.hero-left h1', { autoAlpha: 0, y: 34, skewY: 1.5 }, '<0.08')
      .from('.hero-left .subtitle', { autoAlpha: 0, y: 20 }, '<0.16')
      .from('.hero-buttons .btn', { autoAlpha: 0, y: 16, stagger: 0.08 }, '<0.14')
      .from('.hero-right', { autoAlpha: 0, x: 34, scale: 0.98 }, '<0.05')
      .from('.hero-bottom', { autoAlpha: 0, y: 16, duration: 0.45 }, '<0.18')

    gsap.to('.math-float', {
      autoAlpha: 0.85,
      y: -22,
      rotation: (index) => (index % 2 === 0 ? 7 : -7),
      duration: 3.6,
      stagger: { each: 0.12, from: 'random' },
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
      overwrite: 'auto'
    })

    const courseBoard = gsap.utils.toArray('.hero-course-board')
    if (courseBoard.length > 0) {
      gsap.to(courseBoard, {
        y: -10,
        duration: 3.2,
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut'
      })
    }

    const featureCards = gsap.utils.toArray('.feature-hero, .feature-swap-stage')
    gsap.set(featureCards, { autoAlpha: 0, y: 28 })
    ScrollTrigger.batch(featureCards, {
      scroller: pageRoot.value,
      start: 'top 85%',
      once: true,
      onEnter: (batch) => {
        gsap.to(batch, {
          autoAlpha: 1,
          y: 0,
          stagger: 0.08,
          duration: 0.58,
          ease: 'power3.out',
          overwrite: true,
          clearProps: 'transform,visibility,opacity'
        })
      }
    })

    gsap.from('.section-head', {
      autoAlpha: 0,
      y: 22,
      duration: 0.5,
      stagger: 0.08,
      scrollTrigger: {
        trigger: '#features',
        scroller: pageRoot.value,
        start: 'top 76%',
        once: true
      }
    })

    gsap.from('.desk-document', {
      autoAlpha: 0,
      x: -32,
      duration: 0.65,
      scrollTrigger: {
        trigger: '.demo-section',
        scroller: pageRoot.value,
        start: 'top 74%',
        once: true
      }
    })

    gsap.from('.desk-tutor', {
      autoAlpha: 0,
      x: 32,
      duration: 0.65,
      scrollTrigger: {
        trigger: '.demo-section',
        scroller: pageRoot.value,
        start: 'top 74%',
        once: true
      }
    })
  }, pageRoot.value)

  ScrollTrigger.refresh()
})

onUnmounted(() => {
  ctx?.revert()
})
</script>

<style>
.math-bg-elements {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}
.math-float {
  position: absolute;
  font-family: 'KaTeX_Math', Georgia, serif;
  font-style: italic;
  font-weight: 700;
  font-size: 1.6rem;
  opacity: 0;
  -webkit-user-select: none;
  user-select: none;
}
.math-amber { color: var(--accent-light); }
.math-blue { color: var(--primary-light); }
.math-purple { color: var(--accent-light); }
.math-green { color: var(--primary-light); }
@keyframes mathFloat {
  0%, 100% {
    opacity: 0;
    transform: translateY(0) rotate(0deg);
  }
  20% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
    transform: translateY(-20px) rotate(8deg);
  }
  80% {
    opacity: 1;
  }
}
.hero-section {
  position: relative;
}
.hero-left, .hero-right, .hero-bottom {
  position: relative;
  z-index: 1;
}
.hero-knowledge-graph {
  width: 100%;
  max-width: 560px;
  position: relative;
  padding: 20px 0;
}
.academic-graph {
  width: 100%;
  height: auto;
  overflow: visible;
}
.graph-edge {
  fill: none;
  stroke: rgba(17, 34, 64, 0.22);
  stroke-width: 2.5;
  transition: all var(--transition);
}
.graph-edge.active {
  stroke: url(#edgeGrad);
  stroke-width: 3.5;
  stroke-dasharray: 6 4;
  animation: flowLine 2s linear infinite;
}
@keyframes flowLine {
  to { stroke-dashoffset: -20; }
}
.graph-node circle {
  fill: var(--card-bg-solid);
  stroke: var(--border);
  stroke-width: 2.5;
  transition: all var(--transition);
}
.graph-node text {
  font-family: var(--font);
  font-size: 15px;
  font-weight: 700;
  fill: #fff;
  text-anchor: middle;
}
/* 已掌握：藏青实心球 */
.graph-node.completed circle { fill: var(--primary); stroke: var(--primary); }
.graph-node.completed text { fill: #fff; }
/* 学习中：学者金实心球 + 发光 */
.graph-node.active circle { fill: var(--accent); stroke: var(--accent); filter: url(#glow); }
.graph-node.active text { fill: #fff; }
/* 待解锁：浅填充 + 虚线边 */
.graph-node.locked circle { fill: var(--bg-alt); stroke: var(--text-muted); stroke-dasharray: 5 3; }
.graph-node.locked text { fill: var(--text-secondary); }
.pulse-ring {
  fill: none !important;
  stroke: var(--accent) !important;
  stroke-width: 2 !important;
  opacity: 0.5;
  animation: pulseNode 2.4s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  transform-box: fill-box;
  transform-origin: center;
}
@keyframes pulseNode {
  0% { transform: scale(0.85); opacity: 0.6; }
  100% { transform: scale(1.35); opacity: 0; }
}
.graph-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 10px;
}
.legend-item {
  font-size: 0.8rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid;
}
.legend-item .dot.completed { border-color: var(--primary); background: var(--primary-light); }
.legend-item .dot.active { border-color: var(--accent); background: var(--card-bg-solid); }
.legend-item .dot.locked { border-color: var(--border); background: var(--card-bg-solid); }

/* ===== 核心能力右侧：3D 卡片轮播舞台 =====
   CardSwap 容器绝对定位于 stage 右下角，stage 需相对定位 + 足够高度容纳。 */
.feature-swap-stage {
  position: relative;
  min-height: 360px;
  width: 100%;
}
.swap-card-inner {
  width: 100%;
  height: 100%;
  padding: 30px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  box-sizing: border-box;
}
.swap-card-icon {
  font-size: 2rem;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(197, 160, 89, 0.16);
  border: 1px solid rgba(197, 160, 89, 0.3);
  border-radius: 14px;
}
.swap-card-inner h4 {
  font-family: var(--font-serif);
  font-size: 1.45rem;
  font-weight: 700;
  color: #fff;
  margin: 4px 0 0;
}
.swap-card-inner p {
  font-size: 0.95rem;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.72);
  margin: 0;
}

/* 窄屏：卡片轮播在小屏 transform 缩放后右下角易溢出，stage 降高并居中收纳 */
@media (max-width: 768px) {
  .feature-swap-stage { min-height: 300px; }
}
</style>
