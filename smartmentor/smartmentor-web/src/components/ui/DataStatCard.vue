<template>
  <article class="data-stat-card">
    <div class="data-stat-card__value">{{ value }}</div>
    <div class="data-stat-card__label">{{ label }}</div>
    <div v-if="hint" class="data-stat-card__hint">{{ hint }}</div>
  </article>
</template>

<script setup>
defineProps({
  value: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' }
})
</script>
