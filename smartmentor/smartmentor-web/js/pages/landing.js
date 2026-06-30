import { ref } from 'vue';

export const LandingPage = {
  template: `
<div class="landing-page">
  <nav class="landing-nav">
    <div class="landing-nav-logo">SmartMentor</div>
    <ul class="landing-nav-links">
      <li><a href="#features">功能</a></li>
      <li><a href="#demo">体验</a></li>
      <li><a href="#pricing">方案</a></li>
      <li><a href="#contact">联系</a></li>
    </ul>
    <div style="display:flex;gap:12px">
      <router-link to="/login" class="btn btn-outline">登录</router-link>
      <router-link to="/register" class="btn btn-dark">免费开始</router-link>
    </div>
  </nav>

  <section class="hero-section">
    <div class="hero-left">
      <h1>GROWING<br>SMARTER<br>WITH AI</h1>
      <p class="subtitle">面向高校多专业课程，基于学生画像与多智能体协同生成个性化学习路径、资源包和评估反馈。</p>
      <div class="hero-buttons">
        <router-link to="/register" class="btn btn-dark btn-lg">开始诊断</router-link>
        <a href="#features" class="btn btn-outline btn-lg">了解更多</a>
      </div>
    </div>
    <div class="hero-right">
      <div style="text-align:center;padding:40px">
        <div style="font-size:6rem;margin-bottom:20px">🧠</div>
        <p style="color:var(--text-muted);font-size:0.9rem">AI 驱动的高校课程伴学</p>
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
    <div class="landing-section-header">
      <h2>核心能力</h2>
      <p>多智能体协同，生成你的个性化课程资源</p>
    </div>
    <div class="features-grid">
      <div class="feature-card" v-for="f in features" :key="f.title">
        <div class="feature-icon">{{ f.icon }}</div>
        <h3>{{ f.title }}</h3>
        <p>{{ f.desc }}</p>
      </div>
    </div>
  </section>

  <section class="demo-section" id="demo">
    <div class="demo-chat">
      <div class="demo-chat-header">
        <div class="demo-avatar">AI</div>
        <div><div class="name">SmartMentor</div><div class="status">在线</div></div>
      </div>
      <div class="chat-messages">
        <div class="chat-msg user">我是电子信息专业本科生，想学数字电路，该先补什么？</div>
        <div class="chat-msg bot">系统会先检查逻辑门、布尔代数、卡诺图三个前置节点，再生成讲解文档、实验任务和视频资源。</div>
        <div class="chat-msg user">如果我更偏项目实践呢？</div>
        <div class="chat-msg bot">路径会提高实操案例权重，例如用仿真工具搭建译码器，并在检查点中评估电路分析和调试能力。</div>
      </div>
    </div>
    <div class="demo-info">
      <h2>理解你的学习状态<br>匹配你的下一步</h2>
      <p>基于五维学生画像和课程知识图谱，精准定位薄弱环节。每次互动都让 AI 更了解你的专业背景、学历层次和学习目标。</p>
      <div class="demo-stats">
        <div class="demo-stat"><div class="number">92%</div><div class="label">诊断准确率</div></div>
        <div class="demo-stat"><div class="number">1.2s</div><div class="label">平均响应</div></div>
        <div class="demo-stat"><div class="number">3+</div><div class="label">课程方向</div></div>
      </div>
    </div>
  </section>

  <section class="landing-section" id="pricing">
    <div class="landing-section-header">
      <h2>选择方案</h2>
      <p>从免费开始，随时升级</p>
    </div>
    <div class="pricing-grid">
      <div class="pricing-card">
        <h3>基础版</h3>
        <div class="price">¥0<span>/月</span></div>
        <ul class="pricing-features">
          <li>每日3次诊断测试</li><li>基础知识图谱</li><li>AI对话辅导（20次/天）</li><li>学习效果报告</li>
        </ul>
        <router-link to="/register" class="btn btn-outline btn-full">免费开始</router-link>
      </div>
      <div class="pricing-card featured">
        <h3>进阶版</h3>
        <div class="price">¥39.9<span>/月</span></div>
        <ul class="pricing-features">
          <li>无限诊断测试</li><li>全模块知识图谱</li><li>无限AI对话辅导</li><li>个性化学习路径</li><li>溯因分析</li><li>每日任务奖励</li>
        </ul>
        <router-link to="/register" class="btn btn-dark btn-full">升级进阶版</router-link>
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
  `,
  setup() {
    const features = [
      { icon: '🎯', title: '自适应诊断', desc: '5-8题精准诊断，按专业、学历和课程动态调节题目难度' },
      { icon: '🧠', title: '五维画像', desc: '知识状态、错误模式、学习行为、认知风格、目标画像全方位分析' },
      { icon: '🔍', title: '溯因分析', desc: '沿课程知识图谱前驱链递归追溯，找到问题根因而非表象' },
      { icon: '📐', title: '个性化路径', desc: 'DAG拓扑排序生成最优学习路径，三级教学策略因材施教' },
      { icon: '💬', title: 'AI对话辅导', desc: 'SSE流式对话，结合画像提供课程答疑、案例讲解和资源推荐' },
      { icon: '📊', title: '效果追踪', desc: '前测-后测对比、错误消除率、能力雷达图，学习成果可视化' }
    ];
    return { features };
  }
};
