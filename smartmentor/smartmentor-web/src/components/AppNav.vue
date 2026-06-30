<template>
  <nav class="topnav" ref="navRoot">
    <div class="topnav-brand">SmartMentor</div>
    <ul class="topnav-links">
      <!-- 旅程之家 -->
      <li><router-link to="/dashboard" class="nav-home"><i class="ri-home-5-line"></i> 仪表盘</router-link></li>
      <!-- 主线三步：诊断 → 路径 → 报告（始终显示） -->
      <li class="nav-journey-wrap">
        <div class="nav-journey">
          <router-link to="/diagnostic" class="nav-step"><span class="nav-step-num">1</span> 诊断</router-link>
          <i class="ri-arrow-right-s-line nav-arrow"></i>
          <router-link to="/learning" class="nav-step"><span class="nav-step-num">2</span> 路径</router-link>
          <i class="ri-arrow-right-s-line nav-arrow"></i>
          <router-link to="/report" class="nav-step"><span class="nav-step-num">3</span> 报告</router-link>
        </div>
      </li>
      <!-- 工具组（弱化） -->
      <li class="nav-tools">
        <router-link to="/chat" class="nav-tool"><i class="ri-chat-3-line"></i> AI对话</router-link>
      </li>
    </ul>
    <div class="topnav-user">
      <button class="topnav-user-btn" @click="showDropdown = !showDropdown">
        {{ user?.nickname || user?.username || '用户' }}
        <span class="caret">▾</span>
      </button>
      <Transition :css="false" @enter="dropdownEnter" @leave="dropdownLeave">
        <div class="topnav-dropdown" v-if="showDropdown">
          <router-link to="/profile" @click="showDropdown = false">个人设置</router-link>
          <a href="#" @click.prevent="handleLogout">退出登录</a>
        </div>
      </Transition>
    </div>
  </nav>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { user, logout } from '../composables/state.js'
import { gsap, prefersReducedMotion } from '../lib/gsap.js'

const navRoot = ref(null)
const showDropdown = ref(false)

onMounted(() => {
  if (!navRoot.value || prefersReducedMotion()) return
  gsap.from(navRoot.value, {
    autoAlpha: 0,
    y: -12,
    duration: 0.38,
    clearProps: 'transform,visibility'
  })
})

function handleLogout() {
  showDropdown.value = false
  logout()
}

function dropdownEnter(el, done) {
  if (prefersReducedMotion()) {
    done()
    return
  }
  gsap.fromTo(el,
    { autoAlpha: 0, y: -8, scale: 0.98 },
    { autoAlpha: 1, y: 0, scale: 1, duration: 0.22, transformOrigin: 'right top', clearProps: 'transform,visibility', onComplete: done }
  )
}

function dropdownLeave(el, done) {
  if (prefersReducedMotion()) {
    done()
    return
  }
  gsap.to(el, { autoAlpha: 0, y: -6, scale: 0.98, duration: 0.16, transformOrigin: 'right top', onComplete: done })
}
</script>
