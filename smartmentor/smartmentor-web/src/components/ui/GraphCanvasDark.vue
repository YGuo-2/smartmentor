<template>
  <section class="graph-canvas-dark">
    <div v-if="title || $slots.meta" class="graph-canvas-dark__head">
      <h3 v-if="title">{{ title }}</h3>
      <div v-if="$slots.meta" class="graph-canvas-dark__meta">
        <slot name="meta" />
      </div>
    </div>
    <div class="graph-canvas-dark__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' }
})
</script>
