import { ref, onMounted } from 'vue';
import { api } from '../api.js';
import { user, logout, showToast } from '../state.js';

export const DiagnosticHistoryPage = {
  template: `
<div class="diagnostic-history-page">
  <nav class="topnav">
    <div class="topnav-brand">SmartMentor</div>
    <ul class="topnav-links">
      <li><router-link to="/dashboard">仪表盘</router-link></li>
      <li><router-link to="/diagnostic" class="active">诊断测试</router-link></li>
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

  <div class="main-content">
    <h1 class="page-title">诊断历史</h1>

    <!-- Filter row -->
    <div class="card" style="margin-bottom:24px;">
      <div class="card-body" style="display:flex;gap:16px;align-items:center;flex-wrap:wrap;">
        <select class="form-input" v-model="filterModule" @change="handleFilter" style="width:180px;" aria-label="筛选诊断模块" title="筛选诊断模块">
          <option value="">全部模块</option>
          <option v-for="m in modules" :key="m.value" :value="m.value">{{ m.label }}</option>
        </select>
        <button class="btn" @click="handleFilter">筛选</button>
      </div>
    </div>

    <!-- History table -->
    <div class="card">
      <div class="card-header">
        <span>诊断记录</span>
        <span style="font-size:0.85rem;color:var(--text-secondary);">共 {{ total }} 条记录</span>
      </div>
      <div class="card-body" style="padding:0;">
        <table class="table" v-if="records.length > 0">
          <thead>
            <tr>
              <th>日期</th>
              <th>模块</th>
              <th>得分</th>
              <th>题目数</th>
              <th>掌握水平</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in records" :key="record.diagnosticId">
              <td>{{ formatDate(record.startTime || record.createdAt) }}</td>
              <td>{{ getModuleLabel(record.module) }}</td>
              <td>{{ toPercent(record.accuracy ?? record.score) }}%</td>
              <td>{{ record.totalQuestions }}</td>
              <td><span class="badge" :class="getMasteryClass(record.masteryLevel || record.status)">{{ getMasteryLabel(record.masteryLevel || record.status) }}</span></td>
              <td>
                <router-link :to="'/diagnostic/result/' + record.diagnosticId" class="btn btn-sm">查看详情</router-link>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else style="padding:40px;text-align:center;color:var(--text-secondary);">
          暂无诊断记录
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="btn" :disabled="currentPage <= 0" @click="goToPage(currentPage - 1)">上一页</button>
      <span style="margin:0 12px;font-size:0.9rem;color:var(--text-secondary);">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
      <button class="btn" :disabled="currentPage >= totalPages - 1" @click="goToPage(currentPage + 1)">下一页</button>
    </div>
  </div>
</div>
  `,
  setup() {
    const showDropdown = ref(false);
    const records = ref([]);
    const total = ref(0);
    const currentPage = ref(0);
    const totalPages = ref(1);
    const pageSize = 10;

    const filterModule = ref('');
    const modules = [
      { value: '人工智能基础', label: '人工智能基础' },
      { value: 'Java Web 开发', label: 'Java Web 开发' },
      { value: '数字电路基础', label: '数字电路基础' }
    ];

    async function fetchHistory(page = 0) {
      try {
        const params = { page, pageSize };
        if (filterModule.value) {
          params.module = filterModule.value;
        }
        const data = await api.diagnostic.history(params);
        records.value = data.records || [];
        total.value = data.total || 0;
        currentPage.value = (data.page ?? page);
        totalPages.value = data.totalPages || Math.ceil((data.total || 0) / pageSize);
      } catch (e) {
        showToast(e.message || '加载诊断历史失败', 'error');
      }
    }

    function handleFilter() {
      currentPage.value = 0;
      fetchHistory(0);
    }

    function goToPage(page) {
      if (page < 0 || page >= totalPages.value) return;
      currentPage.value = page;
      fetchHistory(page);
    }

    function formatDate(dateStr) {
      if (!dateStr) return '-';
      const d = new Date(dateStr);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const hours = String(d.getHours()).padStart(2, '0');
      const minutes = String(d.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    }

    function getModuleLabel(moduleKey) {
      const found = modules.find(m => m.value === moduleKey);
      return found ? found.label : moduleKey || '-';
    }

    function getMasteryClass(level) {
      const classMap = {
        'mastered': 'badge-success',
        'proficient': 'badge-info',
        'developing': 'badge-warning',
        'beginner': 'badge-danger'
      };
      return classMap[level] || 'badge-default';
    }

    function getMasteryLabel(level) {
      const labelMap = {
        'mastered': '精通',
        'proficient': '熟练',
        'developing': '发展中',
        'beginner': '入门',
        'completed': '已完成',
        'in_progress': '进行中'
      };
      return labelMap[level] || level || '-';
    }

    function toPercent(value) {
      const numeric = Number(value || 0);
      return Math.round(numeric <= 1 ? numeric * 100 : numeric);
    }

    function handleLogout() {
      logout();
    }

    onMounted(() => {
      fetchHistory(0);
    });

    return {
      user,
      showDropdown,
      records,
      total,
      currentPage,
      totalPages,
      filterModule,
      modules,
      handleFilter,
      goToPage,
      formatDate,
      getModuleLabel,
      getMasteryClass,
      getMasteryLabel,
      toPercent,
      handleLogout
    };
  }
};
