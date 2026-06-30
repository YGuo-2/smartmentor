import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')

function assert(condition, message) {
  if (!condition) {
    console.error('[smoke] FAIL:', message)
    process.exit(1)
  }
}

function read(relativePath) {
  const file = resolve(root, relativePath)
  assert(existsSync(file), `${relativePath} is missing`)
  return readFileSync(file, 'utf8')
}

const requiredFiles = [
  'src/main.js',
  'src/App.vue',
  'src/router/index.js',
  'src/api/index.js',
  'src/views/Diagnostic.vue',
  'src/views/LearningPaths.vue',
  'src/views/Lesson.vue',
  'src/views/Chat.vue'
]

for (const file of requiredFiles) {
  assert(existsSync(resolve(root, file)), `${file} is missing`)
}

const router = read('src/router/index.js')
for (const route of ['/diagnostic', '/learning', '/chat']) {
  assert(router.includes(route), `router missing ${route}`)
}

const api = read('src/api/index.js')
for (const endpoint of [
  '/diagnostic/start',
  '/tracing/analyze',
  '/learning/lesson/',
  '/learning/checkpoint/submit'
]) {
  assert(api.includes(endpoint), `api client missing ${endpoint}`)
}

const lesson = read('src/views/Lesson.vue')
assert(lesson.includes('submitCheckpoint'), 'Lesson.vue missing checkpoint submission flow')
assert(!lesson.includes('correctAnswer: exercise.correctAnswer'), 'checkpoint flow must not send client correctAnswer')

const legacyRouter = read('js/router.js')
const legacyLesson = read('js/pages/lesson.js')
const legacyLearningPaths = read('js/pages/learning-paths.js')
const legacyDiagnosticResult = read('js/pages/diagnostic-result.js')
const legacyDiagnostic = read('js/pages/diagnostic.js')
const legacyDiagnosticHistory = read('js/pages/diagnostic-history.js')
const legacyProfile = read('js/pages/profile.js')
const legacyAuth = read('js/pages/auth.js')
const legacyDashboard = read('js/pages/dashboard.js')
assert(legacyRouter.includes('/learning/:pathId'), 'legacy router missing learning path detail route')
assert(legacyRouter.includes('/tracing/:tracingId'), 'legacy router missing tracing result route')
assert(!legacyLesson.includes('`/path/${props.pathId}`'), 'legacy lesson links to removed /path route')
assert(!legacyLearningPaths.includes('/learning-paths'), 'legacy learning paths page links to removed /learning-paths route')
assert(!legacyDiagnosticResult.includes('#/history'), 'legacy diagnostic result links to removed #/history route')
assert(legacyDiagnostic.includes('optionLabel(opt, idx)'), 'legacy diagnostic must render backend option labels')
assert(legacyDiagnostic.includes('optionText(opt)'), 'legacy diagnostic must render backend option text')
assert(legacyDiagnostic.includes('const hasAnswer = computed'), 'legacy diagnostic must support non-choice answer input')
assert(!legacyDiagnostic.includes('optionLetters[feedback.correctAnswer]'), 'legacy diagnostic must not index correctAnswer as an array number')
assert(!legacyDiagnostic.includes('answer: selectedAnswer.value'), 'legacy diagnostic must submit normalized answer value')
assert(legacyDiagnosticHistory.includes('const currentPage = ref(0)'), 'legacy diagnostic history must use zero-based backend pagination')
assert(legacyDiagnosticHistory.includes('fetchHistory(0)'), 'legacy diagnostic history must load backend page 0 first')
assert(legacyDiagnosticHistory.includes('currentPage + 1'), 'legacy diagnostic history must display one-based page numbers')
assert(!legacyDiagnosticHistory.includes('functions'), 'legacy diagnostic history must not use removed high-school module keys')
assert(!legacyDiagnosticHistory.includes('filterDateStart'), 'legacy diagnostic history must not expose unsupported date filters')
assert(legacyProfile.includes('邮箱：'), 'legacy profile must display email instead of removed phone field')
assert(legacyProfile.includes('currentCourse'), 'legacy profile settings must expose current course')
assert(legacyProfile.includes(':key="node.id"'), 'legacy profile knowledge map must key nodes by backend id')
assert(!legacyProfile.includes('手机：'), 'legacy profile must not show phone label')
assert(!legacyProfile.includes('修改密码'), 'legacy profile must not expose unsupported password update UI')
assert(!legacyProfile.includes('oldPassword'), 'legacy profile must not send unsupported oldPassword')
assert(legacyAuth.includes('sendCaptcha'), 'legacy auth must use email captcha endpoint')
assert(legacyAuth.includes('setToken(data.token)'), 'legacy auth must store single JWT token')
for (const removedModule of ['函数', '导数', '三角函数', '向量', '数列', '解析几何']) {
  assert(!legacyDashboard.includes(`module: '${removedModule}'`), `legacy dashboard must not use removed module ${removedModule}`)
}
for (const moduleName of ['人工智能基础', 'Java Web 开发', '数字电路基础']) {
  assert(legacyDashboard.includes(`module: '${moduleName}'`), `legacy dashboard missing module ${moduleName}`)
}

const authResponse = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/dto/AuthResponse.java')
const currentUserResponse = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/dto/CurrentUserResponse.java')
const authService = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/service/AuthService.java')
const requestBodyUtils = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/common/RequestBodyUtils.java')
const diagnosticController = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/controller/DiagnosticController.java')
const tracingController = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/controller/TracingController.java')
const learningController = read('../SmartMentor-back/src/main/java/com/tricia/smartmentor/controller/LearningController.java')
assert(authResponse.includes('private String email;'), 'AuthResponse must expose email for login/register clients')
assert(currentUserResponse.includes('private String email;'), 'CurrentUserResponse must expose email for /auth/me clients')
assert(authService.includes('.email(student.getEmail())'), 'AuthService must populate email in auth responses')
assert(requestBodyUtils.includes('equalsIgnoreCase(text)'), 'RequestBodyUtils must normalize null/undefined text values')
assert(requestBodyUtils.includes('Double.isFinite(number)'), 'RequestBodyUtils must reject NaN and infinite numeric values')
assert(diagnosticController.includes('RequestBodyUtils.requiredLong(request, "questionId")'), 'DiagnosticController must parse string questionId safely')
assert(diagnosticController.includes('RequestBodyUtils.optionalDouble(request, "difficulty")'), 'DiagnosticController must reject invalid numeric difficulty as 400')
assert(tracingController.includes('RequestBodyUtils.optionalStringList(request, "knowledgePointIds")'), 'TracingController must parse knowledgePointIds defensively')
assert(learningController.includes('RequestBodyUtils.requiredLong(request, "pathId")'), 'LearningController must parse string pathId safely')
assert(learningController.includes('RequestBodyUtils.requiredObjectList(request, "answers")'), 'LearningController must validate checkpoint answers as object list')
assert(learningController.includes('RequestBodyUtils.optionalInteger(request, "totalTimeSeconds")'), 'LearningController must parse string totalTimeSeconds safely')

console.log('[smoke] OK: core routes, API methods, and checkpoint submission boundary are present')
