<template>
  <div class="od-landing" ref="pageRoot">
<a class="skip-link" href="#top">跳到主要内容</a>
  <div class="side-rail left" data-od-id="rail-left"><span class="rail-text">Agent · Graph · Learning · Assessment</span></div>
  <div class="side-rail right" data-od-id="rail-right"><span class="rail-text">SmartMentor · Software Cup A3 · MMXXVI</span></div>

  <div class="shell">
    <div class="topbar" data-od-id="topbar">
      <div class="container topbar-inner">
        <span><b>SM / 2026</b> · Vol. 01 / Issue A3</span>
        <span class="mid"><span>Filed under <b>多智能体学习系统</b></span><span>Vue 3 · Spring Boot · Knowledge Graph</span></span>
        <span class="right"><span><span class="pulse"></span>Live · zh-CN</span><span><b>A3</b> · 评审版</span></span>
      </div>
    </div>

    <header class="nav" data-od-id="nav">
      <div class="container nav-inner">
        <a class="brand" href="#top" data-od-id="brand-home">
          <span class="brand-mark">S</span>
          <span>SmartMentor<span class="brand-meta">Software Cup A3 / Personalized Learning</span></span>
        </a>
        <nav aria-label="主导航">
          <button class="mobile-toggle" type="button" aria-label="展开导航" aria-expanded="false" data-od-id="mobile-nav-toggle">
            <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          </button>
          <ul class="nav-links" data-od-id="nav-links">
            <li><a href="#agents" data-od-id="nav-agents">五智能体<span class="num">05</span></a></li>
            <li><a href="#loop" data-od-id="nav-loop">学习闭环<span class="num">04</span></a></li>
            <li><a href="#archive" data-od-id="nav-archive">功能档案<span class="num">12</span></a></li>
            <li><a href="#teacher" data-od-id="nav-teacher">教师端<span class="num">02</span></a></li>
            <li><a href="#evidence" data-od-id="nav-evidence">证据</a></li>
          </ul>
        </nav>
        <div class="nav-side">
          <a class="nav-cta ghost" href="#archive" data-od-id="nav-secondary">产品档案</a>
          <a class="nav-cta primary" href="#top" data-od-id="nav-primary">开始阅读</a>
          <span class="status-dot" aria-hidden="true"></span>
        </div>
      </div>
    </header>

    <main>
      <section class="hero" id="top" data-od-id="hero">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">I.</span>
            <span class="meta-grp"><span>Hero / Cover Plate</span><span class="dot-mark">·</span><span>SmartMentor / Volume 01</span></span>
            <span>001 / 008</span>
          </div>
        </div>
        <div class="container hero-grid">
          <div class="hero-copy">
            <span class="label" data-reveal data-od-id="hero-label">Software Cup A3 <span class="ix">· Nº 01</span></span>
            <h1 class="display" data-reveal data-od-id="hero-title">让每一次错误回到它的<em>根因</em><span class="dot">.</span></h1>
            <p class="lead" data-reveal data-od-id="hero-lead">SmartMentor 不是普通聊天套壳。它用五个 Agent 串联诊断、知识图谱溯源、DAG 路径规划、教学资源生成与评估回环，把学生端与教师端连接成一套可演示的学习闭环。</p>
            <div class="hero-actions" data-reveal>
              <a class="btn btn-primary" href="#agents" data-od-id="hero-cta-primary">看五智能体 <svg width="15" height="15" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19L19 5M19 5H8M19 5v11" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg></a>
              <a class="btn btn-ghost" href="#loop" data-od-id="hero-cta-secondary">看学习闭环 <svg width="15" height="15" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M9 12h6M12 9v6" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg></a>
            </div>
            <div class="hero-stats" data-reveal data-od-id="hero-stats">
              <div class="stat"><span class="ring coral">5</span><span class="stat-label"><b>Agent</b>协作链</span></div>
              <div class="stat"><span class="ring">3</span><span class="stat-label"><b>Graph</b>课程图谱</span></div>
              <div class="stat"><span class="ring">12</span><span class="stat-label"><b>Views</b>学生功能</span></div>
            </div>
            <div class="hero-foot" data-reveal>
              <span class="micro">AgentOrchestrator · MAX 10 rounds</span>
              <span class="micro">30.5928° N · 114.3055° E</span>
            </div>
          </div>
          <figure class="plate hero-art" data-reveal="scale" data-od-id="hero-art">
            <span class="corner tl"></span><span class="corner tr"></span><span class="corner bl"></span><span class="corner br"></span>
            <span class="annot annot-tl">FIG. 01 / SM-A3</span>
            <span class="annot annot-tr">Plate Nº 01</span>
            <span class="annot annot-bl">DAG · root-cause</span>
            <span class="annot annot-br">Knowledge graph</span>
            <img src="/opendesign-landing/hero-collage.png" alt="SmartMentor 知识图谱编辑拼贴视觉" />
            <div class="index-rail" aria-label="学习闭环索引">
              <span><span class="n">01</span>诊断</span>
              <span class="on"><span class="n">02</span>溯源</span>
              <span><span class="n">03</span>规划</span>
              <span><span class="n">04</span>评估</span>
            </div>
          </figure>
        </div>
      </section>

      <div class="wire" aria-label="技术线索" data-od-id="signal-wire">
        <div class="container wire-inner">
          <div class="wire-left">
            <span class="wire-mark"><span class="pulse"></span></span>
            <span class="wire-title"><b>From the codebase</b><span>实时证据线</span></span>
          </div>
          <div class="wire-rows">
            <div class="marquee">
              <div class="marquee-track">
                <span class="wire-item"><strong>DiagnosticAgent</strong> · IRT</span>
                <span class="wire-item"><strong>TracingAgent</strong> · prerequisite chain</span>
                <span class="wire-item"><strong>PlanningAgent</strong> · DAG</span>
                <span class="wire-item"><strong>TeachingAgent</strong> · strategy</span>
                <span class="wire-item"><strong>EvaluationAgent</strong> · mastery</span>
                <span class="wire-item"><strong>DiagnosticAgent</strong> · IRT</span>
                <span class="wire-item"><strong>TracingAgent</strong> · prerequisite chain</span>
                <span class="wire-item"><strong>PlanningAgent</strong> · DAG</span>
                <span class="wire-item"><strong>TeachingAgent</strong> · strategy</span>
                <span class="wire-item"><strong>EvaluationAgent</strong> · mastery</span>
              </div>
            </div>
            <div class="marquee reverse">
              <div class="marquee-track">
                <span class="wire-item">computer-ai-foundation</span>
                <span class="wire-item">software-java-web</span>
                <span class="wire-item">electronic-digital-circuit</span>
                <span class="wire-item">SSE stream</span>
                <span class="wire-item">server-side scoring</span>
                <span class="wire-item">computer-ai-foundation</span>
                <span class="wire-item">software-java-web</span>
                <span class="wire-item">electronic-digital-circuit</span>
                <span class="wire-item">SSE stream</span>
                <span class="wire-item">server-side scoring</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <section class="about" id="problem" data-od-id="problem-section">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">II.</span>
            <span class="meta-grp"><span>Problem / Root Cause</span><span class="dot-mark">·</span><span>Why this is not another tutor chat</span></span>
            <span>002 / 008</span>
          </div>
          <div class="about-grid">
            <div data-reveal>
              <span class="label" data-od-id="problem-label">Problem <span class="ix">· Nº 02</span></span>
              <h2 class="display" data-od-id="problem-title">不是再刷十道题，而是找到真正<em>断裂</em>的位置<span class="dot">.</span></h2>
              <p class="lead" data-od-id="problem-copy">传统学习工具常停在“你错了这道题”。SmartMentor 的叙事改为“这道题错在哪里，根因在哪个前置知识点，应该先补哪一段路径”。页面第一屏就让评审读懂这个差异。</p>
              <div class="about-proof">
                <div class="proof-item" data-od-id="proof-root"><b>根因追溯</b>沿知识图谱前驱链回溯，不只停在错题表层。</div>
                <div class="proof-item" data-od-id="proof-profile"><b>五维画像</b>知识状态、错误模式、学习行为、认知风格、目标画像共同参与决策。</div>
                <div class="proof-item" data-od-id="proof-quality"><b>质量保障</b>RAG 锚定、LLM 自检、规则校验减少生成幻觉。</div>
              </div>
            </div>
            <figure class="plate square-art" data-reveal="right" data-od-id="problem-art">
              <span class="corner tl"></span><span class="corner br"></span>
              <span class="annot annot-tl">Plate Nº 02</span>
              <span class="annot annot-bl">Root chain</span>
              <img src="/opendesign-landing/tracing-collage.png" alt="根因溯源编辑拼贴视觉" />
              <figcaption class="side-note">从表层错误向前置依赖逐层回溯，直到找到真正需要补齐的知识节点。</figcaption>
            </figure>
          </div>
        </div>
      </section>

      <section class="capabilities" id="agents" data-od-id="agents-section">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">III.</span>
            <span class="meta-grp"><span>Agent Orchestra</span><span class="dot-mark">·</span><span>Event-driven collaboration</span></span>
            <span>003 / 008</span>
          </div>
          <div class="capabilities-grid">
            <figure class="plate square-art" data-reveal="left" data-od-id="agents-art">
              <span class="corner tl"></span><span class="corner br"></span>
              <span class="annot annot-tl">FIG. 03 / Orchestrator</span>
              <span class="annot annot-bl">MAX 10 rounds</span>
              <img src="/opendesign-landing/path-collage.png" alt="学习路径规划拼贴视觉" />
            </figure>
            <div data-reveal>
              <span class="label" data-od-id="agents-label">Capabilities <span class="ix">· Nº 03</span></span>
              <h2 class="display" data-od-id="agents-title">五个 Agent 分工明确，由编排器触发<em>事件</em><span class="dot">.</span></h2>
              <div class="agent-ledger" data-od-id="agent-ledger">
                <article class="agent-entry" data-reveal data-od-id="agent-entry-diagnostic">
                  <span class="agent-no">01</span>
                  <h3 class="agent-name">诊断 Agent</h3>
                  <p class="agent-desc">基于 IRT 思路动态出题，分析作答过程，输出薄弱知识点与初步掌握度。</p>
                  <span class="agent-tag">Diagnostic · DIAGNOSIS_COMPLETE</span>
                </article>
                <article class="agent-entry" data-reveal data-od-id="agent-entry-tracing">
                  <span class="agent-no">02</span>
                  <h3 class="agent-name">溯源 Agent</h3>
                  <p class="agent-desc">沿课程图谱前驱链回溯，把表层错误还原为根因知识点。</p>
                  <span class="agent-tag">Tracing · TRACING_COMPLETE</span>
                </article>
                <article class="agent-entry" data-reveal data-od-id="agent-entry-planning">
                  <span class="agent-no">03</span>
                  <h3 class="agent-name">规划 Agent</h3>
                  <p class="agent-desc">把薄弱点组织成 DAG，按前置关系生成可执行学习路径。</p>
                  <span class="agent-tag">Planning · DAG_PATH</span>
                </article>
                <article class="agent-entry" data-reveal data-od-id="agent-entry-evaluation">
                  <span class="agent-no">04</span>
                  <h3 class="agent-name">教学与评估</h3>
                  <p class="agent-desc">按掌握度分层生成内容，评估未达标时触发回流与补救。</p>
                  <span class="agent-tag">Teach + Evaluate · MASTERY_NOT_REACHED</span>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="method" id="loop" data-od-id="loop-section">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">IV.</span>
            <span class="meta-grp"><span>Closed Loop</span><span class="dot-mark">·</span><span>Diagnosis to mastery</span></span>
            <span>004 / 008</span>
          </div>
          <div class="method-head">
            <div data-reveal>
              <span class="label" data-od-id="loop-label">Method <span class="ix">· Nº 04</span></span>
              <h2 class="display" data-od-id="loop-title">一次学习会话从信号走向<em>闭环</em><span class="dot">.</span></h2>
            </div>
            <div class="right" data-reveal="right">
              <span class="plus">+</span>
              <p>事件不是装饰。`DIAGNOSIS_COMPLETE` 到 `TRACING_COMPLETE`，再到掌握度未达标的回流，决定了系统能否自驱动迭代。</p>
            </div>
          </div>
          <div class="method-grid">
            <article class="method-step" data-reveal data-od-id="loop-step-diagnose">
              <div class="num">I</div>
              <h4>诊断 <span class="dot">→</span></h4>
              <p>读取课程模块与学生画像，生成诊断题并分析薄弱点。</p>
              <div class="transparent-asset method-asset">
                <img src="/opendesign-landing/transparent-loop-diagnose.png" alt="诊断阶段透明拼贴素材" />
              </div>
            </article>
            <article class="method-step" data-reveal data-od-id="loop-step-trace">
              <div class="num">II</div>
              <h4>追溯 <span class="dot">→</span></h4>
              <p>触发 `DIAGNOSIS_COMPLETE`，TracingAgent 追溯前置链。</p>
              <div class="transparent-asset method-asset">
                <img src="/opendesign-landing/transparent-loop-trace.png" alt="追溯阶段透明拼贴素材" />
              </div>
            </article>
            <article class="method-step" data-reveal data-od-id="loop-step-plan">
              <div class="num">III</div>
              <h4>规划 <span class="dot">→</span></h4>
              <p>`TRACING_COMPLETE` 交给 PlanningAgent，输出 DAG 学习路径。</p>
              <div class="transparent-asset method-asset">
                <img src="/opendesign-landing/transparent-loop-plan.png" alt="规划阶段透明拼贴素材" />
              </div>
            </article>
            <article class="method-step" data-reveal data-od-id="loop-step-evaluate">
              <div class="num">IV</div>
              <h4>评估</h4>
              <p>EvaluationAgent 判断掌握度，未达标即回流教学或诊断。</p>
              <div class="transparent-asset method-asset">
                <img src="/opendesign-landing/transparent-loop-evaluate.png" alt="评估阶段透明拼贴素材" />
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="labs" id="archive" data-od-id="archive-section">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">V.</span>
            <span class="meta-grp"><span>Product Archive</span><span class="dot-mark">·</span><span>Student and teacher surfaces</span></span>
            <span>005 / 008</span>
          </div>
          <div class="labs-head">
            <div data-reveal>
              <span class="label" data-od-id="archive-label">Archive <span class="ix">· Nº 05</span></span>
              <h2 class="display" data-od-id="archive-title">学生端与教师端都不是展示页，而是可串联的<em>产品功能</em><span class="dot">.</span></h2>
            </div>
            <div class="pills" data-reveal="right" role="tablist" aria-label="功能筛选">
              <button class="pill active" type="button" role="tab" aria-selected="true" data-filter="all" data-od-id="filter-all">全部 12</button>
              <button class="pill" type="button" role="tab" aria-selected="false" data-filter="student" data-od-id="filter-student">学生端 09</button>
              <button class="pill" type="button" role="tab" aria-selected="false" data-filter="teacher" data-od-id="filter-teacher">教师端 03</button>
              <button class="pill" type="button" role="tab" aria-selected="false" data-filter="realtime" data-od-id="filter-realtime">实时 02</button>
            </div>
          </div>
          <div class="labs-grid" data-od-id="archive-grid">
            <article class="lab" data-kind="student" data-reveal data-od-id="archive-diagnostic">
              <div class="transparent-asset lab-art">
                <span class="badge">Student</span>
                <img src="/opendesign-landing/transparent-archive-diagnostic.png" alt="诊断测试透明拼贴素材" />
              </div>
              <div class="num-row"><span>Nº 01</span><span>Diagnostic</span></div>
              <h4>诊断测试</h4>
              <p>5-8 道题建立当前知识状态，并把答案快照交给服务端评分。</p>
            </article>
            <article class="lab" data-kind="student" data-reveal data-od-id="archive-tracing">
              <div class="transparent-asset lab-art">
                <span class="badge">Graph</span>
                <img src="/opendesign-landing/transparent-archive-tracing.png" alt="溯因分析透明拼贴素材" />
              </div>
              <div class="num-row"><span>Nº 02</span><span>Tracing</span></div>
              <h4>溯因分析</h4>
              <p>将错因映射回知识图谱前置依赖，解释为什么不会。</p>
            </article>
            <article class="lab" data-kind="student" data-reveal data-od-id="archive-path">
              <div class="transparent-asset lab-art">
                <span class="badge">Path</span>
                <img src="/opendesign-landing/transparent-archive-path.png" alt="学习路径透明拼贴素材" />
              </div>
              <div class="num-row"><span>Nº 03</span><span>DAG</span></div>
              <h4>学习路径</h4>
              <p>路径节点、AI 伴学、练习、检查点和补救节点形成连续流程。</p>
            </article>
            <article class="lab" data-kind="student realtime" data-reveal data-od-id="archive-report">
              <div class="transparent-asset lab-art">
                <span class="badge">Report</span>
                <img src="/opendesign-landing/transparent-archive-report.png" alt="学习报告透明拼贴素材" />
              </div>
              <div class="num-row"><span>Nº 04</span><span>Evidence</span></div>
              <h4>学习报告</h4>
              <p>前测-后测、错误消除、掌握度曲线与能力雷达图共同证明效果。</p>
            </article>
            <article class="lab" data-kind="teacher realtime" data-reveal data-od-id="archive-teacher">
              <div class="transparent-asset lab-art">
                <span class="badge">Teacher</span>
                <img src="/opendesign-landing/transparent-archive-teacher.png" alt="班级干预透明拼贴素材" />
              </div>
              <div class="num-row"><span>Nº 05</span><span>Intervention</span></div>
              <h4>班级干预</h4>
              <p>教师从热力图下钻到薄弱学生，再生成分层作业。</p>
            </article>
          </div>
          <div class="labs-foot">
            <div class="progress" aria-hidden="true"><span class="on"></span><span class="on"></span><span class="on"></span><span class="on"></span><span class="on"></span><span></span><span></span><span></span></div>
            <span>05 / 12 PRODUCT SURFACES</span>
          </div>
        </div>
      </section>

      <section class="teacher" id="teacher" data-od-id="teacher-section">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">VI.</span>
            <span class="meta-grp"><span>Teacher Intervention</span><span class="dot-mark">·</span><span>From heatmap to action</span></span>
            <span>006 / 008</span>
          </div>
          <div class="teacher-grid">
            <figure class="plate teacher-plate" data-reveal="left" data-od-id="teacher-art">
              <span class="corner tl"></span><span class="corner tr"></span><span class="corner bl"></span><span class="corner br"></span>
              <span class="annot annot-tl">Plate Nº 06</span>
              <span class="annot annot-bl">Classroom insight</span>
              <img src="/opendesign-landing/teacher-collage.png" alt="教师端班级洞察拼贴视觉" />
            </figure>
            <div class="teacher-panel" data-reveal data-od-id="teacher-panel">
              <div class="panel-head">
                <h3>班级薄弱点下钻</h3>
                <span class="micro">Week 26 · 4 signals</span>
              </div>
              <div class="table" aria-label="班级薄弱点示例">
                <div class="row" data-od-id="teacher-row-1"><b>Java Web 路由</b><span>18 人</span><span class="severity"><span style="width:78%"></span></span><button class="row-action" type="button">生成作业</button></div>
                <div class="row" data-od-id="teacher-row-2"><b>数字电路译码器</b><span>12 人</span><span class="severity"><span style="width:56%"></span></span><button class="row-action" type="button">生成作业</button></div>
                <div class="row" data-od-id="teacher-row-3"><b>AI 基础检索</b><span>9 人</span><span class="severity"><span style="width:43%"></span></span><button class="row-action" type="button">生成作业</button></div>
              </div>
              <p class="lead" style="font-size:18px;margin-top:28px;">教师端不只是看图。热力图下钻到薄弱学生，再触发分层作业，才构成完整教学干预。</p>
            </div>
          </div>
        </div>
      </section>

      <section class="tight" id="evidence" data-od-id="evidence-section">
        <div class="work">
          <div class="work-rule">
            <span class="roman">VII.</span>
            <span>Selected Evidence · 2026 Catalog</span>
            <span>007 / 008</span>
          </div>
          <div class="evidence-layout">
            <div class="work-copy" data-reveal>
              <span class="label">Evidence</span>
              <h2>两条路径证明系统已经从演示走向<em>可联调</em><span class="dot">.</span></h2>
              <p class="evidence-note">把学生侧和教师侧拆成两条证据线：一条证明学习闭环能跑通，一条证明教学干预能落地。</p>
              <a class="work-link" href="#top">回到封面</a>
            </div>
            <div class="evidence-paths">
              <article class="evidence-path" data-reveal data-od-id="work-student">
                <span class="evidence-num">01</span>
                <div>
                  <div class="label-row"><span>Student loop</span><span>Vue 3 · Vite</span></div>
                  <h3>学生端闭环</h3>
                  <p>仪表盘、诊断历史、溯因分析、学习路径、课程节点、AI 对话、学习报告形成一条完整的 5 分钟演示线。</p>
                  <ul class="evidence-flow" aria-label="学生端证据链">
                    <li>诊断</li>
                    <li>追溯</li>
                    <li>路径</li>
                    <li>报告</li>
                  </ul>
                </div>
                <figure class="evidence-asset" aria-label="学生端闭环视觉">
                  <img src="/opendesign-landing/transparent-work-student.png" alt="学生端闭环透明拼贴素材" />
                </figure>
              </article>
              <article class="evidence-path" data-reveal data-od-id="work-teacher">
                <span class="evidence-num">02</span>
                <div>
                  <div class="label-row"><span>Teacher loop</span><span>Spring Boot · SSE</span></div>
                  <h3>教师端闭环</h3>
                  <p>班级管理、知识热力图、薄弱学生下钻、AI 分层作业和班级周报把诊断结果转成可执行干预。</p>
                  <ul class="evidence-flow" aria-label="教师端证据链">
                    <li>班级</li>
                    <li>热力图</li>
                    <li>下钻</li>
                    <li>干预</li>
                  </ul>
                </div>
                <figure class="evidence-asset" aria-label="教师端闭环视觉">
                  <img src="/opendesign-landing/transparent-work-teacher.png" alt="教师端闭环透明拼贴素材" />
                </figure>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="testimonial" data-od-id="testimonial-section">
        <div class="container">
          <div class="sec-rule">
            <span class="roman">VIII.</span>
            <span class="meta-grp"><span>Review Narrative</span><span class="dot-mark">·</span><span>What judges should read first</span></span>
            <span>008 / 008</span>
          </div>
          <div class="testimonial-grid">
            <div data-reveal>
              <span class="label" data-od-id="quote-label">Review note <span class="ix">· Nº 08</span></span>
              <h2 data-od-id="quote-copy">“评审不需要再从按钮堆里寻找亮点；页面第一屏就能读到项目真正的技术抓手：五智能体、知识图谱、DAG 路径与评估回环。”</h2>
              <div class="author" data-od-id="quote-author">
                <span class="avatar">S</span>
                <p>SmartMentor 项目材料提炼<br><span>中国软件杯 A3 展示叙事</span></p>
              </div>
              <div class="evidence-list">
                <div class="evidence-item" data-od-id="evidence-agent"><b>AgentCollaborationConfig</b>诊断完成触发追溯，追溯完成触发规划，掌握度未达标触发教学补救。</div>
                <div class="evidence-item" data-od-id="evidence-graph"><b>Knowledge Graph</b>运行时加载 computer-ai-foundation、software-java-web、electronic-digital-circuit 三类课程图谱。</div>
                <div class="evidence-item" data-od-id="evidence-stream"><b>SSE Chat</b>AI 伴学通过流式输出降低等待感，绑定当前学习上下文。</div>
                <div class="evidence-item" data-od-id="evidence-security"><b>Server Scoring</b>答题评分基于服务端快照，不信任前端传入正确答案。</div>
              </div>
            </div>
            <figure class="transparent-asset ambient-asset" data-reveal="right" data-od-id="quote-art">
              <span class="corner tl"></span><span class="corner br"></span>
              <span class="annot annot-tl">FIN. / SM</span>
              <img src="/opendesign-landing/transparent-quote-root.png" alt="评审叙事透明背景拼贴素材" />
              <figcaption class="ambient-copy">根因<span>透明拼贴退入纸张肌理，让评审叙事保持在前景。</span></figcaption>
            </figure>
          </div>
        </div>
      </section>

      <div class="cta" id="contact" data-od-id="cta-section">
        <div class="container">
          <div class="cta-grid">
            <div data-reveal>
              <span class="label" data-od-id="cta-label">Final Route <span class="ix">· FIN</span></span>
              <h2 class="display" data-od-id="cta-title">把一次错题，变成下一条<em>学习路径</em><span class="dot">.</span></h2>
              <p class="lead" data-od-id="cta-copy">从注册进入画像引导，从诊断进入第一条路径；学生侧看到补救节点，教师侧看到班级干预建议。主页的任务是让这条链路在 10 秒内被看懂。</p>
              <div class="cta-actions">
                <a class="btn btn-primary" href="#agents" data-od-id="cta-primary">查看五智能体 <svg width="15" height="15" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19L19 5M19 5H8M19 5v11" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg></a>
                <a class="btn btn-ghost" href="#top" data-od-id="cta-secondary">回到顶部</a>
              </div>
              <div class="cta-foot"><span><span class="pulse"></span>评审阅读路径</span><span>诊断 → 溯源 → 规划 → 干预</span><span>Software Cup A3</span></div>
            </div>
            <figure class="transparent-asset ambient-asset" data-reveal="right" data-od-id="cta-art">
              <span class="corner tl"></span><span class="corner tr"></span><span class="corner bl"></span><span class="corner br"></span>
              <span class="annot annot-tl">Plate Nº 08</span>
              <span class="annot annot-br">FIN.</span>
              <img src="/opendesign-landing/transparent-cta-path.png" alt="收尾路径透明背景拼贴素材" />
              <figcaption class="ambient-copy">路径<span>收尾视觉以透明素材融入背景过渡，不再作为另一张独立照片出现。</span></figcaption>
            </figure>
          </div>
        </div>
      </div>
    </main>

    <footer data-od-id="footer">
      <div class="container">
        <div class="foot-grid">
          <div class="foot-brand" data-od-id="footer-brand">
            <a class="brand" href="#top"><span class="brand-mark">S</span><span>SmartMentor</span></a>
            <p>基于大模型的个性化资源生成与学习多智能体系统。主页改造方向：更少按钮堆叠，更强叙事证据，更明确的学习闭环。</p>
          </div>
          <div class="foot-col"><h5>学生端</h5><ul><li><a href="#archive">诊断测试</a></li><li><a href="#problem">溯因分析</a></li><li><a href="#loop">学习路径</a></li><li><a href="#archive">AI 对话</a></li></ul></div>
          <div class="foot-col"><h5>教师端</h5><ul><li><a href="#teacher">班级热力图</a></li><li><a href="#teacher">分层作业</a></li><li><a href="#teacher">学生预警</a></li><li><a href="#teacher">周报</a></li></ul></div>
          <div class="foot-col"><h5>引擎</h5><ul><li><a href="#agents">AgentOrchestrator</a></li><li><a href="#loop">KnowledgeGraph</a></li><li><a href="#evidence">DeepSeek</a></li><li><a href="#evidence">SSE</a></li></ul></div>
          <div class="foot-col"><h5>质量</h5><ul><li><a href="#evidence">RAG 锚定</a></li><li><a href="#evidence">LLM 自检</a></li><li><a href="#evidence">规则校验</a></li><li><a href="#evidence">服务端评分</a></li></ul></div>
        </div>
        <div class="foot-bottom">
          <span><span class="pulse"></span><b>SmartMentor</b> · Software Cup A3 · MMXXVI</span>
          <span>Diagnostic · Tracing · Planning · Teaching · Evaluation</span>
        </div>
        <div class="mega" data-reveal data-od-id="footer-mega">Smart <em>Mentor</em>.</div>
      </div>
    </footer>
  </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const pageRoot = ref(null)
