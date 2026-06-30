<template>
  <div id="application" :class="{ 'has-journey': showNav && onJourney && !journeyBarCollapsed }">
    <AmbientBackground v-if="showNav" global />
    <AppNav v-if="showNav" />
    <JourneyProgress v-if="showNav" />
    <router-view v-slot="{ Component, route }">
      <Transition mode="out-in" :css="false" @before-enter="beforeEnter" @enter="enter" @leave="leave">
        <component :is="Component" :key="route.fullPath"></component>
      </Transition>
    </router-view>
    <div class="toast-container">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppNav from './components/AppNav.vue'
import AmbientBackground from './components/AmbientBackground.vue'
import JourneyProgress from './components/JourneyProgress.vue'
import { toasts } from './composables/state.js'
import { useJourney, useJourneyBar } from './composables/useJourney.js'
import { gsap, prefersReducedMotion } from './lib/gsap.js'

const route = useRoute()
const showNav = computed(() => !route.meta.public)
const { onJourney } = useJourney()
const { journeyBarCollapsed } = useJourneyBar()

function beforeEnter(el) {
  if (prefersReducedMotion()) return
  gsap.set(el, { autoAlpha: 0, y: 12 })
}

function enter(el, done) {
  if (prefersReducedMotion()) {
    done()
    return
  }
  gsap.to(el, { autoAlpha: 1, y: 0, duration: 0.34, clearProps: 'transform,visibility', onComplete: done })
}

function leave(el, done) {
  if (prefersReducedMotion()) {
    done()
    return
  }
  gsap.to(el, { autoAlpha: 0, y: -8, duration: 0.18, onComplete: done })
}
</script>
