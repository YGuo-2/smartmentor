import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, setToken } from '../api.js'
import { loadUser, showToast } from '../state.js'

export const AuthPage = {
  name: 'AuthPage',
  props: {
    mode: { type: String, default: 'login' }
  },
  setup() {
    const router = useRouter()
    const route = useRoute()

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

    function landingPath() {
      const redirect = route.query.redirect
      return redirect && typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/dashboard'
    }

    function startCountdown(counterRef) {
      counterRef.value = 60
      const timer = setInterval(() => {
        counterRef.value--
        if (counterRef.value <= 0) clearInterval(timer)
      }, 1000)
    }

    async function sendRegCaptcha() {
      if (!regEmail.value) {
        showToast('请输入邮箱', 'error')
        return
      }
      try {
        await api.auth.sendCaptcha({ email: regEmail.value })
        showToast('验证码已发送到邮箱', 'success')
        startCountdown(regCountdown)
      } catch (e) {
        showToast(e.message || '发送失败', 'error')
      }
    }

    async function handleLogin() {
      if (!loginUsername.value) {
        showToast('请输入用户名', 'error')
        return
      }
      if (!loginPassword.value) {
        showToast('请输入密码', 'error')
        return
      }

      loginLoading.value = true
      try {
        const data = await api.auth.login({
          username: loginUsername.value,
          password: loginPassword.value
        })
        setToken(data.token)
        await loadUser()
        showToast('登录成功', 'success')
        router.push(landingPath())
      } catch (e) {
        showToast(e.message || '登录失败', 'error')
      } finally {
        loginLoading.value = false
      }
    }

    async function handleRegister() {
      if (!regUsername.value) {
        showToast('请输入用户名', 'error')
        return
      }
      if (!regEmail.value) {
        showToast('请输入邮箱', 'error')
        return
      }
      if (!regCode.value) {
        showToast('请输入验证码', 'error')
        return
      }
      if (!regPassword.value || regPassword.value.length < 6) {
        showToast('密码至少6位', 'error')
        return
      }
      if (regPassword.value !== regConfirmPassword.value) {
        showToast('两次密码输入不一致', 'error')
        return
      }

      const params = {
        username: regUsername.value,
        email: regEmail.value,
        code: regCode.value,
        password: regPassword.value,
        nickname: regNickname.value || regUsername.value
      }
      if (regGrade.value) params.grade = regGrade.value
      if (regSchool.value) params.school = regSchool.value

      regLoading.value = true
      try {
        const data = await api.auth.register(params)
        setToken(data.token)
        await loadUser()
        showToast('注册成功', 'success')
        router.push(landingPath())
      } catch (e) {
        showToast(e.message || '注册失败', 'error')
      } finally {
        regLoading.value = false
      }
    }

    return {
      route,
      loginUsername,
      loginPassword,
      loginLoading,
      regUsername,
      regEmail,
      regCode,
      regPassword,
      regConfirmPassword,
      regNickname,
      regGrade,
      regSchool,
      regCountdown,
      regLoading,
      handleLogin,
      handleRegister,
      sendRegCaptcha
    }
  },
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h2 style="text-align:center;margin-bottom:24px;font-size:24px;font-weight:600;">
          {{ mode === 'login' ? '登录 SmartMentor' : '注册 SmartMentor' }}
        </h2>

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
  `
}
