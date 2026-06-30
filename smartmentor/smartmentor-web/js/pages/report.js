import { ref, onMounted } from 'vue';
import { api } from '../api.js';
import { showToast } from '../state.js';
import { user, logout } from '../state.js';

export const ReportPage = {
  template: `
<div class="report-page">
  <nav class="topnav">
    <div class="topnav-brand">SmartMentor</div>
    <ul class="topnav-links">
      <li><router-link to="/dashboard">仪表盘</router-link></li>
      <li><router-link to="/diagnostic">诊断测试</router-link></li>
      <li><router-link to="/learning">学习路径</router-link></li>
      <li><router-link to="/chat">AI对话</router-link></li>
      <li><router-link to="/report" class="active">学习报告</router-link></li>
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

  <div class="report-container">
    <!-- Page header -->
    <div class="report-header">
      <h1>学习报告</h1>
      <div class="tabs">
        <button
          class="tab-item"
          :class="{ active: selectedPeriod === '7d' }"
          @click="changePeriod('7d')"
        >最近7天</button>
        <button
          class="tab-item"
          :class="{ active: selectedPeriod === '30d' }"
          @click="changePeriod('30d')"
        >最近30天</button>
        <button
          class="tab-item"
          :class="{ active: selectedPeriod === '90d' }"
          @click="changePeriod('90d')"
        >最近90天</button>
      </div>
    </div>

    <!-- Summary stats row -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-number">{{ formatStudyTime(summary.totalStudyTime) }}</div>
        <div class="stat-label">总学习时长</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ summary.totalQuestions }}</div>
        <div class="stat-label">完成题目数</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ summary.averageAccuracy }}%</div>
        <div class="stat-label">平均正确率</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">+{{ summary.improvementRate }}%</div>
        <div class="stat-label">能力提升幅度</div>
      </div>
    </div>

    <!-- Pre-post comparison card -->
    <div class="card">
      <div class="card-header">
        <h3>前后对比</h3>
        <span class="badge">真实历史记录</span>
      </div>
      <div class="card-body">
        <div v-if="prePostComparison.length === 0" class="empty-hint">暂无对比数据</div>
        <div class="comparison-item" v-for="item in prePostComparison" :key="item.module">
          <div class="comparison-module">{{ item.module }}</div>
          <div class="comparison-bars">
            <div class="comparison-bar-row">
              <span class="comparison-bar-label">前</span>
              <div class="progress-bar">
                <div class="progress-fill comparison-pre" :style="{ width: item.preMastery + '%' }"></div>
              </div>
              <span class="comparison-bar-value">{{ item.preMastery }}%</span>
            </div>
            <div class="comparison-bar-row">
              <span class="comparison-bar-label">后</span>
              <div class="progress-bar">
                <div class="progress-fill comparison-post" :style="{ width: item.postMastery + '%' }"></div>
              </div>
              <span class="comparison-bar-value">{{ item.postMastery }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Mastery curve card -->
    <div class="card">
      <div class="card-header">
        <h3>掌握度变化曲线</h3>
        <span class="badge">时间趋势</span>
      </div>
      <div class="card-body">
        <div v-if="masteryCurve.length === 0" class="empty-hint">暂无趋势数据</div>
        <div v-else class="mastery-curve-visual">
          <div class="mastery-curve-row" v-for="(point, idx) in masteryCurve" :key="idx">
            <span class="mastery-curve-date">{{ point.date }}</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: point.mastery + '%' }"></div>
            </div>
            <span class="mastery-curve-value">{{ point.mastery }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Error elimination card -->
    <div class="card">
      <div class="card-header">
        <h3>错误消除情况</h3>
        <span class="badge">{{ totalEliminated }}/{{ totalErrors }} 已消除</span>
      </div>
      <div class="card-body">
        <div v-if="errorElimination.length === 0" class="empty-hint">暂无错误数据</div>
        <div class="error-elimination-list">
          <div class="error-elimination-item" v-for="item in errorElimination" :key="item.errorType">
            <div class="error-elimination-header">
              <span class="error-type-name">{{ item.errorType }}</span>
              <span class="error-type-stat">{{ item.eliminatedCount }}/{{ item.totalCount }} 已消除 ({{ item.rate }}%)</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :class="getEliminationClass(item.rate)" :style="{ width: item.rate + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Ability radar card -->
    <div class="card">
      <div class="card-header">
        <h3>能力雷达</h3>
        <span class="badge">当前掌握度</span>
      </div>
      <div class="card-body">
        <div v-if="abilityRadar.length === 0" class="empty-hint">暂无能力数据</div>
        <div class="ability-radar-list">
          <div class="ability-radar-item" v-for="item in abilityRadar" :key="item.dimension">
            <div class="ability-radar-header">
              <span class="ability-dimension">{{ item.dimension }}</span>
              <span class="ability-score">{{ item.score }}分</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill ability-fill" :style="{ width: item.score + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
  `,
  setup() {
    const showDropdown = ref(false);
    const loading = ref(false);
    const selectedPeriod = ref('7d');

    const summary = ref({
      totalStudyTime: 0,
      totalQuestions: 0,
      averageAccuracy: 0,
      improvementRate: 0
    });

    const prePostComparison = ref([]);
    const masteryCurve = ref([]);
    const errorElimination = ref([]);
    const abilityRadar = ref([]);

    const totalErrors = ref(0);
    const totalEliminated = ref(0);

    function formatStudyTime(minutes) {
      if (!minutes) return '0分钟';
      if (minutes < 60) return minutes + '分钟';
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? hours + '小时' + mins + '分钟' : hours + '小时';
    }

    function getEliminationClass(rate) {
      if (rate >= 80) return 'elimination-high';
      if (rate >= 50) return 'elimination-mid';
      return 'elimination-low';
    }

    async function loadReport() {
      loading.value = true;
      try {
        const data = await api.report.effectiveness({ period: selectedPeriod.value });

        summary.value = normalizeSummary(data.overallSummary || data.summary || {});
        prePostComparison.value = normalizeComparison(data.masteryComparison || data.prePostComparison || []);
        masteryCurve.value = normalizeMasteryCurve(data.masteryCurve || []);
        errorElimination.value = normalizeErrorElimination(data.errorElimination || []);
        abilityRadar.value = normalizeAbilityRadar(data.abilityRadar || []);

        // Calculate totals for error elimination
        let errors = 0;
        let eliminated = 0;
        errorElimination.value.forEach(item => {
          errors += item.totalCount || 0;
          eliminated += item.eliminatedCount || 0;
        });
        totalErrors.value = errors;
        totalEliminated.value = eliminated;

      } catch (e) {
        showToast(e.message || '加载报告失败', 'error');
        console.error('Failed to load report:', e);
      } finally {
        loading.value = false;
      }
    }

    function changePeriod(period) {
      selectedPeriod.value = period;
      loadReport();
    }

    function toPercent(value) {
      const numeric = Number(value || 0);
      return Math.round(numeric <= 1 ? numeric * 100 : numeric);
    }

    function normalizeSummary(raw) {
      return {
        totalStudyTime: Math.round(Number(raw.totalStudyMinutes ?? (Number(raw.totalStudyHours || 0) * 60))),
        totalQuestions: raw.totalQuestions ?? raw.totalQuestionsAnswered ?? 0,
        averageAccuracy: toPercent(raw.averageAccuracy ?? raw.accuracy ?? 0),
        improvementRate: toPercent(raw.improvementRate ?? 0)
      };
    }

    function normalizeComparison(items) {
      return items.map(item => ({
        module: item.module || item.name || item.knowledgePointName || '知识点',
        preMastery: toPercent(item.preMastery ?? item.preTestMastery ?? 0),
        postMastery: toPercent(item.postMastery ?? item.postTestMastery ?? 0)
      })).filter(item => item.preMastery || item.postMastery);
    }

    function normalizeMasteryCurve(items) {
      return items.map(item => ({ ...item, mastery: toPercent(item.mastery) }));
    }

    function normalizeErrorElimination(items) {
      return items.map(item => {
        const totalCount = Number(item.totalCount ?? item.countBefore ?? 0);
        const remaining = Number(item.countAfter ?? Math.max(totalCount - Number(item.eliminatedCount || 0), 0));
        const eliminatedCount = Number(item.eliminatedCount ?? Math.max(totalCount - remaining, 0));
        return {
          errorType: item.errorType || '未分类错误',
          totalCount,
          eliminatedCount,
          rate: toPercent(item.rate ?? item.eliminationRate ?? 0)
        };
      }).filter(item => item.totalCount > 0);
    }

    function normalizeAbilityRadar(raw) {
      if (Array.isArray(raw)) return raw;
      const dimensions = raw.dimensions || [];
      const values = raw.after || raw.values || [];
      return dimensions.map((dimension, index) => ({
        dimension,
        score: toPercent(values[index] ?? 0)
      })).filter(item => item.score > 0);
    }

    function handleLogout() {
      showDropdown.value = false;
      logout();
    }

    onMounted(() => {
      loadReport();
    });

    return {
      user,
      showDropdown,
      loading,
      selectedPeriod,
      summary,
      prePostComparison,
      masteryCurve,
      errorElimination,
      abilityRadar,
      totalErrors,
      totalEliminated,
      formatStudyTime,
      getEliminationClass,
      changePeriod,
      handleLogout
    };
  }
};
