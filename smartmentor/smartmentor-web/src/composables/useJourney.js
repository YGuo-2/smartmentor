import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

/**
 * 学习旅程主线步骤定义与当前步推断。
 * 被 JourneyProgress 组件与 App.vue（控制内容上边距）共用，避免逻辑重复。
 * ①画像 → ②诊断 → ③路径 → ④学习 → ⑤报告
 */
export const JOURNEY_STEPS = [
  { key: 'profile',    label: '画像', to: '/onboarding' },
  { key: 'diagnostic', label: '诊断', to: '/diagnostic' },
  { key: 'path',       label: '路径', to: '/learning' },
  { key: 'lesson',     label: '学习', to: '/learning' },
  { key: 'report',     label: '报告', to: '/report' }
]

/** 由路由路径推断处于旅程第几步；非主线返回 -1 */
export function journeyIndexOf(path) {
  if (path.startsWith('/onboarding')) return 0
  if (path.startsWith('/diagnostic') || path.startsWith('/tracing')) return 1
  if (path.startsWith('/learning')) {
    // /learning/:pathId/:nodeId 为课时（第4步），其余为路径列表/详情（第3步）
    const segs = path.split('/').filter(Boolean)
    return segs.length >= 3 ? 3 : 2
  }
  if (path.startsWith('/report')) return 4
  return -1
}

export function useJourney() {
  const route = useRoute()
  const currentIndex = computed(() => journeyIndexOf(route.path))
  const onJourney = computed(() => currentIndex.value >= 0)
  return { steps: JOURNEY_STEPS, currentIndex, onJourney }
}

// 旅程进度条收起状态（全局共享，记住用户偏好）
const journeyBarCollapsed = ref(localStorage.getItem('journeyBarCollapsed') === '1')

export function useJourneyBar() {
  function toggleJourneyBar() {
    journeyBarCollapsed.value = !journeyBarCollapsed.value
    localStorage.setItem('journeyBarCollapsed', journeyBarCollapsed.value ? '1' : '0')
  }
  return { journeyBarCollapsed, toggleJourneyBar }
}