const cleanups = []
let revealObserver
let filterTimer

function addCleanup(cleanup) {
  cleanups.push(cleanup)
}

function addListener(target, event, handler, options) {
  target?.addEventListener?.(event, handler, options)
  addCleanup(() => target?.removeEventListener?.(event, handler, options))
}

function addMediaListener(mediaQueryList, handler) {
  if (mediaQueryList.addEventListener) {
    mediaQueryList.addEventListener('change', handler)
    addCleanup(() => mediaQueryList.removeEventListener('change', handler))
    return
  }
  mediaQueryList.addListener(handler)
  addCleanup(() => mediaQueryList.removeListener(handler))
}

function findHashTarget(root, hash) {
  if (!hash || hash.length < 2) return null
  const id = hash.slice(1)
  return root.querySelector(`#${id}`)
}

onMounted(() => {
  const root = pageRoot.value
  if (!root) return

  const nav = root.querySelector('.nav')
  const links = root.querySelector('.nav-links')
  const toggle = root.querySelector('.mobile-toggle')
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
  let lastY = window.scrollY || 0
  const showTop = 110
  const delta = 6
  let ticking = false

  function closeMobileNav() {
    links?.classList.remove('open')
    toggle?.setAttribute('aria-expanded', 'false')
  }

  function handleScroll() {
    const y = window.scrollY || 0
    const d = y - lastY
    nav?.classList.toggle('is-docked', y > 20)
    if (nav && !reduceMotion.matches) {
      if (y <= showTop || d < -delta) nav.classList.remove('is-hidden')
      else if (d > delta) nav.classList.add('is-hidden')
      if (links?.classList.contains('open')) nav.classList.remove('is-hidden')
    }
    lastY = y
    ticking = false
  }

  function requestScrollUpdate() {
    if (ticking) return
    ticking = true
    window.requestAnimationFrame(handleScroll)
  }

  if (toggle && links) {
    addListener(toggle, 'click', () => {
      const open = links.classList.toggle('open')
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false')
    })
  }

  addListener(root, 'click', (event) => {
    const anchor = event.target.closest?.('a[href^="#"]')
    if (!anchor || !root.contains(anchor)) return
    const href = anchor.getAttribute('href')
    const target = findHashTarget(root, href)
    if (!target) return
    event.preventDefault()
    closeMobileNav()
    target.scrollIntoView({
      behavior: reduceMotion.matches ? 'auto' : 'smooth',
      block: 'start'
    })
  })

  addListener(window, 'scroll', requestScrollUpdate, { passive: true })
  addMediaListener(reduceMotion, () => {
    nav?.classList.remove('is-hidden')
    handleScroll()
  })
  handleScroll()

  const revealElements = root.querySelectorAll('[data-reveal]:not([data-revealed])')
  if (revealElements.length) {
    if (reduceMotion.matches) {
      revealElements.forEach((element) => {
        element.dataset.revealed = 'true'
      })
    } else {
      revealObserver = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          entry.target.dataset.revealed = 'true'
          revealObserver.unobserve(entry.target)
        })
      }, { threshold: 0.12, rootMargin: '0px 0px -8% 0px' })
      revealElements.forEach((element) => revealObserver.observe(element))
      addCleanup(() => revealObserver?.disconnect())
    }
  }

  const pills = Array.from(root.querySelectorAll('.pill[data-filter]'))
  const cards = Array.from(root.querySelectorAll('.lab[data-kind]'))

  function setCards(filter) {
    cards.forEach((card) => {
      const kinds = (card.dataset.kind || '').split(/\s+/)
      const hidden = filter !== 'all' && !kinds.includes(filter)
      card.classList.toggle('is-hidden', hidden)
      card.classList.remove('is-filtering')
      card.setAttribute('aria-hidden', hidden ? 'true' : 'false')
    })
  }

  function applyFilter(pill, animate) {
    const filter = pill.dataset.filter
    pills.forEach((item) => {
      const selected = item === pill
      item.classList.toggle('active', selected)
      item.setAttribute('aria-selected', selected ? 'true' : 'false')
    })

    if (!animate || reduceMotion.matches) {
      setCards(filter)
      return
    }

    window.clearTimeout(filterTimer)
    cards.forEach((card) => card.classList.add('is-filtering'))
    filterTimer = window.setTimeout(() => {
      cards.forEach((card) => {
        const kinds = (card.dataset.kind || '').split(/\s+/)
        const hidden = filter !== 'all' && !kinds.includes(filter)
        card.classList.toggle('is-hidden', hidden)
        card.setAttribute('aria-hidden', hidden ? 'true' : 'false')
      })
      window.requestAnimationFrame(() => {
        cards.forEach((card) => card.classList.remove('is-filtering'))
      })
    }, 130)
  }

  if (pills.length && cards.length) {
    pills.forEach((pill, index) => {
      const onClick = () => applyFilter(pill, true)
      const onKeydown = (event) => {
        if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return
        event.preventDefault()
        const offset = event.key === 'ArrowRight' ? 1 : -1
        const next = pills[(index + offset + pills.length) % pills.length]
        next.focus()
        applyFilter(next, true)
      }
      addListener(pill, 'click', onClick)
      addListener(pill, 'keydown', onKeydown)
    })
    applyFilter(pills.find((pill) => pill.classList.contains('active')) || pills[0], false)
  }
})

