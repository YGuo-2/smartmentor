<template>
  <!--
    旅程进度指示器：贯穿学习闭环主线的统一步骤条。
    ①画像 → ②诊断 → ③路径 → ④学习 → ⑤报告
    - 当前步高亮，已过步骤标记完成，未来步灰显
    - 每步可点击跳转到该阶段入口
    - 当前步由路由自动推断，无需父组件传参
    放在主线各页顶部即可：<JourneyProgress />
  -->
  <template v-if="currentIndex >= 0">
    <nav class="journey" :class="{ 'is-collapsed': journeyBarCollapsed }" aria-label="学习旅程进度">
      <ol class="journey-track">
        <li
          v-for="(step, i) in steps"
          :key="step.key"
          class="journey-step"
          :class="{
            'is-current': i === currentIndex,
            'is-done': i < currentIndex,
            'is-future': i > currentIndex
          }"
        >
          <button class="journey-node" @click="go(step)" :title="step.label">
            <span class="journey-num">
              <i v-if="i < currentIndex" class="ri-check-line"></i>
              <template v-else>{{ i + 1 }}</template>
            </span>
            <span class="journey-label">{{ step.label }}</span>
          </button>
          <span v-if="i < steps.length - 1" class="journey-link" aria-hidden="true"></span>
        </li>
      </ol>
      <button
        class="journey-collapse-btn"
        @click="toggleJourneyBar"
        :title="journeyBarCollapsed ? '展开学习旅程' : '收起学习旅程'"
        :aria-label="journeyBarCollapsed ? '展开学习旅程' : '收起学习旅程'"
      >
        <i class="ri-arrow-up-s-line"></i>
      </button>
    </nav>
    <!-- 收起后的浮出小把手：点击重新展开 -->
    <button
      v-if="journeyBarCollapsed"
      class="journey-reopen"
      @click="toggleJourneyBar"
      title="展开学习旅程"
      aria-label="展开学习旅程"
    >
      <i class="ri-route-line"></i>
      <span>第 {{ currentIndex + 1 }} 步 · {{ steps[currentIndex]?.label }}</span>
      <i class="ri-arrow-down-s-line"></i>
    </button>
  </template>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useJourney, useJourneyBar } from '../composables/useJourney.js'

const router = useRouter()
const { steps, currentIndex } = useJourney()
const { journeyBarCollapsed, toggleJourneyBar } = useJourneyBar()

function go(step) {
  if (router.currentRoute.value.path !== step.to) router.push(step.to)
}
</script>

<style scoped>
.journey {
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  z-index: 90;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 9px 0;
  background: rgba(253, 252, 249, 0.92);
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
  transition: transform 0.32s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.28s ease;
}
/* 收起：整条向上滑出并淡出，不占视觉空间 */
.journey.is-collapsed {
  transform: translateY(-100%);
  opacity: 0;
  pointer-events: none;
}
.journey-track {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
}
.journey-step {
  display: flex;
  align-items: center;
}
.journey-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 4px 6px;
  white-space: nowrap;
}
.journey-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-size: 0.78rem;
  font-weight: 700;
  border: 1.5px solid var(--border);
  background: var(--bg);
  color: var(--text-muted);
  transition: all var(--transition);
}
.journey-label {
  font-size: 0.85rem;
  color: var(--text-muted);
  transition: color var(--transition);
}
/* 连接线 */
.journey-link {
  width: 36px;
  height: 2px;
  margin: 0 4px;
  background: var(--border);
  border-radius: 2px;
  transition: background var(--transition);
}

/* 已完成 */
.journey-step.is-done .journey-num {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}
.journey-step.is-done .journey-label { color: var(--text-secondary); }
.journey-step.is-done .journey-link { background: var(--accent); }

/* 当前步 */
.journey-step.is-current .journey-num {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
  box-shadow: 0 0 0 4px var(--accent-light);
}
.journey-step.is-current .journey-label {
  color: var(--primary);
  font-weight: 700;
}

/* 未来步 hover 提示可点 */
.journey-node:hover .journey-label { color: var(--text); }
.journey-node:hover .journey-num { border-color: var(--text-muted); }

@media (max-width: 768px) {
  .journey-label { display: none; }
  .journey-link { width: 20px; }
}

/* 收起按钮：贴在旅程条右侧 */
.journey-collapse-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text-muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  transition: all var(--transition);
}
.journey-collapse-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* 收起后的浮出小把手 */
.journey-reopen {
  position: fixed;
  top: 64px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 90;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  border: 1px solid var(--border);
  border-top: none;
  border-radius: 0 0 14px 14px;
  background: rgba(253, 252, 249, 0.96);
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
  animation: journeyHandleDrop 0.32s cubic-bezier(0.16, 1, 0.3, 1);
}
.journey-reopen:hover { color: var(--accent); border-color: var(--accent); }
.journey-reopen i:first-child { color: var(--accent); font-size: 0.9rem; }
.journey-reopen .ri-arrow-down-s-line { font-size: 1rem; opacity: 0.7; }
@keyframes journeyHandleDrop {
  from { transform: translate(-50%, -100%); opacity: 0; }
  to { transform: translate(-50%, 0); opacity: 1; }
}

</style>
