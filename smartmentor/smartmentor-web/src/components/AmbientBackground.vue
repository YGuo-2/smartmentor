<template>
  <!--
    全局氛围背景：与首页插画/Dashboard 呼应的多层叠加。
    - 第一层：静态柔光球（暖金 + 淡蓝）
    - 第二层：极淡金色点阵底纹（消除大白底单调）
    - 第三层：晨光顶栏（顶部暖金渐隐，呼应插画晨光）
    - 第四层：留空，避免持续扫光和飞行动效干扰阅读

    用法：
    - 全局：在 App.vue 内首行放 <AmbientBackground global />，固定铺满视口、z-index:-1。
    - 局部：在页面根容器内首行放 <AmbientBackground />，绝对定位铺底（容器需 position:relative）。
  -->
  <div class="ambient-background" :class="{ 'ambient-background--global': global }" aria-hidden="true">
    <!-- 第一层：柔光球 -->
    <div class="amb-orb amb-orb--gold"></div>
    <div class="amb-orb amb-orb--blue"></div>
    <!-- 第二层：极淡点阵底纹 -->
    <div class="amb-dots"></div>
    <!-- 第三层：晨光顶栏 -->
    <div class="amb-dawn"></div>
  </div>
</template>

<script setup>
defineProps({
  // 是否作为全局背景（固定定位、铺满视口、置于内容之下）
  global: { type: Boolean, default: false }
})
</script>

<style scoped>
.ambient-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}
/* 全局模式：固定铺满视口，沉到内容之下 */
.ambient-background--global {
  position: fixed;
  z-index: -1;
}

/* ===== 第一层：静态柔光球 ===== */
.amb-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.28;
}
.amb-orb--gold {
  width: 50vw;
  height: 50vw;
  max-width: 720px;
  max-height: 720px;
  background: radial-gradient(circle, var(--accent-light) 0%, transparent 60%);
  top: -12%;
  right: -6%;
}
.amb-orb--blue {
  width: 40vw;
  height: 40vw;
  max-width: 560px;
  max-height: 560px;
  background: radial-gradient(circle, var(--info-light) 0%, transparent 60%);
  bottom: -6%;
  left: -8%;
}

/* ===== 第二层：极淡点阵底纹 ===== */
.amb-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(
    rgba(197, 160, 89, 0.16) 1px,
    transparent 1.4px
  );
  background-size: 22px 22px;
  -webkit-mask-image: radial-gradient(ellipse 80% 70% at 50% 40%, rgba(0,0,0,0.5), transparent 95%);
  mask-image: radial-gradient(ellipse 80% 70% at 50% 40%, rgba(0,0,0,0.5), transparent 95%);
  opacity: 0.5;
}

/* ===== 第三层：晨光顶栏 ===== */
.amb-dawn {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 320px;
  background: linear-gradient(
    180deg,
    rgba(197, 160, 89, 0.18) 0%,
    rgba(197, 160, 89, 0.07) 45%,
    transparent 100%
  );
}

/* 低动效偏好：背景保持完全静态 */
@media (prefers-reduced-motion: reduce) {
  .amb-orb { opacity: 0.22; }
}
</style>
