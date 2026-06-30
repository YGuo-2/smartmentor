import { ref, onMounted } from 'vue';
import { api } from '../api.js';
import { user, logout, showToast } from '../state.js';

export const DashboardPage = {
  template: `
<div class="dashboard-page">
  <nav class="topnav">
    <div class="topnav-brand">SmartMentor</div>
    <ul class="topnav-links">
      <li><router-link to="/dashboard" class="active">仪表盘</router-link></li>
      <li><router-link to="/diagnostic">诊断测试</router-link></li>
      <li><router-link to="/learning">学习路径</router-link></li>
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

  <div class="dashboard-container">
    <!-- Greeting bar -->
    <div class="greeting-bar">
      <h1>你好，{{ user?.nickname || user?.username || '同学' }}</h1>
      <span class="today-date">{{ todayDate }}</span>
    </div>

    <!-- Stats row -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-number">{{ stats.streak }}</div>
        <div class="stat-label">学习天数</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.level }}</div>
        <div class="stat-label">当前等级</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.xp }}</div>
        <div class="stat-label">今日XP</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.masteredPoints }}</div>
        <div class="stat-label">掌握知识点</div>
      </div>
    </div>

    <!-- Main grid -->
    <div class="dashboard-grid">
      <!-- Daily missions -->
      <div class="card">
        <div class="card-header">
          <h3>每日任务</h3>
          <span class="badge">{{ dailyProgress.completed }}/{{ dailyProgress.total }}</span>
        </div>
        <div class="card-body">
          <div v-if="missions.length === 0" class="empty-hint">暂无任务</div>
          <div class="mission-item" v-for="m in missions" :key="m.id" :class="{ completed: m.completed }">
            <div class="mission-info">
              <span class="mission-title">{{ m.title }}</span>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: (m.progress || 0) + '%' }"></div>
              </div>
              <span class="mission-progress-text">{{ m.progress || 0 }}%</span>
            </div>
            <button
              class="btn btn-sm btn-dark"
              :disabled="m.completed"
              @click="completeMission(m.id)"
            >{{ m.completed ? '已完成' : '完成' }}</button>
          </div>
        </div>
      </div>

      <!-- Current learning path -->
      <div class="card">
        <div class="card-header">
          <h3>当前学习路径</h3>
          <router-link to="/learning" class="card-link">查看全部</router-link>
        </div>
        <div class="card-body">
          <div v-if="!currentPath" class="empty-hint">
            暂无进行中的学习路径
            <router-link to="/diagnostic" class="btn btn-outline btn-sm" style="margin-top:12px">开始诊断</router-link>
          </div>
          <div v-else class="current-path">
            <h4>{{ currentPath.title }}</h4>
            <p class="path-desc">{{ currentPath.module }} · {{ currentPath.totalNodes }}个节点</p>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: currentPath.progress + '%' }"></div>
            </div>
            <div class="path-meta">
              <span>进度 {{ currentPath.progress }}%</span>
              <router-link :to="'/learning/' + currentPath.id" class="btn btn-sm btn-dark">继续学习</router-link>
            </div>
          </div>
        </div>
      </div>

      <!-- Module mastery overview -->
      <div class="card">
        <div class="card-header">
          <h3>模块掌握度</h3>
        </div>
        <div class="card-body">
          <div class="module-list">
            <div class="module-item" v-for="mod in moduleMastery" :key="mod.module">
              <div class="module-name">{{ mod.module }}</div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: mod.mastery + '%' }" :class="getMasteryClass(mod.mastery)"></div>
              </div>
              <span class="module-percent">{{ mod.mastery }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent activities -->
      <div class="card">
        <div class="card-header">
          <h3>最近动态</h3>
        </div>
        <div class="card-body">
          <div v-if="recentActivities.length === 0" class="empty-hint">暂无动态</div>
          <div class="activity-item" v-for="(act, idx) in recentActivities" :key="idx">
            <div class="activity-icon">{{ getActivityIcon(act.type) }}</div>
            <div class="activity-content">
              <span class="activity-title">{{ act.title }}</span>
              <span class="activity-time">{{ act.time }}</span>
            </div>
            <span class="badge" v-if="act.badge">{{ act.badge }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
  `,
  setup() {
    const showDropdown = ref(false);
    const loading = ref(true);

    const stats = ref({
      streak: 0,
      level: 1,
      xp: 0,
      masteredPoints: 0
    });

    const missions = ref([]);
    const dailyProgress = ref({ completed: 0, total: 0 });
    const currentPath = ref(null);
    const moduleMastery = ref([
      { module: '人工智能基础', mastery: 0 },
      { module: 'Java Web 开发', mastery: 0 },
      { module: '数字电路基础', mastery: 0 }
    ]);
    const recentActivities = ref([]);

    const now = new Date();
    const todayDate = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`;

    async function loadDashboard() {
      try {
        const data = await api.report.dashboard();
        stats.value.streak = data.streak ?? data.streakDays ?? 0;
        stats.value.level = data.level || 1;
        stats.value.xp = data.xp ?? data.experiencePoints ?? 0;
        stats.value.masteredPoints = data.masteredPoints ?? data.masteredKnowledgePoints ?? 0;

        moduleMastery.value = normalizeModuleMastery(data.moduleMastery);
        recentActivities.value = normalizeRecentActivities(data.recentActivities);
        currentPath.value = normalizeCurrentPath(data.currentPath);
      } catch (e) {
        console.error('Failed to load dashboard:', e);
      }
    }

    async function loadMissions() {
      try {
        const data = await api.engagement.missions();
        missions.value = normalizeMissions(data.missions || []);
        dailyProgress.value = normalizeMissionProgress(data);
      } catch (e) {
        console.error('Failed to load missions:', e);
      }
    }

    async function completeMission(missionId) {
      try {
        await api.engagement.completeMission(missionId);
        showToast('任务完成！获得经验奖励', 'success');
        // Refresh missions and dashboard
        await Promise.all([loadMissions(), loadDashboard()]);
      } catch (e) {
        showToast(e.message || '完成任务失败', 'error');
      }
    }

    function getMasteryClass(mastery) {
      if (mastery >= 80) return 'mastery-high';
      if (mastery >= 50) return 'mastery-mid';
      return 'mastery-low';
    }

    function getActivityIcon(type) {
      const icons = {
        diagnostic: '🎯',
        learning: '📐',
        chat: '💬',
        mission: '⭐',
        achievement: '🏆'
      };
      return icons[type] || '📝';
    }

    function toPercent(value) {
      const numeric = Number(value || 0);
      return Math.round(numeric <= 1 ? numeric * 100 : numeric);
    }

    function normalizeModuleMastery(raw) {
      if (Array.isArray(raw)) {
        return raw.map(item => ({
          module: item.module || item.name || '模块',
          mastery: toPercent(item.mastery ?? item.value ?? 0)
        }));
      }
      if (raw?.dimensions && raw?.values) {
        return raw.dimensions.map((module, index) => ({
          module,
          mastery: toPercent(raw.values[index] ?? 0)
        }));
      }
      return moduleMastery.value;
    }

    function normalizeRecentActivities(raw) {
      if (!Array.isArray(raw)) return [];
      return raw.flatMap(day => {
        if (!Array.isArray(day.activities)) return [day];
        return day.activities.map(activity => ({
          type: activity.type,
          title: activity.title || activity.description || activity.result || '学习活动',
          time: day.date || activity.createdAt || '',
          badge: activity.duration || ''
        }));
      });
    }

    function normalizeCurrentPath(raw) {
      if (!raw?.pathId) return null;
      return {
        ...raw,
        id: raw.pathId,
        title: raw.title || raw.name || '学习路径',
        module: raw.module || '高校课程',
        progress: toPercent(raw.progress),
        totalNodes: raw.totalNodes || 0
      };
    }

    function normalizeMissions(raw) {
      return raw.map(item => ({
        ...item,
        id: item.id || item.missionId,
        completed: item.completed ?? item.status === 'completed',
        xp: item.xp ?? item.rewardExp ?? 0,
        progress: item.progress ?? (item.status === 'completed' ? 100 : 0)
      }));
    }

    function normalizeMissionProgress(data) {
      if (data.dailyProgress) return data.dailyProgress;
      const total = data.missions?.length || 0;
      const completed = data.missions?.filter(m => m.status === 'completed' || m.completed).length || 0;
      return { completed, total };
    }

    function handleLogout() {
      showDropdown.value = false;
      logout();
    }

    onMounted(async () => {
      await Promise.all([loadDashboard(), loadMissions()]);
      loading.value = false;
    });

    return {
      user,
      showDropdown,
      loading,
      stats,
      missions,
      dailyProgress,
      currentPath,
      moduleMastery,
      recentActivities,
      todayDate,
      completeMission,
      getMasteryClass,
      getActivityIcon,
      handleLogout
    };
  }
};
