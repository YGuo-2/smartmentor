<template>
  <div ref="container" class="card-swap-container" :style="containerStyle">
    <slot />
  </div>
</template>

<script setup>
/*
  CardSwap —— 3D 卡片堆叠轮播（移植自 React Bits 的 CardSwap，Vue 3 + GSAP 版）。
  实现要点：不走 React 的 cloneElement/ref 转发，改为 onMounted 时用
  container.querySelectorAll('.card') 直接拿子卡片 DOM（文档顺序 == 插槽顺序），
  对其做 GSAP 时间线动画。子卡片请用配套的 <Card> 组件（带 .card class）。
  尊重 prefers-reduced-motion：静态堆叠、不轮播。
*/
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { gsap, prefersReducedMotion } from '../lib/gsap.js'

const props = defineProps({
  width: { type: [Number, String], default: 500 },
  height: { type: [Number, String], default: 400 },
  cardDistance: { type: Number, default: 60 },
  verticalDistance: { type: Number, default: 70 },
  delay: { type: Number, default: 5000 },
  pauseOnHover: { type: Boolean, default: false },
  skewAmount: { type: Number, default: 6 },
  easing: { type: String, default: 'elastic' } // 'elastic' | 'linear'
})
const emit = defineEmits(['cardClick'])

const container = ref(null)
let tl = null
let intervalId = null
let order = []
let cards = []
let cleanupFns = []

const toCssSize = (v) => (typeof v === 'number' ? `${v}px` : v)
const containerStyle = computed(() => ({
  width: toCssSize(props.width),
  height: toCssSize(props.height)
}))

const config = computed(() =>
  props.easing === 'elastic'
    ? { ease: 'elastic.out(0.6,0.9)', durDrop: 2, durMove: 2, durReturn: 2, promoteOverlap: 0.9, returnDelay: 0.05 }
    : { ease: 'power1.inOut', durDrop: 0.8, durMove: 0.8, durReturn: 0.8, promoteOverlap: 0.45, returnDelay: 0.2 }
)

const makeSlot = (i, distX, distY, total) => ({
  x: i * distX,
  y: -i * distY,
  z: -i * distX * 1.5,
  zIndex: total - i
})

const placeNow = (el, slot, skew) =>
  gsap.set(el, {
    x: slot.x,
    y: slot.y,
    z: slot.z,
    xPercent: -50,
    yPercent: -50,
    skewY: skew,
    transformOrigin: 'center center',
    zIndex: slot.zIndex,
    force3D: true
  })

function swap() {
  if (order.length < 2) return
  const cfg = config.value
  const [front, ...rest] = order
  const elFront = cards[front]
  tl = gsap.timeline()

  tl.to(elFront, { y: '+=500', duration: cfg.durDrop, ease: cfg.ease })

  tl.addLabel('promote', `-=${cfg.durDrop * cfg.promoteOverlap}`)
  rest.forEach((idx, i) => {
    const el = cards[idx]
    const slot = makeSlot(i, props.cardDistance, props.verticalDistance, cards.length)
    tl.set(el, { zIndex: slot.zIndex }, 'promote')
    tl.to(el, { x: slot.x, y: slot.y, z: slot.z, duration: cfg.durMove, ease: cfg.ease }, `promote+=${i * 0.15}`)
  })

  const backSlot = makeSlot(cards.length - 1, props.cardDistance, props.verticalDistance, cards.length)
  tl.addLabel('return', `promote+=${cfg.durMove * cfg.returnDelay}`)
  tl.call(() => { gsap.set(elFront, { zIndex: backSlot.zIndex }) }, undefined, 'return')
  tl.to(elFront, { x: backSlot.x, y: backSlot.y, z: backSlot.z, duration: cfg.durReturn, ease: cfg.ease }, 'return')

  tl.call(() => { order = [...rest, front] })
}

onMounted(() => {
  if (!container.value) return
  cards = Array.from(container.value.querySelectorAll('.card'))
  const total = cards.length
  order = Array.from({ length: total }, (_, i) => i)

  // 统一卡片尺寸（React 原版通过 cloneElement 注入 style，这里直接写 DOM）
  const w = toCssSize(props.width)
  const h = toCssSize(props.height)
  cards.forEach((el) => {
    el.style.width = w
    el.style.height = h
  })

  // 初始堆叠
  cards.forEach((el, i) => placeNow(el, makeSlot(i, props.cardDistance, props.verticalDistance, total), props.skewAmount))

  // 点击回调
  cards.forEach((el, i) => {
    const handler = () => emit('cardClick', i)
    el.addEventListener('click', handler)
    cleanupFns.push(() => el.removeEventListener('click', handler))
  })

  // 低动效偏好：静态堆叠，不轮播
  if (prefersReducedMotion() || total < 2) return

  swap()
  intervalId = window.setInterval(swap, props.delay)

  if (props.pauseOnHover) {
    const node = container.value
    const pause = () => { tl?.pause(); clearInterval(intervalId) }
    const resume = () => { tl?.play(); intervalId = window.setInterval(swap, props.delay) }
    node.addEventListener('mouseenter', pause)
    node.addEventListener('mouseleave', resume)
    cleanupFns.push(() => {
      node.removeEventListener('mouseenter', pause)
      node.removeEventListener('mouseleave', resume)
    })
  }
})

onUnmounted(() => {
  clearInterval(intervalId)
  tl?.kill()
  cleanupFns.forEach((fn) => fn())
  cleanupFns = []
})
</script>

<style scoped>
.card-swap-container {
  position: absolute;
  bottom: 0;
  right: 0;
  transform: translate(5%, 20%);
  transform-origin: bottom right;
  perspective: 900px;
  overflow: visible;
}

@media (max-width: 768px) {
  .card-swap-container { transform: scale(0.75) translate(25%, 25%); }
}
@media (max-width: 480px) {
  .card-swap-container { transform: scale(0.55) translate(25%, 25%); }
}
</style>