onUnmounted(() => {
  window.clearTimeout(filterTimer)
  revealObserver?.disconnect()
  while (cleanups.length) cleanups.pop()?.()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter+Tight:wght@500;600;700;800;900&family=Inter:wght@300;400;500;600&family=JetBrains+Mono:wght@400;500&family=Noto+Serif+SC:wght@500;700;900&display=swap');

.od-landing {
      --bg: #ffffff;
      --surface: #f7f7f7;
      --surface-warm: #eeeeee;
      --fg: #111111;
      --fg-2: #3a3a3a;
      --muted: #707070;
      --meta: #111111;
      --border: #d9d9d9;
      --border-soft: #eeeeee;
      --accent: #111111;
      --accent-on: #ffffff;
      --accent-hover: color-mix(in oklab, var(--accent), black 8%);
      --accent-active: color-mix(in oklab, var(--accent), black 14%);
      --success: #168a46;
      --warn: #b7791f;
      --danger: #c53030;
      --font-display: "Helvetica Neue", Arial, sans-serif;
      --font-body: "Helvetica Neue", Arial, sans-serif;
      --font-mono: "SF Mono", ui-monospace, Menlo, monospace;
      --text-xs: 12px;
      --text-sm: 14px;
      --text-base: 16px;
      --text-lg: 18px;
      --text-xl: 24px;
      --text-2xl: 36px;
      --text-3xl: 54px;
      --text-4xl: 76px;
      --leading-body: 1.52;
      --leading-tight: 1.06;
      --tracking-display: -0.025em;
      --space-1: 4px;
      --space-2: 8px;
      --space-3: 12px;
      --space-4: 16px;
      --space-5: 20px;
      --space-6: 24px;
      --space-8: 32px;
      --space-12: 48px;
      --section-y-desktop: 96px;
      --section-y-tablet: 68px;
      --section-y-phone: 48px;
      --radius-sm: 4px;
      --radius-md: 8px;
      --radius-lg: 12px;
      --radius-pill: 9999px;
      --elev-flat: none;
      --elev-ring: 0 0 0 1px var(--border);
      --elev-raised: 0 16px 40px rgba(0, 0, 0, 0.10);
      --focus-ring: 0 0 0 3px rgba(17, 17, 17, 0.18);
      --motion-fast: 150ms;
      --motion-base: 240ms;
      --ease-standard: cubic-bezier(0.2, 0, 0, 1);
      --container-max: 1180px;
      --container-gutter-desktop: 36px;
      --container-gutter-tablet: 24px;
      --container-gutter-phone: 16px;
    }

.od-landing {
      --paper: color-mix(in oklab, var(--surface) 82%, var(--warn) 18%);
      --paper-warm: color-mix(in oklab, var(--surface-warm) 78%, var(--warn) 14%);
      --paper-dark: color-mix(in oklab, var(--border-soft) 70%, var(--warn) 22%);
      --bone: color-mix(in oklab, var(--surface) 94%, var(--warn) 6%);
      --ink: var(--fg);
      --ink-soft: var(--fg-2);
      --ink-mute: var(--muted);
      --ink-faint: color-mix(in oklab, var(--muted) 72%, var(--surface) 28%);
      --coral: color-mix(in oklab, var(--danger) 62%, var(--warn) 38%);
      --coral-soft: color-mix(in oklab, var(--danger) 45%, var(--surface) 55%);
      --mustard: var(--warn);
      --olive: color-mix(in oklab, var(--success) 42%, var(--warn) 58%);
      --line: color-mix(in oklab, var(--fg), transparent 84%);
      --line-soft: color-mix(in oklab, var(--fg), transparent 92%);
      --line-faint: color-mix(in oklab, var(--fg), transparent 96%);
      --shadow-paper: 0 30px 60px -30px color-mix(in oklab, var(--fg), transparent 80%);
      --display: "Inter Tight", var(--font-display), "Noto Serif SC", sans-serif;
      --serif: "Noto Serif SC", "Songti SC", serif;
      --body: "Inter", var(--font-body), "Noto Sans CJK SC", sans-serif;
      --mono: "JetBrains Mono", var(--font-mono);
    }

    * { box-sizing: border-box; }
    .od-landing { scroll-behavior: smooth; background: var(--paper); }
    .od-landing {
      margin: 0;
      min-width: 320px;
      color: var(--ink);
      background: var(--paper);
      font-family: var(--body);
      font-size: var(--text-base);
      line-height: var(--leading-body);
      overflow-x: hidden;
      -webkit-font-smoothing: antialiased;
      text-rendering: optimizeLegibility;
    }

    .od-landing::before {
      content: "";
      position: fixed;
      inset: 0;
      z-index: 1;
      pointer-events: none;
      opacity: .8;
      mix-blend-mode: multiply;
      background:
        radial-gradient(circle at 13% 18%, color-mix(in oklab, var(--warn), transparent 91%) 0, transparent 30%),
        radial-gradient(circle at 88% 72%, color-mix(in oklab, var(--olive), transparent 92%) 0, transparent 34%),
        repeating-linear-gradient(90deg, color-mix(in oklab, var(--ink), transparent 97%) 0 1px, transparent 1px 52px),
        repeating-linear-gradient(0deg, color-mix(in oklab, var(--ink), transparent 98%) 0 1px, transparent 1px 52px);
    }

    a { color: inherit; text-decoration: none; }
    img { display: block; max-width: 100%; }
    button { font: inherit; }

    /* Keyboard focus — coral hairline ring, consistent across all interactive
       elements. No focus styling existed before; keyboard users had no cue. */
    :focus-visible {
      outline: 2px solid var(--coral);
      outline-offset: 3px;
    }
    :focus:not(:focus-visible) { outline: none; }

    .skip-link {
      position: fixed;
      top: -60px;
      left: var(--space-4);
      z-index: 100;
      padding: 12px 18px;
      background: var(--ink);
      color: var(--paper);
      font: 800 12px/1 var(--display);
      letter-spacing: .08em;
      text-transform: uppercase;
      border-radius: var(--radius-sm);
      transition: top var(--motion-fast) var(--ease-standard);
    }
    .skip-link:focus { top: var(--space-4); }

    .shell { position: relative; z-index: 2; }
    .container {
      width: min(var(--container-max), calc(100% - var(--container-gutter-desktop) * 2));
      margin: 0 auto;
    }

    .side-rail {
      position: fixed;
      top: 0;
      bottom: 0;
      width: 36px;
      z-index: 3;
      pointer-events: none;
      display: grid;
      place-items: center;
      border-color: var(--line-faint);
    }
    .side-rail.left { left: 0; border-right: 1px solid var(--line-faint); }
    .side-rail.right { right: 0; border-left: 1px solid var(--line-faint); }
    .rail-text {
      writing-mode: vertical-rl;
      font: 600 10px/1 var(--display);
      letter-spacing: .42em;
      text-transform: uppercase;
      color: var(--ink-faint);
      white-space: nowrap;
    }
    .side-rail.right .rail-text { transform: rotate(180deg); }

    .topbar {
      position: relative;
      z-index: 4;
      background: var(--paper);
      border-bottom: 1px solid var(--line);
    }
    .topbar-inner {
      min-height: 42px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-6);
      color: var(--ink-faint);
      font: 600 10.5px/1.3 var(--display);
      letter-spacing: .18em;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .topbar b { color: var(--ink); }
    .topbar .mid { display: inline-flex; gap: var(--space-8); }
    .topbar .right { display: inline-flex; align-items: center; gap: var(--space-5); }
    .pulse {
      display: inline-block;
      width: 6px;
      height: 6px;
      margin-right: var(--space-2);
      border-radius: 50%;
      background: var(--coral);
      animation: pulse 2.4s ease-in-out infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: .35; transform: scale(.82); }
    }

    .nav {
      position: sticky;
      top: 0;
      z-index: 50;
      background: color-mix(in oklab, var(--paper), transparent 8%);
      border-bottom: 1px solid transparent;
      transition: transform 180ms cubic-bezier(0.23, 1, 0.32, 1), border-color 150ms var(--ease-standard);
      backdrop-filter: blur(14px);
      will-change: transform;
    }
    .nav.is-hidden { transform: translateY(-100%); }
    .nav.is-docked { border-bottom-color: var(--line-soft); }
    .nav-inner {
      min-height: 84px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-6);
    }
    .brand {
      display: inline-flex;
      align-items: center;
      gap: var(--space-4);
      min-width: 0;
      font-family: var(--display);
      font-weight: 800;
      letter-spacing: var(--tracking-display);
    }
    .brand-mark {
      width: 40px;
      height: 40px;
      display: grid;
      place-items: center;
      border: 1.5px solid var(--ink);
      border-radius: 50%;
      font-family: var(--serif);
      font-size: 20px;
      font-weight: 900;
      line-height: 1;
      background: transparent;
    }
    .brand-meta {
      display: block;
      margin-top: var(--space-1);
      color: var(--ink-faint);
      font: 600 10px/1 var(--display);
      letter-spacing: .18em;
      text-transform: uppercase;
    }
    .nav-links {
      display: flex;
      gap: var(--space-1);
      align-items: center;
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .nav-links a {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      padding: 10px 12px;
      border-radius: var(--radius-pill);
      color: var(--ink-soft);
      font: 700 12px/1 var(--display);
      letter-spacing: .08em;
      text-transform: uppercase;
      transition: background var(--motion-fast) var(--ease-standard), color var(--motion-fast) var(--ease-standard);
    }
    .nav-links a:hover { background: color-mix(in oklab, var(--ink), transparent 94%); color: var(--ink); }
    .nav-links .num {
      color: var(--coral);
      font-family: var(--mono);
      letter-spacing: 0;
    }
    .nav-side { display: inline-flex; gap: var(--space-3); align-items: center; }
    .btn, .nav-cta, .pill {
      transition: transform var(--motion-fast) var(--ease-standard), background var(--motion-fast) var(--ease-standard), color var(--motion-fast) var(--ease-standard), border-color var(--motion-fast) var(--ease-standard);
    }
    .nav-cta, .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      min-height: 44px;
      padding: 0 18px;
      border-radius: var(--radius-pill);
      border: 1px solid var(--line);
      font: 800 13px/1 var(--display);
      letter-spacing: .05em;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .btn-primary, .nav-cta.primary {
      background: var(--coral);
      border-color: var(--coral);
      color: var(--accent-on);
      box-shadow: 0 14px 26px -16px var(--coral);
    }
    .btn-ghost, .nav-cta.ghost { background: transparent; color: var(--ink); }
    .btn:hover, .nav-cta:hover { transform: translateY(-1px); }
    .btn-primary:hover, .nav-cta.primary:hover { background: color-mix(in oklab, var(--coral), var(--ink) 10%); }
    .status-dot {
      width: 9px;
      height: 9px;
      border-radius: 50%;
      background: var(--mustard);
    }
    .mobile-toggle { display: none; }

    section { position: relative; padding: 130px 0; }
    section.tight { padding: 96px 0; }
    .sec-rule {
      border-top: 1px solid var(--line);
      padding-top: 15px;
      margin-bottom: 58px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-4);
      color: var(--ink-faint);
      font: 500 10.5px/1.3 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
    }
    .roman {
      color: var(--coral);
      font-family: var(--serif);
      font-weight: 900;
      font-size: 15px;
      letter-spacing: 0;
      text-transform: none;
    }
    .meta-grp {
      display: inline-flex;
      gap: var(--space-5);
      align-items: center;
      justify-content: center;
      text-align: center;
    }
    .dot-mark { color: var(--coral); }

    .label {
      display: inline-flex;
      align-items: center;
      gap: var(--space-3);
      margin-bottom: var(--space-8);
      color: var(--coral);
      font: 800 11px/1 var(--display);
      letter-spacing: .22em;
      text-transform: uppercase;
    }
    .label::before {
      content: "";
      display: inline-block;
      width: 18px;
      height: 1px;
      background: var(--coral);
    }
    .ix {
      color: var(--ink-faint);
      font-family: var(--mono);
      letter-spacing: .04em;
    }
    .display {
      margin: 0;
      font-family: var(--display);
      font-weight: 900;
      letter-spacing: var(--tracking-display);
      line-height: var(--leading-tight);
      text-wrap: balance;
    }
    h1.display { font-size: clamp(56px, 7.4vw, 104px); max-width: 10.5ch; }
    h2.display, .work-copy h2, .testimonial h2 { font-size: clamp(40px, 4.6vw, 68px); }
    .display em, .work-copy em, .testimonial em, .mega em {
      font-family: var(--serif);
      font-style: italic;
      font-weight: 900;
      letter-spacing: 0;
    }
    .dot { color: var(--coral); }
    .lead {
      margin: var(--space-6) 0 0;
      color: var(--ink-soft);
      font-size: clamp(17px, 1.4vw, 22px);
      line-height: 1.58;
      max-width: 62ch;
    }
    .micro {
      color: var(--ink-faint);
      font: 500 11px/1.45 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
    }
    .code-inline {
      font-family: var(--mono);
      background: color-mix(in oklab, var(--ink), transparent 94%);
      border: 1px solid var(--line-soft);
      border-radius: var(--radius-sm);
      padding: 2px 6px;
    }

    .hero {
      padding-top: 72px;
      min-height: calc(100vh - 126px);
    }
    .hero-grid {
      display: grid;
      grid-template-columns: .82fr 1.18fr;
      gap: clamp(44px, 6vw, 96px);
      align-items: center;
    }
    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-3);
      margin-top: var(--space-8);
    }
    .hero-stats {
      margin-top: var(--space-8);
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--space-4);
      max-width: 560px;
    }
    .stat {
      display: grid;
      grid-template-columns: 38px 1fr;
      gap: var(--space-3);
      align-items: center;
    }
    .ring {
      width: 34px;
      height: 34px;
      display: grid;
      place-items: center;
      border: 1px dashed var(--line);
      border-radius: 50%;
      color: var(--ink);
      font: 700 12px/1 var(--mono);
    }
    .ring.coral {
      border-color: var(--coral);
      color: var(--coral);
    }
    .stat-label {
      display: grid;
      gap: 1px;
      color: var(--ink-mute);
      font: 600 11px/1.25 var(--display);
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .stat-label b {
      color: var(--ink);
      font-size: 14px;
      letter-spacing: 0;
      text-transform: none;
    }
    .hero-foot {
      margin-top: var(--space-8);
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-4);
      align-items: center;
    }
    .plate {
      position: relative;
      background: var(--bone);
      border: 1px solid var(--line);
      box-shadow: var(--shadow-paper);
      overflow: hidden;
    }
    .hero-art {
      height: min(760px, calc(100vh - 150px));
      min-height: 560px;
      border-radius: 22px;
    }
    .hero-art img, .square-art img, .teacher-plate img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .transparent-asset {
      position: relative;
      overflow: hidden;
      isolation: isolate;
      background:
        radial-gradient(circle at 18% 14%, color-mix(in oklab, var(--coral), transparent 84%), transparent 30%),
        linear-gradient(135deg, color-mix(in oklab, var(--paper), transparent 2%), color-mix(in oklab, var(--paper-warm), transparent 14%));
    }
    .transparent-asset img {
      position: relative;
      z-index: 2;
      display: block;
      width: 100%;
      height: 100%;
      object-fit: contain;
      padding: var(--asset-pad, 22px);
      mix-blend-mode: multiply;
      filter: saturate(.92) contrast(.96);
    }
    .method-asset {
      margin-top: var(--space-5);
      aspect-ratio: 1 / 1;
      --asset-pad: 20px;
    }
    .lab-art {
      aspect-ratio: 3 / 4;
      margin-bottom: var(--space-4);
      --asset-pad: 18px;
    }
    .lab-art .badge { position: absolute; }
    .work-art {
      margin-top: auto;
      aspect-ratio: 3 / 4;
      border-radius: 16px;
      border: 1px solid color-mix(in oklab, var(--accent-on), transparent 84%);
      background: color-mix(in oklab, var(--accent-on), transparent 94%);
      --asset-pad: 20px;
    }
    .work-art img {
      mix-blend-mode: screen;
      opacity: .92;
      filter: saturate(.72) contrast(.92);
    }
    .ambient-asset {
      aspect-ratio: 1 / 1;
      border-radius: 18px;
      box-shadow: none;
      --asset-pad: clamp(22px, 5vw, 58px);
    }
    .method-asset,
    .lab-art,
    .ambient-asset {
      border: 0;
      background: transparent;
      overflow: visible;
    }
    .ambient-asset .corner,
    .ambient-asset .annot {
      display: none;
    }
    .method-asset img,
    .lab-art img,
    .ambient-asset img {
      mix-blend-mode: normal;
      filter: none;
    }
    .ambient-asset img {
      opacity: .82;
      mask-image: none;
      -webkit-mask-image: none;
    }
    .ambient-copy {
      position: absolute;
      inset: auto 28px 28px 28px;
      display: grid;
      gap: var(--space-2);
      color: var(--ink);
      font: 900 clamp(34px, 5vw, 72px)/.86 var(--display);
      letter-spacing: -.045em;
      text-wrap: balance;
    }
    .ambient-copy span {
      max-width: 24ch;
      color: var(--ink-faint);
      font: 700 10px/1.4 var(--mono);
      letter-spacing: .14em;
      text-transform: uppercase;
    }
    .corner {
      position: absolute;
      z-index: 3;
      width: 22px;
      height: 22px;
      border-color: var(--ink);
      opacity: .54;
    }
    .corner.tl { top: 18px; left: 18px; border-top: 1px solid; border-left: 1px solid; }
    .corner.tr { top: 18px; right: 18px; border-top: 1px solid; border-right: 1px solid; }
    .corner.bl { bottom: 18px; left: 18px; border-bottom: 1px solid; border-left: 1px solid; }
    .corner.br { bottom: 18px; right: 18px; border-bottom: 1px solid; border-right: 1px solid; }
    .annot {
      position: absolute;
      z-index: 4;
      color: var(--ink-faint);
      font: 500 10px/1 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
      background: color-mix(in oklab, var(--paper), transparent 18%);
      padding: 6px 8px;
      border: 1px solid var(--line-soft);
    }
    .annot-tl { top: 28px; left: 54px; }
    .annot-tr { top: 28px; right: 54px; }
    .annot-bl { bottom: 28px; left: 54px; }
    .annot-br { bottom: 28px; right: 54px; }
    .index-rail {
      position: absolute;
      right: 24px;
      bottom: 86px;
      z-index: 4;
      min-width: 150px;
      padding-left: 18px;
      border-left: 1px solid var(--line);
      display: grid;
      gap: 9px;
      font: 700 12px/1 var(--display);
      text-transform: uppercase;
    }
    .index-rail span { display: flex; gap: var(--space-2); color: var(--ink-faint); }
    .index-rail .on { color: var(--ink); }
    .index-rail .n { color: var(--coral); font-family: var(--mono); }

    .wire {
      padding: 0;
      border-top: 1px solid var(--line);
      border-bottom: 1px solid var(--line);
      background: color-mix(in oklab, var(--paper-warm), transparent 20%);
      overflow: hidden;
    }
    .cta {
      position: relative;
      padding: 130px 0;
    }
    .wire-inner {
      min-height: 104px;
      display: grid;
      grid-template-columns: 240px minmax(0, 1fr);
      align-items: stretch;
    }
    .wire-left {
      display: flex;
      align-items: center;
      gap: var(--space-4);
      border-right: 1px solid var(--line);
    }
    .wire-mark {
      width: 34px;
      height: 34px;
      border: 1px solid var(--line);
      border-radius: 50%;
      display: grid;
      place-items: center;
    }
    .wire-title {
      display: grid;
      gap: 3px;
      color: var(--ink-faint);
      font: 600 11px/1.3 var(--display);
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .wire-title b { color: var(--ink); letter-spacing: 0; text-transform: none; font-size: 15px; }
    .wire-rows {
      min-width: 0;
      display: grid;
      align-content: center;
      gap: var(--space-2);
      padding: var(--space-4) 0 var(--space-4) var(--space-6);
    }
    .marquee {
      white-space: nowrap;
      overflow: hidden;
      color: var(--ink-faint);
      font: 600 11px/1 var(--mono);
      letter-spacing: .04em;
    }
    .marquee-track {
      display: inline-flex;
      gap: var(--space-6);
      min-width: max-content;
      animation: marquee 28s linear infinite;
    }
    .marquee.reverse .marquee-track { animation-direction: reverse; animation-duration: 32s; }
    .wire-item strong { color: var(--ink); font-family: var(--display); }
    @keyframes marquee {
      to { transform: translateX(-50%); }
    }

    .about-grid {
      display: grid;
      grid-template-columns: 1.02fr .98fr;
      gap: clamp(44px, 6vw, 88px);
      align-items: center;
    }
    .square-art {
      aspect-ratio: 1 / 1;
      border-radius: 18px;
    }
    .side-note {
      position: absolute;
      right: -30px;
      top: 18%;
      z-index: 4;
      width: 160px;
      transform: rotate(5deg);
      color: var(--ink-mute);
      font: 500 12px/1.45 var(--mono);
      background: var(--bone);
      border: 1px solid var(--line);
      padding: var(--space-4);
    }
    .about-proof {
      margin-top: var(--space-8);
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--space-3);
    }
    .proof-item {
      border-top: 1px solid var(--line);
      padding-top: var(--space-4);
      color: var(--ink-mute);
      font-size: 14px;
    }
    .proof-item b {
      display: block;
      margin-bottom: var(--space-1);
      color: var(--ink);
      font-family: var(--display);
      font-size: 18px;
      letter-spacing: var(--tracking-display);
    }

    .capabilities-grid {
      display: grid;
      grid-template-columns: .72fr 1.28fr;
      gap: clamp(40px, 5vw, 78px);
      align-items: start;
    }
    .agent-ledger {
      margin-top: var(--space-8);
      border-top: 1px solid var(--line);
      counter-reset: agent;
    }
    .agent-entry {
      display: grid;
      grid-template-columns: 74px minmax(120px, .42fr) minmax(0, 1fr) 152px;
      gap: clamp(16px, 2.6vw, 34px);
      align-items: baseline;
      padding: 26px 0 25px;
      border-bottom: 1px solid var(--line-soft);
      position: relative;
    }
    .agent-entry::before {
      content: "";
      position: absolute;
      left: 34px;
      top: 0;
      bottom: 0;
      width: 1px;
      background: repeating-linear-gradient(180deg, var(--line) 0 5px, transparent 5px 12px);
      opacity: .65;
    }
    .agent-no {
      position: relative;
      z-index: 1;
      width: 68px;
      color: var(--coral);
      font: 900 42px/.82 var(--serif);
      letter-spacing: -.04em;
      background: var(--paper);
    }
    .agent-name {
      margin: 0;
      color: var(--ink);
      font: 900 clamp(25px, 2.4vw, 36px)/.98 var(--display);
      letter-spacing: var(--tracking-display);
      text-wrap: balance;
    }
    .agent-desc {
      margin: 0;
      color: var(--ink-mute);
      font-size: 16px;
      max-width: 34ch;
    }
    .agent-tag {
      justify-self: end;
      color: var(--ink-faint);
      font: 700 10.5px/1.35 var(--mono);
      letter-spacing: .12em;
      text-transform: uppercase;
      text-align: right;
    }
    .agent-tag::before {
      content: "";
      display: block;
      width: 28px;
      height: 1px;
      margin: 0 0 8px auto;
      background: var(--coral);
    }
    .num {
      display: flex;
      justify-content: space-between;
      gap: var(--space-3);
      margin-bottom: var(--space-8);
      color: var(--coral);
      font: 900 15px/1 var(--serif);
    }
    .tag {
      color: var(--ink-faint);
      font: 700 10px/1 var(--display);
      letter-spacing: .18em;
      text-transform: uppercase;
    }
    .lab h4, .method-step h4 {
      margin: 0;
      color: var(--ink);
      font-family: var(--display);
      letter-spacing: var(--tracking-display);
      line-height: 1.08;
    }
    .lab p, .method-step p {
      margin: var(--space-4) 0 0;
      color: var(--ink-mute);
    }

    .labs-head {
      display: grid;
      grid-template-columns: 1fr auto;
      align-items: end;
      gap: var(--space-8);
      margin-bottom: var(--space-8);
    }
    .pills {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-2);
      justify-content: flex-end;
    }
    .pill {
      min-height: 38px;
      border: 1px solid var(--line);
      border-radius: var(--radius-pill);
      background: transparent;
      color: var(--ink-soft);
      padding: 0 16px;
      cursor: pointer;
      font: 800 11px/1 var(--display);
      letter-spacing: .08em;
      text-transform: uppercase;
    }
    .pill:hover { background: color-mix(in oklab, var(--ink), transparent 95%); }
    .pill.active { background: var(--coral); border-color: var(--coral); color: var(--accent-on); }
    .labs-grid {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: var(--space-4);
    }
    .lab {
      position: relative;
      border-top: 1px solid var(--line);
      padding-top: var(--space-4);
    }
    .lab.is-hidden { display: none; }
    .badge {
      position: absolute;
      top: 12px;
      left: 12px;
      z-index: 2;
      padding: 6px 10px;
      border-radius: var(--radius-pill);
      background: color-mix(in oklab, var(--bone), transparent 7%);
      border: 1px solid var(--line-soft);
      color: var(--ink);
      font: 800 10px/1 var(--display);
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .num-row {
      display: flex;
      justify-content: space-between;
      color: var(--ink-faint);
      font: 500 10px/1 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
      margin-bottom: var(--space-3);
    }
    .lab h4 { font-size: 22px; }
    .lab p { font-size: 14px; }
    .labs-foot {
      margin-top: var(--space-8);
      display: flex;
      justify-content: space-between;
      gap: var(--space-6);
      align-items: center;
      color: var(--ink-faint);
      font: 500 11px/1 var(--mono);
      text-transform: uppercase;
    }
    .progress {
      flex: 1;
      display: grid;
      grid-template-columns: repeat(8, 1fr);
      gap: var(--space-2);
    }
    .progress span {
      height: 2px;
      background: var(--line);
    }
    .progress span.on { background: var(--coral); }

    .method-head {
      display: grid;
      grid-template-columns: 1fr minmax(260px, 420px);
      gap: var(--space-8);
      align-items: end;
      margin-bottom: var(--space-12);
    }
    .plus {
      display: block;
      color: var(--coral);
      font-family: var(--serif);
      font-size: 54px;
      line-height: 1;
      margin-bottom: var(--space-3);
    }
    .method-head .right p {
      margin: 0;
      color: var(--ink-mute);
      font-size: 18px;
    }
    .method-grid {
      position: relative;
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: var(--space-5);
    }
    .method-grid::before {
      content: "";
      position: absolute;
      top: 34px;
      left: 0;
      right: 0;
      height: 1px;
      background: var(--line);
    }
    .method-step {
      position: relative;
      z-index: 1;
      padding-top: 0;
    }
    .method-step .num {
      width: 68px;
      height: 68px;
      border-radius: 50%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0;
      background: var(--bone);
      border: 1px solid var(--line);
      margin-bottom: var(--space-6);
      color: var(--coral);
      font-family: var(--display);
      font-size: 18px;
      font-weight: 900;
      line-height: 1;
      letter-spacing: .02em;
      text-align: center;
    }
    .method-step h4 { font-size: 26px; }

    .work {
      width: min(1480px, calc(100% - 72px));
      margin: 0 auto;
      padding: clamp(48px, 6vw, 78px);
      border-radius: 28px;
      background: var(--ink);
      color: var(--accent-on);
      overflow: hidden;
    }
    .work .work-rule {
      border-top: 1px solid color-mix(in oklab, var(--accent-on), transparent 82%);
      padding-top: 15px;
      margin-bottom: var(--space-12);
      display: flex;
      justify-content: space-between;
      gap: var(--space-5);
      color: color-mix(in oklab, var(--accent-on), transparent 48%);
      font: 500 10.5px/1.3 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
    }
    .work-copy h2 {
      color: var(--accent-on);
      max-width: 9ch;
      line-height: var(--leading-tight);
      letter-spacing: var(--tracking-display);
      font-family: var(--display);
      font-weight: 900;
    }
    .work-link {
      display: inline-flex;
      margin-top: var(--space-8);
      color: var(--coral-soft);
      font: 800 12px/1 var(--display);
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .label-row, .meta-row {
      display: flex;
      justify-content: space-between;
      gap: var(--space-4);
      color: color-mix(in oklab, var(--accent-on), transparent 48%);
      font: 500 10px/1 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
    }

    #evidence {
      background:
        linear-gradient(180deg, var(--paper) 0%, color-mix(in oklab, var(--paper-warm), var(--paper) 64%) 100%);
    }
    #evidence .work {
      background: transparent;
      color: var(--ink);
      border-radius: 0;
      padding: 0;
      overflow: visible;
    }
    #evidence .work-rule {
      border-top-color: var(--line);
      color: var(--ink-faint);
    }
    #evidence .evidence-layout {
      display: grid;
      grid-template-columns: minmax(280px, .56fr) minmax(0, 1.44fr);
      gap: clamp(48px, 6vw, 96px);
      align-items: start;
    }
    #evidence .work-copy h2 {
      color: var(--ink);
      max-width: 11ch;
    }
    #evidence .evidence-note {
      margin-top: var(--space-6);
      max-width: 34ch;
      color: var(--ink-soft);
      font-size: 19px;
    }
    #evidence .work-link { color: var(--coral); }
    #evidence .evidence-paths {
      border-top: 1px solid var(--line);
    }
    #evidence .evidence-path {
      display: grid;
      grid-template-columns: 72px minmax(0, 1fr) minmax(170px, .36fr);
      gap: clamp(24px, 3.6vw, 56px);
      align-items: center;
      padding: clamp(30px, 4vw, 48px) 0;
      border-bottom: 1px solid var(--line);
    }
    #evidence .evidence-num {
      align-self: start;
      color: var(--coral);
      font: 500 12px/1 var(--mono);
      letter-spacing: .16em;
    }
    #evidence .label-row,
    #evidence .meta-row {
      color: var(--ink-faint);
    }
    #evidence .evidence-path h3 {
      margin: var(--space-3) 0 var(--space-4);
      color: var(--ink);
      font-family: var(--display);
      font-size: clamp(34px, 3.5vw, 56px);
      line-height: .98;
      letter-spacing: var(--tracking-display);
    }
    #evidence .evidence-path p {
      max-width: 58ch;
      color: var(--ink-soft);
      font-size: clamp(18px, 1.5vw, 22px);
    }
    #evidence .evidence-flow {
      list-style: none;
      margin: var(--space-5) 0 0;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 8px 0;
      color: var(--ink);
      font: 500 12px/1.35 var(--mono);
      letter-spacing: .03em;
      text-transform: uppercase;
    }
    #evidence .evidence-flow li:not(:last-child)::after {
      content: "/";
      margin: 0 12px;
      color: var(--ink-faint);
    }
    #evidence .evidence-asset {
      margin: 0;
      justify-self: end;
      width: min(220px, 100%);
    }
    #evidence .evidence-asset img {
      display: block;
      width: 100%;
      height: auto;
      mix-blend-mode: multiply;
      filter: saturate(.96) contrast(.98);
    }

    .testimonial-grid {
      display: grid;
      grid-template-columns: 1.05fr .95fr;
      gap: clamp(44px, 6vw, 84px);
      align-items: center;
    }
    .testimonial h2 {
      margin: 0;
      font-family: var(--display);
      font-weight: 900;
      letter-spacing: var(--tracking-display);
      line-height: 1.08;
      text-wrap: balance;
    }
    .author {
      margin-top: var(--space-8);
      display: flex;
      align-items: center;
      gap: var(--space-4);
    }
    .avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      border: 1px solid var(--line);
      font-family: var(--serif);
      font-size: 20px;
      font-weight: 900;
    }
    .author p { margin: 0; font-weight: 700; }
    .author span { color: var(--ink-faint); font: 500 12px/1.3 var(--mono); letter-spacing: .04em; text-transform: uppercase; }
    .evidence-list {
      margin-top: var(--space-8);
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--space-3);
    }
    .evidence-item {
      border-top: 1px solid var(--line);
      padding-top: var(--space-4);
      color: var(--ink-mute);
      font-size: 14px;
    }
    .evidence-item b {
      display: block;
      color: var(--ink);
      font-family: var(--mono);
      font-size: 12px;
      letter-spacing: .04em;
      text-transform: uppercase;
      margin-bottom: var(--space-2);
    }

    .teacher-grid {
      display: grid;
      grid-template-columns: .92fr 1.08fr;
      gap: clamp(40px, 5vw, 78px);
      align-items: center;
    }
    .teacher-panel {
      padding: var(--space-2) 0 0;
    }
    .panel-head {
      display: flex;
      justify-content: space-between;
      gap: var(--space-4);
      align-items: center;
      padding-bottom: var(--space-5);
      border-bottom: 1px solid var(--line);
    }
    .panel-head h3 {
      margin: 0;
      font: 900 28px/1.1 var(--display);
      letter-spacing: var(--tracking-display);
    }
    .table {
      margin-top: 0;
      display: grid;
    }
    .row {
      display: grid;
      grid-template-columns: 1.2fr .7fr .8fr auto;
      align-items: center;
      gap: var(--space-4);
      min-height: 64px;
      padding: var(--space-4) 0;
      border-bottom: 1px solid var(--line-soft);
      color: var(--ink-mute);
      font-size: 14px;
    }
    .row b { color: var(--ink); }
    .severity {
      height: 8px;
      border-radius: var(--radius-pill);
      background: var(--line-soft);
      overflow: hidden;
    }
    .severity span {
      display: block;
      height: 100%;
      background: var(--coral);
    }
    .row-action {
      border: 0;
      border-bottom: 1px solid currentColor;
      border-radius: 0;
      background: transparent;
      min-height: 32px;
      padding: 0 0 3px;
      cursor: pointer;
      color: var(--ink);
      font: 800 11px/1 var(--display);
      letter-spacing: .06em;
      text-transform: uppercase;
    }
    .row-action:hover { color: var(--coral); }
    .teacher-plate {
      aspect-ratio: 1 / 1;
      border-radius: 18px;
    }

    .cta-grid {
      display: grid;
      grid-template-columns: 1.02fr .98fr;
      gap: clamp(44px, 6vw, 88px);
      align-items: center;
    }
    .cta-actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-3);
      margin-top: var(--space-8);
    }
    .cta-foot {
      margin-top: var(--space-8);
      display: flex;
      gap: var(--space-5);
      flex-wrap: wrap;
      color: var(--ink-faint);
      font: 500 11px/1 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
    }

    footer {
      padding: 84px 0 40px;
      border-top: 1px solid var(--line);
    }
    .foot-grid {
      display: grid;
      grid-template-columns: 1.6fr repeat(4, 1fr);
      gap: var(--space-8);
    }
    .foot-brand p {
      margin: var(--space-5) 0 0;
      max-width: 42ch;
      color: var(--ink-mute);
    }
    .foot-col h5 {
      margin: 0 0 var(--space-4);
      color: var(--ink);
      font: 900 13px/1 var(--display);
      letter-spacing: .14em;
      text-transform: uppercase;
    }
    .foot-col ul { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--space-2); }
    .foot-col a {
      color: var(--ink-mute);
      font-size: 14px;
    }
    .foot-bottom {
      margin-top: var(--space-12);
      padding-top: var(--space-5);
      border-top: 1px solid var(--line);
      display: flex;
      justify-content: space-between;
      gap: var(--space-5);
      color: var(--ink-faint);
      font: 500 11px/1.4 var(--mono);
      letter-spacing: .04em;
      text-transform: uppercase;
    }
    .mega {
      margin-top: var(--space-8);
      font: 900 clamp(72px, 13vw, 200px)/.85 var(--display);
      letter-spacing: -.055em;
      text-wrap: balance;
    }

    [data-reveal] {
      opacity: 0;
      transform: translate3d(0, 18px, 0);
      transition: opacity 520ms cubic-bezier(0.23, 1, 0.32, 1), transform 520ms cubic-bezier(0.23, 1, 0.32, 1);
      transition-delay: var(--reveal-delay, 0ms);
      will-change: opacity, transform;
    }
    [data-reveal="left"] { transform: translate3d(-22px, 0, 0); }
    [data-reveal="right"] { transform: translate3d(22px, 0, 0); }
    [data-reveal="scale"] { transform: scale(.985); }
    [data-reveal][data-revealed="true"] {
      opacity: 1;
      transform: none;
      will-change: auto;
    }
    .lab[data-revealed="true"] {
      transition: opacity 220ms var(--ease-standard), transform 220ms var(--ease-standard);
    }
    .lab.is-filtering {
      opacity: 0;
      transform: translate3d(0, 8px, 0);
      pointer-events: none;
    }
    .agent-ledger > .agent-entry:nth-child(2), .labs-grid > .lab:nth-child(2), .method-grid > .method-step:nth-child(2) { --reveal-delay: 60ms; }
    .agent-ledger > .agent-entry:nth-child(3), .labs-grid > .lab:nth-child(3), .method-grid > .method-step:nth-child(3) { --reveal-delay: 120ms; }
    .agent-ledger > .agent-entry:nth-child(4), .labs-grid > .lab:nth-child(4), .method-grid > .method-step:nth-child(4) { --reveal-delay: 180ms; }
    .labs-grid > .lab:nth-child(5) { --reveal-delay: 240ms; }

    @media (prefers-reduced-motion: reduce) {
      .od-landing { scroll-behavior: auto; }
      *, *::before, *::after {
        animation: none !important;
        transition: none !important;
      }
      .nav.is-hidden { transform: none !important; }
      [data-reveal] {
        opacity: 1 !important;
        transform: none !important;
        will-change: auto !important;
      }
    }

    @media (max-width: 1280px) {
      .side-rail { display: none; }
      .container { width: min(var(--container-max), calc(100% - var(--container-gutter-tablet) * 2)); }
      .topbar .mid { display: none; }
      .labs-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .work-copy { grid-column: 1 / -1; }
      .work-copy h2 { max-width: 16ch; }
      .foot-grid { grid-template-columns: 1.4fr repeat(2, 1fr); }
    }

    @media (max-width: 960px) {
      .nav-inner { min-height: 72px; }
      .nav-links {
        position: fixed;
        top: 72px;
        left: var(--container-gutter-tablet);
        right: var(--container-gutter-tablet);
        display: none;
        padding: var(--space-4);
        border: 1px solid var(--line);
        border-radius: 18px;
        background: var(--bone);
        box-shadow: var(--shadow-paper);
      }
      .nav-links.open { display: grid; }
      .nav-links a { width: 100%; justify-content: space-between; }
      .nav-side .ghost, .status-dot { display: none; }
      .mobile-toggle {
        display: inline-flex;
        width: 44px;
        height: 44px;
        align-items: center;
        justify-content: center;
        border: 1px solid var(--line);
        border-radius: 50%;
        background: transparent;
        color: var(--ink);
      }
      .hero-grid, .about-grid, .capabilities-grid, .method-head, .testimonial-grid, .teacher-grid, .cta-grid {
        grid-template-columns: 1fr;
      }
      .hero-art { min-height: 420px; height: min(620px, 72vh); }
      .agent-entry {
        grid-template-columns: 68px minmax(120px, .44fr) minmax(0, 1fr);
      }
      .agent-tag {
        grid-column: 2 / -1;
        justify-self: start;
        text-align: left;
      }
      .agent-tag::before { margin-left: 0; margin-right: auto; }
      .about-proof, .evidence-list { grid-template-columns: 1fr; }
      .method-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .method-grid::before { display: none; }
      .labs-head { grid-template-columns: 1fr; align-items: start; }
      .pills { justify-content: flex-start; }
      .work { width: min(100% - 32px, 1480px); padding: 40px 24px; border-radius: 22px; }
      #evidence .work { padding: 0; border-radius: 0; }
      #evidence .evidence-layout {
        grid-template-columns: 1fr;
        gap: var(--space-10);
      }
      #evidence .work-copy h2 { max-width: 12ch; }
      .foot-bottom { flex-direction: column; }
    }

    @media (max-width: 640px) {
      .container { width: calc(100% - var(--container-gutter-phone) * 2); }
      section { padding: 84px 0; }
      .topbar .right span:last-child { display: none; }
      .brand-meta, .nav-side { display: none; }
      h1.display { font-size: clamp(48px, 18vw, 76px); }
      h2.display, .work-copy h2, .testimonial h2 { font-size: clamp(34px, 10vw, 48px); }
      .sec-rule, .work .work-rule { align-items: flex-start; flex-direction: column; gap: var(--space-2); }
      .meta-grp { justify-content: flex-start; flex-wrap: wrap; text-align: left; }
      .hero-stats, .method-grid, .labs-grid, .foot-grid { grid-template-columns: 1fr; }
      #evidence .evidence-path {
        grid-template-columns: 1fr;
        gap: var(--space-5);
      }
      #evidence .evidence-asset {
        justify-self: start;
        width: min(210px, 72vw);
      }
      .hero-art { min-height: 360px; }
      .annot-tr, .annot-br, .side-note { display: none; }
      .wire-inner { grid-template-columns: 1fr; }
      .wire-left { border-right: 0; border-bottom: 1px solid var(--line); min-height: 74px; }
      .wire-rows { padding-left: 0; }
      .agent-entry {
        grid-template-columns: 56px 1fr;
        gap: 12px 18px;
        padding: 22px 0;
      }
      .agent-entry::before { left: 27px; }
      .agent-no {
        width: 56px;
        font-size: 34px;
      }
      .agent-desc, .agent-tag { grid-column: 2; }
      .row { grid-template-columns: 1fr; padding: var(--space-4) 0; }
      .mega { font-size: clamp(56px, 20vw, 92px); }
    }

.od-landing {
  position: relative;
  min-height: 100vh;
  isolation: isolate;
}

.od-landing .skip-link,
.od-landing .nav a,
.od-landing .btn,
.od-landing .pill,
.od-landing .row-action,
.od-landing .mobile-toggle {
  cursor: pointer;
}
</style>
