import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api.js';
import { showToast } from '../state.js';
import { user, logout } from '../state.js';

export const LearningPathDetailPage = {
  props: {
    pathId: { type: String, required: true }
  },
  template: `
<div class="learning-path-detail-page">
  <nav class="topnav">
    <div class="topnav-brand">SmartMentor</div>
    <ul class="topnav-links">
      <li><router-link to="/dashboard">仪表盘</router-link></li>
      <li><router-link to="/diagnostic">诊断测试</router-link></li>
      <li><router-link to="/learning" class="active">学习路径</router-link></li>
      <li><router-link to="/chat">AI对话</router-link></li>
      <li><router-link to="/report">学习报告</router-link></li>
    </ul>
    <div class="topnav-user">
      <button class="topnav-user-btn" @click="showDropdown = !showDropdown">
        {{ user?.nickname || user?.username || '用户' }}
        <span class="caret">▾</span>
      </button>
      <div class="topnav-dropdown" v-if="showDropdown">
        <router-link to="/profile" @click="showDropdown = false">个人设置</router-link>
        <a href="#" @click.prevent="handleLogout">退出登录</a>
      </div>
    </div>
  </nav>

  <div class="path-detail-container" style="max-width:900px;margin:32px auto;padding:0 20px;">
    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:80px 0;color:#999;">加载中...</div>

    <!-- Error -->
    <div v-else-if="error" style="text-align:center;padding:80px 0;color:#e53e3e;">
      <p>{{ error }}</p>
      <button class="btn" @click="fetchDetail" style="margin-top:16px;">重试</button>
    </div>

    <template v-else>
      <!-- Back link -->
      <div style="margin-bottom:20px;">
        <router-link to="/learning" style="color:#4a6cf7;text-decoration:none;font-size:14px;">← 返回学习路径列表</router-link>
      </div>

      <!-- Path header card -->
      <div class="card" style="margin-bottom:28px;">
        <div class="card-header" style="flex-direction:column;align-items:flex-start;gap:12px;">
          <div style="display:flex;justify-content:space-between;align-items:center;width:100%;">
            <h2 style="margin:0;font-size:20px;">{{ pathData.title }}</h2>
            <span class="badge" :class="'badge-' + pathData.status">{{ statusLabel(pathData.status) }}</span>
          </div>
          <div style="color:#666;font-size:14px;">
            <span>模块：{{ pathData.module }}</span>
            <span style="margin-left:20px;">节点：{{ pathData.completedNodes }}/{{ pathData.totalNodes }}</span>
            <span style="margin-left:20px;">预计总时长：{{ formatTime(pathData.estimatedTotalTime) }}</span>
          </div>
          <div style="width:100%;margin-top:4px;">
            <div style="display:flex;align-items:center;gap:12px;">
              <div class="progress-bar" style="flex:1;">
                <div class="progress-fill" :style="{ width: pathData.progress + '%' }"></div>
              </div>
              <span style="font-size:14px;font-weight:600;white-space:nowrap;">{{ pathData.progress }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Path stats -->
      <div style="display:flex;gap:16px;margin-bottom:28px;">
        <div class="card" style="flex:1;padding:20px;text-align:center;">
          <div style="font-size:24px;font-weight:700;color:#4a6cf7;">{{ pathData.completedNodes }}</div>
          <div style="font-size:13px;color:#666;margin-top:4px;">已完成节点</div>
        </div>
        <div class="card" style="flex:1;padding:20px;text-align:center;">
          <div style="font-size:24px;font-weight:700;color:#4a6cf7;">{{ completionRate }}%</div>
          <div style="font-size:13px;color:#666;margin-top:4px;">完成率</div>
        </div>
        <div class="card" style="flex:1;padding:20px;text-align:center;">
          <div style="font-size:24px;font-weight:700;color:#4a6cf7;">{{ formatTime(totalTimeSpent) }}</div>
          <div style="font-size:13px;color:#666;margin-top:4px;">已用时长</div>
        </div>
      </div>

      <!-- Node timeline -->
      <div class="card">
        <div class="card-header">
          <h3 style="margin:0;">学习节点</h3>
          <span class="badge">共 {{ pathData.totalNodes }} 个节点</span>
        </div>
        <div class="path-timeline">
          <div
            v-for="(node, index) in sortedNodes"
            :key="node.nodeId"
            class="path-node"
            :class="node.status"
            @click="goToNode(node)"
            :style="{ cursor: node.status === 'locked' ? 'not-allowed' : 'pointer' }"
          >
            <!-- Timeline connector -->
            <div class="node-timeline-track">
              <div class="node-status-icon" :class="node.status">
                <span v-if="node.status === 'locked'">🔒</span>
                <span v-else-if="node.status === 'completed'">✓</span>
                <span v-else-if="node.status === 'failed'">✗</span>
                <span v-else-if="node.status === 'in_progress'">●</span>
                <span v-else>○</span>
              </div>
              <div v-if="index < sortedNodes.length - 1" class="timeline-line" :class="node.status"></div>
            </div>

            <!-- Node info -->
            <div class="node-info">
              <div class="node-title">
                {{ node.title }}
                <span class="badge" style="margin-left:8px;font-size:11px;">{{ typeLabel(node.type) }}</span>
              </div>
              <div class="node-meta">
                <span>{{ node.knowledgePoint }}</span>
                <span style="margin-left:12px;">预计 {{ formatTime(node.estimatedTime) }}</span>
                <span class="badge" :class="'badge-' + node.status" style="margin-left:12px;">{{ nodeStatusLabel(node.status) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</div>
  `,
  setup(props) {
    const router = useRouter();
    const showDropdown = ref(false);
    const loading = ref(true);
    const error = ref('');

    const pathData = ref({
      pathId: '',
      title: '',
      module: '',
      progress: 0,
      status: '',
      totalNodes: 0,
      completedNodes: 0,
      estimatedTotalTime: 0,
      nodes: []
    });

    const sortedNodes = ref([]);
    const totalTimeSpent = ref(0);
    const completionRate = ref(0);

    function formatTime(minutes) {
      if (!minutes && minutes !== 0) return '0分钟';
      if (minutes < 60) return minutes + '分钟';
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      return m > 0 ? h + '小时' + m + '分钟' : h + '小时';
    }

    function statusLabel(status) {
      const map = {
        active: '进行中',
        completed: '已完成',
        paused: '已暂停',
        in_progress: '进行中'
      };
      return map[status] || status || '未知';
    }

    function nodeStatusLabel(status) {
      const map = {
        locked: '未解锁',
        available: '可学习',
        in_progress: '学习中',
        completed: '已完成',
        failed: '未通过'
      };
      return map[status] || status || '未知';
    }

    function typeLabel(type) {
      const map = {
        lesson: '课程',
        exercise: '练习',
        checkpoint: '检查点'
      };
      return map[type] || type || '未知';
    }

    function goToNode(node) {
      if (node.status === 'locked') {
        showToast('该节点尚未解锁', 'warning');
        return;
      }
      router.push('/learning/' + props.pathId + '/' + node.nodeId);
    }

    async function fetchDetail() {
      loading.value = true;
      error.value = '';
      try {
        const data = await api.learning.pathDetail(props.pathId);
        pathData.value = data;
        sortedNodes.value = [...(data.nodes || [])].sort((a, b) => a.order - b.order);

        // Calculate stats
        const completed = (data.nodes || []).filter(n => n.status === 'completed');
        completionRate.value = data.totalNodes > 0
          ? Math.round((completed.length / data.totalNodes) * 100)
          : 0;
        totalTimeSpent.value = completed.reduce((sum, n) => sum + (n.estimatedTime || 0), 0);
      } catch (e) {
        error.value = e.message || '加载失败';
        showToast(error.value, 'error');
      } finally {
        loading.value = false;
      }
    }

    function handleLogout() {
      logout();
      router.push('/login');
    }

    onMounted(() => {
      fetchDetail();
    });

    return {
      user,
      showDropdown,
      loading,
      error,
      pathData,
      sortedNodes,
      totalTimeSpent,
      completionRate,
      formatTime,
      statusLabel,
      nodeStatusLabel,
      typeLabel,
      goToNode,
      fetchDetail,
      handleLogout
    };
  }
};
