<template>
<div class="diagnostic-history-page">

  <div class="main-content">
    <div class="page-wrap">

      <!-- Page Hero -->
      <div class="dh-hero anim-1">
        <div>
          <div class="page-hero-label page-hero--blue">
            <i class="ri-history-line"></i> 诊断档案
          </div>
          <h1 style="font-size:2rem;font-weight:800;margin:4px 0 8px">诊断历史</h1>
          <p style="color:var(--text-secondary);font-size:0.88rem;margin:0">查看你的所有诊断测试记录与分析结果</p>
        </div>
        <router-link to="/diagnostic" class="btn btn-dark btn-sm">
          <i class="ri-add-line"></i> 新建诊断
        </router-link>
      </div>

      <!-- Summary strip -->
      <div class="dh-stats anim-2">
        <div class="card dh-stat-card">
          <div class="card-body dh-stat-body">
            <div class="icon-badge icon-badge-sm icon-badge-blue"><i class="ri-history-line"></i></div>
            <div>
              <div class="dh-stat-num">{{ total }}</div>
              <div class="hero-stat-label">诊断次数</div>
            </div>
          </div>
        </div>
        <div class="card dh-stat-card">
          <div class="card-body dh-stat-body">
            <div class="icon-badge icon-badge-sm icon-badge-amber"><i class="ri-percent-line"></i></div>
            <div>
              <div class="dh-stat-num">{{ averageAccuracy }}%</div>
              <div class="hero-stat-label">平均正确率</div>
            </div>
          </div>
        </div>
        <div class="card dh-stat-card">
          <div class="card-body dh-stat-body">
            <div class="icon-badge icon-badge-sm icon-badge-green"><i class="ri-book-open-line"></i></div>
            <div>
              <div class="dh-stat-num">{{ topModule || '—' }}</div>
              <div class="hero-stat-label">最多诊断模块</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Toolbar -->
      <div class="dh-toolbar anim-3">
        <div class="dh-filter-group">
          <i class="ri-filter-3-line" style="color:var(--text-muted)"></i>
          <select class="dh-select" v-model="filterModule" @change="handleFilter" aria-label="筛选诊断模块" title="筛选诊断模块">
            <option value="">全部模块</option>
            <option v-for="m in modules" :key="m" :value="m">{{ m }}</option>
          </select>
        </div>
        <span class="dh-total-hint">共 {{ total }} 条记录</span>
      </div>

      <!-- Records table -->
      <div class="card anim-4">
        <div class="card-body" style="padding:0">
          <table class="table dh-table" v-if="records.length > 0">
            <thead>
              <tr>
                <th><i class="ri-calendar-line"></i> 日期</th>
                <th><i class="ri-book-2-line"></i> 模块</th>
                <th><i class="ri-percent-line"></i> 正确率</th>
                <th><i class="ri-question-line"></i> 题目数</th>
                <th><i class="ri-bar-chart-fill"></i> 掌握度</th>
                <th><i class="ri-checkbox-circle-line"></i> 状态</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in records" :key="record.diagnosticId" class="dh-row">
                <td class="dh-date">{{ formatDate(record.startTime) }}</td>
                <td>
                  <span class="dh-module-tag">{{ record.module }}</span>
                </td>
                <td>
                  <span class="dh-accuracy" :class="getAccuracyClass(record.accuracy)">
                    {{ formatAccuracy(record.accuracy) }}
                  </span>
                </td>
                <td class="dh-count">{{ record.totalQuestions }}</td>
                <td>
                  <span class="badge" :class="getMasteryClass(record.overallMastery)">
                    <i class="ri-bar-chart-fill"></i> {{ formatMastery(record.overallMastery) }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="record.status === 'completed' ? 'badge-green' : 'badge-info'">
                    <i :class="record.status === 'completed' ? 'ri-check-line' : 'ri-time-line'"></i>
                    {{ record.status === 'completed' ? '已完成' : '进行中' }}
                  </span>
                </td>
                <td>
                  <router-link :to="'/diagnostic/result/' + record.diagnosticId" class="dh-detail-btn">
                    查看 <i class="ri-arrow-right-line"></i>
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="dh-empty">
            <i class="ri-file-list-3-line" style="font-size:2.5rem;color:var(--text-muted);display:block;margin-bottom:12px"></i>
            暂无诊断记录
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="dh-pagination anim-5" v-if="totalPages > 1">
        <button class="btn btn-outline btn-sm" :disabled="currentPage <= 0" @click="goToPage(currentPage - 1)">
          <i class="ri-arrow-left-line"></i> 上一页
        </button>
        <span class="dh-page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
        <button class="btn btn-outline btn-sm" :disabled="currentPage >= totalPages - 1" @click="goToPage(currentPage + 1)">
          下一页 <i class="ri-arrow-right-line"></i>
        </button>
      </div>

    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api/index.js'
import { showToast } from '../composables/state.js'

const records = ref([])
const total = ref(0)
const currentPage = ref(0)
const totalPages = ref(1)
const pageSize = 10
const filterModule = ref('')

const modules = ['人工智能基础', 'Java Web 开发', '数字电路基础']

const averageAccuracy = computed(() => {
  if (!records.value.length) return 0
  const valid = records.value.filter(r => r.accuracy != null)
  if (!valid.length) return 0
  return Math.round(valid.reduce((s, r) => s + r.accuracy, 0) / valid.length * 100)
})

const topModule = computed(() => {
  if (!records.value.length) return ''
  const counts = {}
  records.value.forEach(r => { if (r.module) counts[r.module] = (counts[r.module] || 0) + 1 })
  return Object.entries(counts).sort((a, b) => b[1] - a[1])[0]?.[0] || ''
})

async function fetchHistory(page = 0) {
  try {
    const params = { page, pageSize }
    if (filterModule.value) params.module = filterModule.value
    const data = await api.diagnostic.history(params)
    records.value = data.records || []
    total.value = data.total || 0
    currentPage.value = data.page != null ? data.page : page
    totalPages.value = data.totalPages || Math.ceil((data.total || 0) / pageSize)
  } catch (e) { showToast(e.message || '加载诊断历史失败', 'error') }
}

function handleFilter() { currentPage.value = 0; fetchHistory(0) }
function goToPage(page) { if (page < 0 || page >= totalPages.value) return; currentPage.value = page; fetchHistory(page) }

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function formatAccuracy(accuracy) {
  if (accuracy == null) return '-'
  return (accuracy * 100).toFixed(0) + '%'
}

function formatMastery(mastery) {
  if (mastery == null) return '-'
  return (mastery * 100).toFixed(0) + '%'
}

function getMasteryClass(mastery) {
  if (mastery == null) return 'badge-default'
  if (mastery >= 0.8) return 'badge-green'
  if (mastery >= 0.6) return 'badge-info'
  if (mastery >= 0.4) return 'badge-yellow'
  return 'badge-red'
}

function getAccuracyClass(accuracy) {
  if (accuracy == null) return ''
  if (accuracy >= 0.8) return 'acc-high'
  if (accuracy >= 0.6) return 'acc-mid'
  return 'acc-low'
}

onMounted(() => { fetchHistory(0) })
</script>

<style scoped>
/* ===== Hero ===== */
.dh-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

/* ===== Stats strip ===== */
.dh-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
.dh-stat-body {
  display: flex;
  align-items: center;
  gap: 12px;
}
.dh-stat-num {
  font-size: 1.3rem;
  font-weight: 800;
  line-height: 1;
  color: var(--text);
}

/* ===== Toolbar ===== */
.dh-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  margin-bottom: 14px;
}
.dh-filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dh-select {
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--card-bg-solid);
  color: var(--text);
  font-size: 0.85rem;
  cursor: pointer;
  outline: none;
  transition: border-color var(--transition);
}
.dh-select:focus { border-color: var(--info); }
.dh-total-hint {
  font-size: 0.82rem;
  color: var(--text-muted);
}

