<template>
  <span class="badge-pill" :class="`badge-pill--${tone}`">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  tone: { type: String, default: 'neutral' }
})
</script>
