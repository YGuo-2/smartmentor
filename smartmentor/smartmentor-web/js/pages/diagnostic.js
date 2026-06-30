import { computed, ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api.js';
import { renderLatex, showToast } from '../state.js';

export const DiagnosticPage = {
  template: `
<div>
  <nav class="topnav">
    <div class="topnav-logo">SmartMentor</div>
    <div class="topnav-links">
      <router-link to="/dashboard" class="topnav-link">仪表盘</router-link>
      <router-link to="/diagnostic" class="topnav-link active">诊断测试</router-link>
      <router-link to="/learning" class="topnav-link">学习路径</router-link>
      <router-link to="/chat" class="topnav-link">AI 对话</router-link>
      <router-link to="/report" class="topnav-link">学习报告</router-link>
      <router-link to="/profile" class="topnav-link">个人中心</router-link>
    </div>
  </nav>

  <div class="main-content">
    <div class="diagnostic-container">

      <!-- Start Screen: Module Selection -->
      <div v-if="!started">
        <h2 style="margin-bottom:8px">自适应诊断测试</h2>
        <p style="color:var(--text-secondary);margin-bottom:32px">选择一个课程模块，系统将通过 5-8 道自适应题目精准诊断你的知识掌握情况。</p>

        <div class="module-grid">
          <div
            v-for="m in modules"
            :key="m.key"
            class="module-card"
            :class="{ selected: selectedModule === m.key }"
            @click="selectedModule = m.key"
          >
            <div class="module-icon">{{ m.icon }}</div>
            <div class="module-name">{{ m.name }}</div>
          </div>
        </div>

        <div style="text-align:center;margin-top:36px">
          <button class="btn btn-dark btn-lg" :disabled="!selectedModule || loading" @click="startDiagnostic">
            <span v-if="loading" class="spinner" style="width:16px;height:16px"></span>
            <span v-else>开始诊断</span>
          </button>
        </div>
      </div>

      <!-- Test In Progress -->
      <div v-else>
        <!-- Progress Indicator -->
        <div class="question-header">
          <span class="q-progress">第 {{ currentQuestion.questionNumber }} 题 / 约 5-8 题</span>
          <span class="badge" :class="difficultyBadgeClass">{{ difficultyLabel }}</span>
        </div>

        <!-- Question Card -->
        <div class="question-card">
          <div class="question-content" ref="questionContentRef">{{ currentQuestion.content }}</div>

          <div class="option-list" v-if="currentQuestion.options && currentQuestion.options.length">
            <div
              v-for="(opt, idx) in currentQuestion.options"
              :key="optionLabel(opt, idx)"
              class="option-item"
              :class="optionClass(optionLabel(opt, idx))"
              @click="selectOption(optionLabel(opt, idx))"
            >
              <span class="option-label">{{ optionLabel(opt, idx) }}.</span>
              <span class="option-text">{{ optionText(opt) }}</span>
            </div>
          </div>

          <div v-else class="form-group" style="margin-top:20px">
            <textarea
              v-if="currentQuestion.type === 'subjective'"
              class="form-input"
              v-model="fillAnswer"
              rows="5"
              placeholder="请写出你的解答"
              :disabled="!!feedback"
            ></textarea>
            <input
              v-else
              class="form-input"
              v-model="fillAnswer"
              placeholder="请输入答案"
              :disabled="!!feedback"
            />
          </div>
        </div>

        <!-- Submit Button -->
        <div style="text-align:center">
          <button
            class="btn btn-dark"
            :disabled="!hasAnswer || submitting"
            @click="submitAnswer"
          >
            <span v-if="submitting" class="spinner" style="width:16px;height:16px"></span>
            <span v-else>提交答案</span>
          </button>
        </div>

        <!-- Feedback -->
        <div v-if="feedback" class="feedback-box" :class="feedback.correct ? 'feedback-correct' : 'feedback-wrong'">
          <div class="feedback-title">{{ feedback.correct ? '回答正确!' : '回答错误' }}</div>
          <div v-if="!feedback.correct && feedback.correctAnswer" class="feedback-answer">正确答案: {{ feedback.correctAnswer }}</div>
          <div class="feedback-explanation">{{ feedback.explanation }}</div>
        </div>
      </div>

    </div>
  </div>
</div>
  `,
  setup() {
    const router = useRouter();

    const modules = [
      { key: '人工智能基础', name: '人工智能基础', icon: 'AI' },
      { key: 'Java Web 开发', name: 'Java Web 开发', icon: 'API' },
      { key: '数字电路基础', name: '数字电路基础', icon: 'DC' }
    ];

    const optionLetters = ['A', 'B', 'C', 'D'];

    const selectedModule = ref(null);
    const started = ref(false);
    const loading = ref(false);
    const submitting = ref(false);

    const diagnosticId = ref(null);
    const currentQuestion = ref(null);
    const selectedAnswer = ref(null);
    const fillAnswer = ref('');
    const feedback = ref(null);
    const questionStartTime = ref(null);

    const questionContentRef = ref(null);

    const difficultyMap = {
      easy: '简单',
      medium: '中等',
      hard: '困难'
    };

    const difficultyLabel = ref('');
    const difficultyBadgeClass = ref('badge-info');

    const hasAnswer = computed(() => {
      if (feedback.value || !currentQuestion.value) return false;
      if (currentQuestion.value.options && currentQuestion.value.options.length) {
        return selectedAnswer.value !== null;
      }
      return fillAnswer.value.trim() !== '';
    });

    function updateDifficulty(difficulty) {
      const numericDifficulty = typeof difficulty === 'number' ? difficulty : Number.parseFloat(difficulty);
      if (!Number.isNaN(numericDifficulty)) {
        if (numericDifficulty <= 0.4) {
          difficultyLabel.value = '简单';
          difficultyBadgeClass.value = 'badge-success';
        } else if (numericDifficulty <= 0.7) {
          difficultyLabel.value = '中等';
          difficultyBadgeClass.value = 'badge-accent';
        } else {
          difficultyLabel.value = '困难';
          difficultyBadgeClass.value = 'badge-danger';
        }
        return;
      }

      difficultyLabel.value = difficultyMap[difficulty] || difficulty || '中等';
      if (difficulty === 'easy') {
        difficultyBadgeClass.value = 'badge-success';
      } else if (difficulty === 'medium') {
        difficultyBadgeClass.value = 'badge-accent';
      } else {
        difficultyBadgeClass.value = 'badge-danger';
      }
    }

    async function startDiagnostic() {
      if (!selectedModule.value) return;
      loading.value = true;
      try {
        const res = await api.diagnostic.start({ module: selectedModule.value });
        diagnosticId.value = res.diagnosticId;
        currentQuestion.value = res.question;
        updateDifficulty(res.question.difficulty);
        started.value = true;
        selectedAnswer.value = null;
        fillAnswer.value = '';
        feedback.value = null;
        questionStartTime.value = Date.now();
        await nextTick();
        if (questionContentRef.value) {
          renderLatex(questionContentRef.value);
        }
      } catch (e) {
        showToast(e.message || '启动诊断失败', 'error');
      } finally {
        loading.value = false;
      }
    }

    function optionLabel(option, idx) {
      if (option && typeof option === 'object' && option.label) {
        return String(option.label);
      }
      return optionLetters[idx] || String(idx + 1);
    }

    function optionText(option) {
      if (option && typeof option === 'object') {
        return option.text || option.content || option.value || '';
      }
      return option;
    }

    function selectOption(label) {
      if (feedback.value) return;
      selectedAnswer.value = label;
    }

    function optionClass(label) {
      if (feedback.value) {
        if (label === feedback.value.correctAnswer) return 'correct';
        if (label === selectedAnswer.value && !feedback.value.correct) return 'wrong';
        return '';
      }
      if (label === selectedAnswer.value) return 'selected';
      return '';
    }

    async function submitAnswer() {
      if (!hasAnswer.value || submitting.value) return;
      submitting.value = true;

      const timeSpent = Math.round((Date.now() - questionStartTime.value) / 1000);
      const answer = currentQuestion.value.options && currentQuestion.value.options.length
        ? selectedAnswer.value
        : fillAnswer.value.trim();

      try {
        const res = await api.diagnostic.submit({
          diagnosticId: diagnosticId.value,
          questionId: currentQuestion.value.questionId,
          answer,
          timeSpent
        });

        feedback.value = {
          correct: res.result?.isCorrect ?? res.correct,
          correctAnswer: res.result?.correctAnswer ?? res.correctAnswer,
          explanation: res.result?.solution ?? res.explanation
        };

        if (res.isFinished || res.isComplete) {
          // Test complete, finish and redirect
          setTimeout(async () => {
            try {
              await api.diagnostic.finish(diagnosticId.value);
            } catch (e) {
              // ignore finish errors
            }
            router.push('/diagnostic/result/' + diagnosticId.value);
          }, 1500);
        } else if (res.nextQuestion) {
          // Load next question after brief delay
          setTimeout(async () => {
            currentQuestion.value = res.nextQuestion;
            updateDifficulty(res.nextQuestion.difficulty);
            selectedAnswer.value = null;
            fillAnswer.value = '';
            feedback.value = null;
            questionStartTime.value = Date.now();
            await nextTick();
            if (questionContentRef.value) {
              renderLatex(questionContentRef.value);
            }
          }, 1500);
        }
      } catch (e) {
        showToast(e.message || '提交答案失败', 'error');
      } finally {
        submitting.value = false;
      }
    }

    return {
      modules,
      optionLetters,
      selectedModule,
      started,
      loading,
      submitting,
      sessionId: diagnosticId,
      currentQuestion,
      selectedAnswer,
      fillAnswer,
      feedback,
      questionContentRef,
      difficultyLabel,
      difficultyBadgeClass,
      hasAnswer,
      startDiagnostic,
      optionLabel,
      optionText,
      selectOption,
      optionClass,
      submitAnswer
    };
  }
};
