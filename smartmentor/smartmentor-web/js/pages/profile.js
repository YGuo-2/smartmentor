import { ref, onMounted } from 'vue';
import { api } from '../api.js';
import { user, logout, showToast } from '../state.js';

export const ProfilePage = {
  template: `
<div class="profile-page">
  <nav class="topnav">
    <div class="topnav-brand">SmartMentor</div>
    <ul class="topnav-links">
      <li><router-link to="/dashboard">仪表盘</router-link></li>
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
        <router-link to="/profile" class="active" @click="showDropdown = false">个人设置</router-link>
        <a href="#" @click.prevent="handleLogout">退出登录</a>
      </div>
    </div>
  </nav>

  <div class="profile-container" style="max-width:960px;margin:0 auto;padding:24px 16px;">
    <!-- Profile Card -->
    <div class="card profile-card" style="margin-bottom:24px;">
      <div class="card-body" style="display:flex;align-items:center;gap:24px;flex-wrap:wrap;">
        <div class="profile-avatar">{{ initials }}</div>
        <div class="profile-info">
          <h2 style="margin:0 0 8px 0;">{{ profile.nickname || '未设置昵称' }}</h2>
          <p style="margin:4px 0;color:#666;">邮箱：{{ profile.email || '未绑定' }}</p>
          <p style="margin:4px 0;color:#666;">角色：{{ profile.role === 'student' ? '学生' : profile.role || '未知' }}</p>
          <p style="margin:4px 0;color:#666;">学历层次：{{ profile.educationLevel || profile.grade || '未设置' }}</p>
          <p style="margin:4px 0;color:#666;">当前课程：{{ profile.currentCourse || '未设置' }}</p>
          <p style="margin:4px 0;color:#666;">注册时间：{{ formatDate(profile.createdAt) }}</p>
        </div>
        <div style="margin-left:auto;text-align:center;">
          <div style="display:flex;gap:24px;">
            <div>
              <div style="font-size:24px;font-weight:700;color:#1a1a2e;">{{ profile.stats.totalStudyDays }}</div>
              <div style="font-size:12px;color:#888;">学习天数</div>
            </div>
            <div>
              <div style="font-size:24px;font-weight:700;color:#1a1a2e;">{{ profile.stats.totalQuestions }}</div>
              <div style="font-size:12px;color:#888;">答题总数</div>
            </div>
            <div>
              <div style="font-size:24px;font-weight:700;color:#1a1a2e;">{{ (profile.stats.averageMastery * 100).toFixed(0) }}%</div>
              <div style="font-size:12px;color:#888;">平均掌握度</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Five-dimension Profile -->
    <div class="card" style="margin-bottom:24px;">
      <div class="card-header"><h3>五维学习画像</h3></div>
      <div class="card-body">
        <div class="dimension-list">
          <div class="dimension-item" v-for="dim in dimensions" :key="dim.key">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
              <span style="font-size:14px;font-weight:500;">{{ dim.label }}</span>
              <span style="font-size:13px;color:#666;">{{ (dim.value * 100).toFixed(0) }}分</span>
            </div>
            <div class="dimension-bar">
              <div class="dimension-bar-fill" :style="{ width: (dim.value * 100) + '%', background: dim.color }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Knowledge Map Section -->
    <div class="card" style="margin-bottom:24px;">
      <div class="card-header"><h3>知识图谱</h3></div>
      <div class="card-body">
        <div class="subnav" style="margin-bottom:16px;">
          <span
            class="subnav-item"
            v-for="mod in modules"
            :key="mod"
            :class="{ active: selectedModule === mod }"
            @click="selectModule(mod)"
          >{{ mod }}</span>
        </div>
        <div class="knowledge-map-container">
          <div v-if="knowledgeLoading" style="text-align:center;padding:24px;color:#888;">加载中...</div>
          <div v-else-if="knowledgeNodes.length === 0" style="text-align:center;padding:24px;color:#888;">暂无知识节点数据</div>
          <div v-else style="display:flex;flex-wrap:wrap;gap:12px;">
            <div
              class="knowledge-node"
              :class="getNodeClass(node)"
              v-for="node in knowledgeNodes"
              :key="node.id"
            >
              <div style="font-size:14px;font-weight:500;margin-bottom:4px;">{{ node.name }}</div>
              <div style="font-size:12px;color:inherit;opacity:0.8;">
                掌握度：{{ (node.mastery * 100).toFixed(0) }}%
              </div>
              <div style="font-size:11px;margin-top:4px;opacity:0.7;">
                {{ getNodeStatusLabel(node) }}
              </div>
            </div>
          </div>
          <div style="margin-top:16px;display:flex;gap:16px;flex-wrap:wrap;">
            <span style="font-size:12px;display:flex;align-items:center;gap:4px;">
              <span style="width:12px;height:12px;border-radius:3px;background:#4caf50;display:inline-block;"></span> 已掌握 (>70%)
            </span>
            <span style="font-size:12px;display:flex;align-items:center;gap:4px;">
              <span style="width:12px;height:12px;border-radius:3px;background:#ff9800;display:inline-block;"></span> 学习中 (40%-70%)
            </span>
            <span style="font-size:12px;display:flex;align-items:center;gap:4px;">
              <span style="width:12px;height:12px;border-radius:3px;background:#f44336;display:inline-block;"></span> 薄弱 (<40%)
            </span>
            <span style="font-size:12px;display:flex;align-items:center;gap:4px;">
              <span style="width:12px;height:12px;border-radius:3px;background:#bdbdbd;display:inline-block;"></span> 未开始
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Settings Section -->
    <div class="card" style="margin-bottom:24px;">
      <div class="card-header"><h3>个人设置</h3></div>
      <div class="card-body">
        <div class="form-group">
          <label class="form-label">昵称</label>
          <input class="form-input" v-model="settingsForm.nickname" placeholder="请输入昵称" />
        </div>
        <div class="form-group">
          <label class="form-label">专业方向</label>
          <select class="form-input" v-model="settingsForm.majorDirection" aria-label="专业方向" title="专业方向">
            <option value="">请选择专业方向</option>
            <option value="计算机类">计算机类</option>
            <option value="软件工程">软件工程</option>
            <option value="电子信息">电子信息</option>
            <option value="自动化">自动化</option>
            <option value="数据科学">数据科学</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">学历层次</label>
          <select class="form-input" v-model="settingsForm.educationLevel" aria-label="学历层次" title="学历层次">
            <option value="">请选择学历层次</option>
            <option value="高职">高职</option>
            <option value="本科">本科</option>
            <option value="研究生">研究生</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">当前课程</label>
          <select class="form-input" v-model="settingsForm.currentCourse" aria-label="当前课程" title="当前课程">
            <option value="">请选择当前课程</option>
            <option v-for="mod in modules" :key="mod" :value="mod">{{ mod }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">学习目标</label>
          <select class="form-input" v-model="settingsForm.learningGoal" aria-label="学习目标" title="学习目标">
            <option value="">请选择学习目标</option>
            <option value="考试复习">考试复习</option>
            <option value="项目实践">项目实践</option>
            <option value="科研入门">科研入门</option>
            <option value="就业技能">就业技能</option>
            <option value="竞赛提升">竞赛提升</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">基础水平</label>
          <select class="form-input" v-model="settingsForm.foundationLevel" aria-label="基础水平" title="基础水平">
            <option value="">请选择基础水平</option>
            <option value="入门">入门</option>
            <option value="基础">基础</option>
            <option value="进阶">进阶</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">兴趣方向</label>
          <input class="form-input" v-model="settingsForm.academicInterest" placeholder="如 大模型应用、Web 后端" />
        </div>
        <div style="margin-top:20px;">
          <button class="btn btn-dark" @click="saveSettings" :disabled="saving">
            {{ saving ? '保存中...' : '保存设置' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</div>
  `,
  setup() {
    const showDropdown = ref(false);

    const profile = ref({
      nickname: '',
      email: '',
      role: '',
      grade: '',
      educationLevel: '',
      currentCourse: '',
      createdAt: '',
      stats: {
        totalStudyDays: 0,
        totalQuestions: 0,
        averageMastery: 0
      }
    });

    const dimensions = ref([
      { key: 'knowledgeState', label: '知识状态', value: 0, color: '#4caf50' },
      { key: 'errorPattern', label: '错误模式', value: 0, color: '#f44336' },
      { key: 'learningBehavior', label: '学习行为', value: 0, color: '#2196f3' },
      { key: 'cognitiveStyle', label: '认知风格', value: 0, color: '#9c27b0' },
      { key: 'goalProfile', label: '目标画像', value: 0, color: '#ff9800' }
    ]);

    const modules = ['人工智能基础', 'Java Web 开发', '数字电路基础'];
    const selectedModule = ref('人工智能基础');
    const knowledgeNodes = ref([]);
    const knowledgeLoading = ref(false);

    const settingsForm = ref({
      nickname: '',
      majorDirection: '',
      educationLevel: '',
      currentCourse: '',
      learningGoal: '',
      foundationLevel: '',
      academicInterest: ''
    });
    const saving = ref(false);

    const initials = ref('');

    function formatDate(dateStr) {
      if (!dateStr) return '未知';
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
    }

    function getNodeClass(node) {
      if (node.status === 'not_started' || node.mastery === 0) return 'not-started';
      if (node.mastery > 0.7) return 'mastered';
      if (node.mastery >= 0.4) return 'learning';
      return 'weak';
    }

    function getNodeStatusLabel(node) {
      if (node.status === 'not_started' || node.mastery === 0) return '未开始';
      if (node.mastery > 0.7) return '已掌握';
      if (node.mastery >= 0.4) return '学习中';
      return '薄弱';
    }

    async function loadProfile() {
      try {
        const [currentUser, data] = await Promise.all([
          api.auth.me().catch(() => ({})),
          api.profile.overview()
        ]);
        const summary = currentUser.profile || data.dimensions?.goalProfile || {};
        profile.value.nickname = currentUser.nickname || '';
        profile.value.email = currentUser.email || '';
        profile.value.role = currentUser.role || '';
        profile.value.grade = currentUser.grade || '';
        profile.value.educationLevel = summary.educationLevel || currentUser.grade || '';
        profile.value.currentCourse = summary.currentCourse || '';
        profile.value.createdAt = currentUser.createdAt || '';
        profile.value.stats = {
          totalStudyDays: data.streakDays || 0,
          totalQuestions: data.dimensions?.errorPatterns?.totalErrors || 0,
          averageMastery: Number(data.overallMastery || 0)
        };

        // Update initials
        const name = currentUser.nickname || currentUser.username || '用户';
        initials.value = name.substring(0, 2);

        // Update dimensions
        if (data.dimensions) {
          dimensions.value[0].value = normalizeScore(data.dimensions.knowledgeState?.score);
          dimensions.value[1].value = normalizeScore(data.dimensions.errorPatterns?.score);
          dimensions.value[2].value = normalizeScore(data.dimensions.learningBehavior?.score);
          dimensions.value[3].value = normalizeScore(data.dimensions.cognitiveStyle?.score);
          dimensions.value[4].value = normalizeScore(data.dimensions.goalProfile?.score);
        }

        // Pre-fill settings form
        settingsForm.value.nickname = currentUser.nickname || '';
        settingsForm.value.majorDirection = summary.majorDirection || '';
        settingsForm.value.educationLevel = summary.educationLevel || currentUser.grade || '';
        settingsForm.value.currentCourse = summary.currentCourse || '';
        settingsForm.value.learningGoal = summary.learningGoal || '';
        settingsForm.value.foundationLevel = summary.foundationLevel || '';
        settingsForm.value.academicInterest = summary.academicInterest || '';
      } catch (e) {
        console.error('Failed to load profile:', e);
        showToast('加载个人信息失败', 'error');
      }
    }

    async function loadKnowledgeMap(module) {
      knowledgeLoading.value = true;
      try {
        const data = await api.profile.knowledgeMap({ module });
        knowledgeNodes.value = data.nodes || [];
      } catch (e) {
        console.error('Failed to load knowledge map:', e);
        knowledgeNodes.value = [];
      } finally {
        knowledgeLoading.value = false;
      }
    }

    function selectModule(mod) {
      selectedModule.value = mod;
      loadKnowledgeMap(mod);
    }

    function normalizeScore(value) {
      const numeric = Number(value || 0);
      return numeric > 1 ? numeric / 100 : numeric;
    }

    async function saveSettings() {
      saving.value = true;
      try {
        const body = {};
        if (settingsForm.value.nickname) body.nickname = settingsForm.value.nickname;
        if (settingsForm.value.majorDirection) body.majorDirection = settingsForm.value.majorDirection;
        if (settingsForm.value.educationLevel) body.educationLevel = settingsForm.value.educationLevel;
        if (settingsForm.value.currentCourse) body.currentCourse = settingsForm.value.currentCourse;
        if (settingsForm.value.learningGoal) body.learningGoal = settingsForm.value.learningGoal;
        if (settingsForm.value.foundationLevel) body.foundationLevel = settingsForm.value.foundationLevel;
        if (settingsForm.value.academicInterest) body.academicInterest = settingsForm.value.academicInterest;

        await api.profile.updateSettings(body);
        showToast('设置已保存', 'success');

        // Reload profile
        await loadProfile();
      } catch (e) {
        showToast(e.message || '保存失败', 'error');
      } finally {
        saving.value = false;
      }
    }

    function handleLogout() {
      showDropdown.value = false;
      logout();
    }

    onMounted(async () => {
      await loadProfile();
      await loadKnowledgeMap(selectedModule.value);
    });

    return {
      user,
      showDropdown,
      profile,
      initials,
      dimensions,
      modules,
      selectedModule,
      knowledgeNodes,
      knowledgeLoading,
      settingsForm,
      saving,
      formatDate,
      getNodeClass,
      getNodeStatusLabel,
      selectModule,
      saveSettings,
      handleLogout
    };
  }
};
