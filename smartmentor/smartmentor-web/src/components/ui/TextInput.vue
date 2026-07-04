<template>
  <textarea
    v-if="multiline"
    class="text-input text-input--multiline"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    @input="$emit('update:modelValue', $event.target.value)"
  ></textarea>
  <input
    v-else
    class="text-input"
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
defineEmits(['update:modelValue'])
defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  multiline: { type: Boolean, default: false }
})
</script>
