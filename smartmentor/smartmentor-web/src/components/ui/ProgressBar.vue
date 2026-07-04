<template>
  <progress class="ui-progress-bar" :max="max" :value="safeValue" :aria-label="label"></progress>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  label: { type: String, default: '进度' }
})

const safeValue = computed(() => Math.min(props.max, Math.max(0, props.value || 0)))
</script>
