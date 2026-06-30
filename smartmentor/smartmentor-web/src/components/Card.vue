<template>
  <div class="card" :class="customClass">
    <slot />
  </div>
</template>

<script setup>
/* CardSwap 配套卡片。带 .card class 供父组件 querySelectorAll 抓取。 */
defineProps({
  customClass: { type: String, default: '' }
})
</script>

<style scoped>
/* 主题化卡片：深藏青底 + 学者金描边，呼应站点视觉（替代原版纯黑白）。 */
.card {
  position: absolute;
  top: 50%;
  left: 50%;
  border-radius: 16px;
  border: 1px solid rgba(197, 160, 89, 0.35);
  background: linear-gradient(155deg, #16294a 0%, #0e1d38 100%);
  box-shadow: 0 24px 60px -18px rgba(8, 18, 38, 0.7);
  transform-style: preserve-3d;
  will-change: transform;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  cursor: pointer;
}
</style>
