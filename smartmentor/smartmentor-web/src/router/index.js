import { createRouter, createWebHashHistory } from 'vue-router';
import { getAccessToken } from '../api/index.js';
import { getUserRole, loadUser, roleHome, user } from '../composables/state.js';

const STUDENT_ROLES = ['student'];

const routes = [
  { path: '/', component: () => import('../views/Landing.vue'), meta: { public: true } },
  { path: '/login', component: () => import('../views/Auth.vue'), props: { mode: 'login' }, meta: { public: true, authPage: true } },
  { path: '/register', component: () => import('../views/Auth.vue'), props: { mode: 'register' }, meta: { public: true, authPage: true } },
  { path: '/dashboard', component: () => import('../views/Dashboard.vue'), meta: { roles: STUDENT_ROLES } },
  { path: '/diagnostic', component: () => import('../views/Diagnostic.vue'), meta: { roles: STUDENT_ROLES } },
  { path: '/diagnostic/history', component: () => import('../views/DiagnosticHistory.vue'), meta: { roles: STUDENT_ROLES } },
  { path: '/diagnostic/result/:diagnosticId', component: () => import('../views/DiagnosticResult.vue'), props: true, meta: { roles: STUDENT_ROLES } },
  { path: '/tracing/:tracingId', component: () => import('../views/TracingResult.vue'), props: true, meta: { roles: STUDENT_ROLES } },
  { path: '/learning', component: () => import('../views/LearningPaths.vue'), meta: { roles: STUDENT_ROLES } },
  { path: '/learning/:pathId', component: () => import('../views/LearningPathDetail.vue'), props: true, meta: { roles: STUDENT_ROLES } },
  { path: '/learning/:pathId/:nodeId', component: () => import('../views/Lesson.vue'), props: true, meta: { roles: STUDENT_ROLES } },
  { path: '/chat', component: () => import('../views/Chat.vue') },
  { path: '/onboarding', component: () => import('../views/ProfileOnboarding.vue'), meta: { roles: STUDENT_ROLES } },
  { path: '/profile', component: () => import('../views/Profile.vue') },
  { path: '/report', component: () => import('../views/Report.vue'), meta: { roles: STUDENT_ROLES } },
];

export const router = createRouter({
  history: createWebHashHistory(),
  routes
});

router.beforeEach(async (to, from, next) => {
  const token = getAccessToken();

  if (to.meta.public) {
    if (to.meta.authPage && token) {
      const currentUser = user.value || await loadUser();
      if (currentUser) return next(roleHome(getUserRole(currentUser)));
    }
    return next();
  }

  if (!token) return next('/login?redirect=' + encodeURIComponent(to.fullPath));

  const currentUser = user.value || await loadUser();
  if (!currentUser) return next('/login?redirect=' + encodeURIComponent(to.fullPath));

  const role = getUserRole(currentUser) || 'student';
  const allowedRoles = to.meta.roles;
  if (Array.isArray(allowedRoles) && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    return next(roleHome(role));
  }

  next();
});
