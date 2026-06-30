<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link class="auth-home-link" to="/">
        <i class="ri-arrow-left-line" aria-hidden="true"></i>
        返回首页
      </router-link>

      <h2 style="text-align:center;margin-bottom:24px;font-size:24px;font-weight:600;">
        {{ mode === 'login' ? '登录 SmartMentor' : '注册 SmartMentor' }}
      </h2>

      <!-- Login Form -->
      <template v-if="mode === 'login'">
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input class="form-input" type="text" v-model="loginUsername" placeholder="请输入用户名" />
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input class="form-input" type="password" v-model="loginPassword" placeholder="请输入密码" />
          </div>

          <div class="form-group" style="margin-top:24px;">
            <button type="submit" class="btn btn-dark btn-full" :disabled="loginLoading">
              {{ loginLoading ? '登录中...' : '登录' }}
            </button>
          </div>
        </form>

        <div class="auth-link">
          还没有账号？<router-link :to="{ path: '/register', query: route.query }">立即注册</router-link>
        </div>
      </template>

      <!-- Register Form -->
      <template v-if="mode === 'register'">
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input class="form-input" type="text" v-model="regUsername" placeholder="请输入用户名" />
          </div>

          <div class="form-group">
            <label class="form-label">邮箱</label>
            <input class="form-input" type="email" v-model="regEmail" placeholder="请输入邮箱" />
          </div>

          <div class="form-group">
            <label class="form-label">邮箱验证码</label>
            <div class="form-row">
              <input class="form-input" type="text" v-model="regCode" placeholder="请输入验证码" maxlength="6" style="flex:1;" />
              <button type="button" class="btn btn-outline" @click="sendRegCaptcha" :disabled="regCountdown > 0" style="margin-left:12px;white-space:nowrap;">
                {{ regCountdown > 0 ? regCountdown + 's' : '发送验证码' }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input class="form-input" type="password" v-model="regPassword" placeholder="请设置密码（至少6位）" />
          </div>

          <div class="form-group">
            <label class="form-label">确认密码</label>
            <input class="form-input" type="password" v-model="regConfirmPassword" placeholder="请再次输入密码" />
          </div>

          <div class="form-group">
            <label class="form-label">昵称</label>
            <input class="form-input" type="text" v-model="regNickname" placeholder="请输入昵称" />
          </div>

          <div class="form-group">
            <label class="form-label">学历层次</label>
            <select class="form-input" v-model="regGrade" aria-label="学历层次" title="学历层次">
              <option value="">请选择学历层次</option>
              <option value="高职">高职</option>
              <option value="本科">本科</option>
              <option value="研究生">研究生</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">学校</label>
            <input class="form-input" type="text" v-model="regSchool" placeholder="请输入学校名称" />
          </div>

          <div class="form-group" style="margin-top:24px;">
            <button type="submit" class="btn btn-dark btn-full" :disabled="regLoading">
              {{ regLoading ? '注册中...' : '注册' }}
            </button>
          </div>
        </form>

        <div class="auth-link">
          已有账号？<router-link :to="{ path: '/login', query: route.query }">立即登录</router-link>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api, setToken } from '../api/index.js'
import { getUserRole, loadUser, roleHome, showToast } from '../composables/state.js'

const props = defineProps({
  mode: { type: String, default: 'login' }
})

const router = useRouter()
const route = useRoute()

// 登录/注册成功后的落点：优先消费 ?redirect=（携带用户进入前的意图，
// 如首页“开始诊断”或被守卫拦截的原目标），否则回落到角色主页。
function resolveLanding(currentUser, fallbackRole) {
  const redirect = route.query.redirect
  if (redirect && typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }
  return roleHome(getUserRole(currentUser) || fallbackRole)
}

const loginUsername = ref('')
const loginPassword = ref('')
const loginLoading = ref(false)

const regUsername = ref('')
const regEmail = ref('')
const regCode = ref('')
const regPassword = ref('')
const regConfirmPassword = ref('')
const regNickname = ref('')
const regGrade = ref('')
const regSchool = ref('')
const regCountdown = ref(0)
const regLoading = ref(false)

function startCountdown(counterRef) {
  counterRef.value = 60
  const timer = setInterval(() => {
    counterRef.value--
    if (counterRef.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendRegCaptcha() {
  if (!regEmail.value) { showToast('请输入邮箱', 'error'); return }
  try {
    await api.auth.sendCaptcha({ email: regEmail.value })
    showToast('验证码已发送到邮箱', 'success')
    startCountdown(regCountdown)
  } catch (e) { showToast(e.message || '发送失败', 'error') }
}

async function handleLogin() {
  if (!loginUsername.value) { showToast('请输入用户名', 'error'); return }
  if (!loginPassword.value) { showToast('请输入密码', 'error'); return }

  loginLoading.value = true
  try {
    const data = await api.auth.login({
      username: loginUsername.value,
      password: loginPassword.value,
      role: 'student'
    })
    setToken(data.token)
    const currentUser = await loadUser()
    showToast('登录成功', 'success')
    router.push(resolveLanding(currentUser, 'student'))
  } catch (e) { showToast(e.message || '登录失败', 'error') }
  finally { loginLoading.value = false }
}

async function handleRegister() {
  if (!regUsername.value) { showToast('请输入用户名', 'error'); return }
  if (!regEmail.value) { showToast('请输入邮箱', 'error'); return }
  if (!regCode.value) { showToast('请输入验证码', 'error'); return }
  if (!regPassword.value || regPassword.value.length < 6) { showToast('密码至少6位', 'error'); return }
  if (regPassword.value !== regConfirmPassword.value) { showToast('两次密码输入不一致', 'error'); return }
  if (!regNickname.value) { showToast('请输入昵称', 'error'); return }

  const params = {
    username: regUsername.value,
    email: regEmail.value,
    code: regCode.value,
    password: regPassword.value,
    nickname: regNickname.value,
    role: 'student'
  }
  if (regGrade.value) params.grade = regGrade.value
  if (regSchool.value) params.school = regSchool.value

  regLoading.value = true
  try {
    const data = await api.auth.register(params)
    setToken(data.token)
    const currentUser = await loadUser()
    showToast('注册成功', 'success')
    router.push(resolveLanding(currentUser, 'student'))
  } catch (e) { showToast(e.message || '注册失败', 'error') }
  finally { regLoading.value = false }
}
</script>
