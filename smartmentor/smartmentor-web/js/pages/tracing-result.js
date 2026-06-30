import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { showToast } from '../state.js'

export const TracingResultPage = {
  name: 'TracingResultPage',
  props: {
    tracingId: { type: String, required: true }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(true)
    const result = ref(null)
    const generating = ref(false)

    async function loadResult() {
      loading.value = true
      try {
        result.value = await api.tracing.detail(props.tracingId)
      } catch (e) {
        showToast(e.message || '加载溯因结果失败', 'error')
      } finally {
        loading.value = false
      }
    }

    function pointName(item) {
      return item?.knowledgePointName || item?.knowledgePoint || item?.kpName || item?.name || item?.knowledgePointId || '未知知识点'
    }

    async function generatePath() {
      if (!result.value?.tracingId) {
        showToast('溯因结果尚未加载完成', 'warning')
        return
      }
      generating.value = true
      try {
        const path = await api.learning.generate({
          tracingId: result.value.tracingId,
          mode: 'systematic',
          dailyStudyMinutes: 30
        })
        const nodes = path?.nodes || []
        const firstNode = nodes.find(n => ['in_progress', 'unlocked', 'pending', 'available'].includes(n.status)) || nodes[0]
        showToast('学习路径已创建', 'success')
        if (path?.pathId && firstNode?.nodeId) {
          router.push('/learning/' + path.pathId + '/' + firstNode.nodeId)
        } else if (path?.pathId) {
          router.push('/learning/' + path.pathId)
        } else {
          router.push('/learning')
        }
      } catch (e) {
        showToast(e.message || '生成学习路径失败', 'error')
      } finally {
        generating.value = false
      }
    }

    onMounted(loadResult)

    return { loading, result, generating, pointName, generatePath }
  },
  template: `
    <div class="tracing-result-page">
      <nav class="topnav">
        <div class="topnav-brand">SmartMentor</div>
        <div class="topnav-links">
          <router-link to="/dashboard">仪表盘</router-link>
          <router-link to="/diagnostic/history">诊断历史</router-link>
          <router-link to="/learning">学习路径</router-link>
        </div>
      </nav>

      <div class="page-container">
        <div v-if="loading" class="loading-state">加载中...</div>
        <template v-else-if="result">
          <div class="page-header">
            <h1 class="page-title">溯因分析结果</h1>
            <p class="page-subtitle">基于诊断结果定位薄弱知识点与前置根因</p>
          </div>

          <div class="card" v-if="result.suggestion">
            <div class="card-header"><h2>AI 分析建议</h2></div>
            <div class="card-body" style="white-space:pre-wrap;line-height:1.8">{{ result.suggestion }}</div>
          </div>

          <div class="card" v-if="(result.mergedRootCauses || []).length">
            <div class="card-header"><h2>根因知识点</h2></div>
            <div class="card-body">
              <div v-for="item in result.mergedRootCauses" :key="item.knowledgePointId || item.id" class="weak-point-item">
                <div class="weak-point-name">{{ pointName(item) }}</div>
                <div class="weak-point-detail">{{ item.reason || item.explanation || item.description || '建议优先补齐该前置基础' }}</div>
              </div>
            </div>
          </div>

          <div class="card" v-if="(result.suggestedLearningPath || []).length">
            <div class="card-header"><h2>建议学习顺序</h2></div>
            <div class="card-body">
              <ol>
                <li v-for="item in result.suggestedLearningPath" :key="item.knowledgePointId || item.id">
                  {{ pointName(item) }}
                </li>
              </ol>
            </div>
          </div>

          <div class="action-buttons">
            <button class="btn btn-dark" :disabled="generating" @click="generatePath">
              {{ generating ? '生成中...' : '生成学习路径' }}
            </button>
            <router-link class="btn btn-outline" to="/diagnostic/history">返回历史</router-link>
          </div>
        </template>
      </div>
    </div>
  `
}
