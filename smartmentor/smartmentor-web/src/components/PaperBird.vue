<template>
  <!-- 共享视觉母题：折纸鸟，呼应首页插画。线描极简，适合内页空状态/装饰 -->
  <svg
    class="paper-bird"
    :class="{ 'paper-bird--float': float }"
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    fill="none"
    aria-hidden="true"
  >
    <!-- 机身：折纸三角 -->
    <path
      d="M6 30 L58 14 L34 40 Z"
      :stroke="color"
      stroke-width="2"
      stroke-linejoin="round"
      fill="var(--accent-light)"
    />
    <!-- 折痕：从尖端到机身的中线 -->
    <path
      d="M58 14 L30 30 L34 40"
      :stroke="color"
      stroke-width="1.6"
      stroke-linejoin="round"
      stroke-linecap="round"
      opacity="0.6"
    />
    <!-- 下翼 -->
    <path
      d="M6 30 L30 30 L34 40 Z"
      :stroke="color"
      stroke-width="1.6"
      stroke-linejoin="round"
      fill="none"
      opacity="0.7"
    />
  </svg>
</template>

<script setup>
defineProps({
  size: { type: [Number, String], default: 48 },
  color: { type: String, default: 'var(--accent)' },
  float: { type: Boolean, default: true }
})
</script>

<style scoped>
.paper-bird {
  display: inline-block;
  overflow: visible;
}
.paper-bird--float {
  animation: birdGlide 5s ease-in-out infinite;
}
@keyframes birdGlide {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-8px) rotate(-3deg); }
}
@media (prefers-reduced-motion: reduce) {
  .paper-bird--float { animation: none; }
}
</style>