/* ===== Table ===== */
.dh-table th {
  font-size: 0.78rem;
  color: var(--text-muted);
  font-weight: 600;
  white-space: nowrap;
}
.dh-table th i { margin-right: 4px; }
.dh-row { transition: background var(--transition); }
.dh-row:hover { background: var(--bg-alt); }
.dh-date { font-size: 0.82rem; color: var(--text-muted); white-space: nowrap; }
.dh-module-tag {
  font-size: 0.8rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: var(--info-light);
  color: var(--info);
}
.dh-accuracy { font-size: 0.9rem; font-weight: 700; }
.dh-accuracy.acc-high { color: var(--success); }
.dh-accuracy.acc-mid  { color: var(--accent); }
.dh-accuracy.acc-low  { color: var(--danger); }
.dh-count { font-size: 0.85rem; color: var(--text-secondary); }
.dh-detail-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color var(--transition);
  white-space: nowrap;
}
.dh-detail-btn:hover { color: var(--info); }

/* ===== Empty ===== */
.dh-empty {
  padding: 60px 40px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

/* ===== Pagination ===== */
.dh-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding-bottom: 40px;
}
.dh-page-info {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .dh-stats { grid-template-columns: repeat(3, 1fr); }
  .dh-hero { flex-direction: column; align-items: flex-start; }
  .dh-table { font-size: 0.82rem; }
}
@media (max-width: 560px) {
  .dh-stats { grid-template-columns: 1fr; }
}
</style>
