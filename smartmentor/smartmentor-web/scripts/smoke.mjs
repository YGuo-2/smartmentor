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
