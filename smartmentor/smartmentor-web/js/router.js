import { createRouter, createWebHashHistory } from 'vue-router';
import { getAccessToken } from './api.js';

import { LandingPage } from './pages/landing.js';
import { AuthPage } from './pages/auth.js';
import { DashboardPage } from './pages/dashboard.js';
import { DiagnosticPage } from './pages/diagnostic.js';
import { DiagnosticHistoryPage } from './pages/diagnostic-history.js';
import { DiagnosticResultPage } from './pages/diagnostic-result.js';
import { TracingResultPage } from './pages/tracing-result.js';
import { LearningPathsPage } from './pages/learning-paths.js';
import { LearningPathDetailPage } from './pages/learning-path-detail.js';
import { LessonPage } from './pages/lesson.js';
import { ChatPage } from './pages/chat.js';
import { ProfilePage } from './pages/profile.js';
import { ReportPage } from './pages/report.js';

const routes = [
  { path: '/', component: LandingPage, meta: { public: true } },
  { path: '/login', component: AuthPage, props: { mode: 'login' }, meta: { public: true } },
  { path: '/register', component: AuthPage, props: { mode: 'register' }, meta: { public: true } },
  { path: '/dashboard', component: DashboardPage },
  { path: '/diagnostic', component: DiagnosticPage },
  { path: '/diagnostic/history', component: DiagnosticHistoryPage },
  { path: '/diagnostic/result/:diagnosticId', component: DiagnosticResultPage, props: true },
  { path: '/tracing/:tracingId', component: TracingResultPage, props: true },
  { path: '/learning', component: LearningPathsPage },
  { path: '/learning/:pathId', component: LearningPathDetailPage, props: true },
  { path: '/learning/:pathId/:nodeId', component: LessonPage, props: true },
  { path: '/chat', component: ChatPage },
  { path: '/profile', component: ProfilePage },
  { path: '/report', component: ReportPage },
];

export const router = createRouter({
  history: createWebHashHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  if (to.meta.public) return next();
  if (!getAccessToken()) return next('/login?redirect=' + encodeURIComponent(to.fullPath));
  next();
});
