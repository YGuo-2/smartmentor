import { ref, onMounted } from 'vue'
import { api } from '../api.js'
import { showToast } from '../state.js'

export const LearningPathsPage = {
  name: 'LearningPathsPage',
  setup() {
    const paths = ref([])
    const loading = ref(true)

    const fetchPaths = async () => {
      try {
        loading.value = true
        const res = await api.learning.activePaths()
        paths.value = res.paths || []
      } catch (e) {
        showToast('加载学习路径失败', 'error')
      } finally {
        loading.value = false
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const d = new Date(dateStr)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }

    onMounted(() => {
      fetchPaths()
    })

    return { paths, loading, formatDate }
  },
  template: `
    <div class="learning-paths-page">
      <nav class="topnav">
        <div class="topnav-brand">SmartMentor</div>
        <div class="topnav-links">
          <router-link to="/dashboard">仪表盘</router-link>
          <router-link to="/learning">学习路径</router-link>
          <router-link to="/diagnostic">诊断测试</router-link>
          <router-link to="/profile">个人中心</router-link>
        </div>
      </nav>

      <div class="page-container">
        <div class="page-header">
          <h1 class="page-title">学习路径</h1>
          <p class="page-subtitle">查看和管理您的个性化学习路径</p>
        </div>

        <div v-if="loading" class="loading-state">
          <p>加载中...</p>
        </div>

        <div v-else-if="paths.length === 0" class="empty-state">
          <p>暂无学习路径，完成诊断测试后即可生成个性化学习路径</p>
          <router-link to="/diagnostic" class="btn btn-dark">开始诊断测试</router-link>
        </div>

        <div v-else class="paths-list">
          <router-link
            v-for="path in paths"
            :key="path.pathId"
            :to="'/learning/' + path.pathId"
            class="card path-card"
          >
            <div class="card-header">
              <div class="path-title-row">
                <h3 class="path-title">{{ path.title }}</h3>
                <span class="badge" :class="'badge-' + path.status">
                  {{ path.status === 'completed' ? '已完成' : '进行中' }}
                </span>
              </div>
              <span class="path-module">{{ path.module }}</span>
            </div>
            <div class="card-body">
              <div class="path-meta">
                <span class="path-date">创建于 {{ formatDate(path.createdAt) }}</span>
                <span class="path-nodes">节点：{{ path.completedNodes }} / {{ path.totalNodes }}</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: path.progress + '%' }"></div>
              </div>
              <div class="path-progress-text">{{ path.progress }}%</div>
              <div class="path-actions">
                <span class="btn btn-dark">继续学习</span>
              </div>
            </div>
          </router-link>
        </div>
      </div>
    </div>
  `
}
