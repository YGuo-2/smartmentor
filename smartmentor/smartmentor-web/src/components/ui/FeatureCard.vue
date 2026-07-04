<template>
  <article class="feature-card" :class="{ 'feature-card--interactive': interactive }">
    <div v-if="$slots.icon" class="feature-card__icon">
      <slot name="icon" />
    </div>
    <div class="feature-card__body">
      <slot />
    </div>
  </article>
</template>

<script setup>
defineProps({
  interactive: { type: Boolean, default: false }
})
</script>
