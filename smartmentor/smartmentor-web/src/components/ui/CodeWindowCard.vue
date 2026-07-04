<template>
  <section class="code-window-card">
    <div v-if="title" class="code-window-card__bar">
      <span class="code-window-card__dot"></span>
      <span class="code-window-card__title">{{ title }}</span>
    </div>
    <pre class="code-window-card__body"><code><slot /></code></pre>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' }
})
</script>
