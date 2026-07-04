<template>
  <div class="chat-bubble" :class="role === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai'">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  role: { type: String, default: 'ai' }
})
</script>
