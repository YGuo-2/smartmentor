# SmartMentor 接口文档合集

> 本文档由 SmartMentor/docs 目录下的接口文档合并生成。

## 2026-06-02 当前项目接口更新摘要

本节为当前代码状态的合并修订，覆盖 `SmartMentor` 后端与 `smartmentor-web` 前端最新联调结果。

- 认证采用 JWT，登录/注册均区分 `student` 与 `teacher` 角色。
- 学生端接口仅允许 `STUDENT`：`/api/diagnostic/**`、`/api/learning/**`、`/api/tracing/**`、`/api/report/**`、`/api/engagement/**`、`/api/homework/**`。
- 教师端接口仅允许 `TEACHER`：`/api/teacher/**`。
- 教师端已接入班级学生、知识热力图、知识点下钻、AI 分层作业、作业历史、学生预警、班级周报。
- 学生端已接入学习路径节点闭环：课程内容、练习提交、检查点评估、补救节点、下一节点跳转。
- 新增学生领取老师作业接口：`GET /api/homework`、`GET /api/homework/{homeworkId}`。
- 教师生成作业保存到 `teacher_homework`，学生端根据 `class_student` 班级归属与 `student_assignments` 分配信息读取自己的作业层级。

### 当前权限矩阵

| 角色 | 可访问页面 | 可访问 API |
|---|---|---|
| 学生 | 仪表盘、诊断测试、学习路径、我的作业、AI 对话、学习报告、个人设置 | `/api/diagnostic/**`、`/api/tracing/**`、`/api/learning/**`、`/api/homework/**`、`/api/report/**`、`/api/engagement/**`、通用 `/api/chat/**`、`/api/profile/**` |
| 教师 | 教师面板、AI 对话、个人设置 | `/api/teacher/**`、通用 `/api/chat/**`、`/api/profile/**` |

### 学生端 - 老师布置作业接口

#### 获取我的作业列表

- **路径**：`GET /api/homework`
- **认证**：学生 JWT
- **说明**：获取当前学生所在班级中、老师已经发布且分配给当前学生的作业。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | Integer | 否 | 页码，默认 `1` |
| pageSize | Integer | 否 | 每页数量，默认 `10` |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "homeworks": [
      {
        "homeworkId": "hw_20260602_001",
        "className": "高一1班",
        "knowledgePointIds": ["derivative-definition"],
        "knowledgePoints": ["导数的定义"],
        "assignedLayers": ["基础层"],
        "questionCount": 5,
        "status": "published",
        "deadline": "2026-06-05T23:59:59",
        "note": "重点完成导数概念题",
        "createdAt": "2026-06-02T10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 获取我的作业详情

- **路径**：`GET /api/homework/{homeworkId}`
- **认证**：学生 JWT
- **说明**：返回当前学生被分配到的层级和题目，不返回其他学生分配名单。

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "homeworkId": "hw_20260602_001",
    "className": "高一1班",
    "knowledgePoints": ["导数的定义"],
    "assignedLayers": ["基础层"],
    "questionCount": 5,
    "deadline": "2026-06-05T23:59:59",
    "layers": [
      {
        "layer": "基础层",
        "description": "概念巩固，注重基础计算和定义理解",
        "questions": [
          {
            "questionId": 501,
            "content": "求函数 f(x)=x^2 在 x=1 处的导数。",
            "questionType": "计算题",
            "difficulty": 0.3
          }
        ]
      }
    ]
  }
}
```

## 合并范围

- api-auth-profile.md
- api-diagnostic-tracing.md
- api-learning-chat.md
- api-teacher-report.md

---

<!-- Source: api-auth-profile.md -->

# SmartMentor 认证与学生画像模块 API 接口文档

## 概述

本文档定义了 SmartMentor 系统中认证（Auth）和学生画像（Profile）模块的 RESTful API 接口规范。

- **基础路径**：`/api`
- **认证方式**：JWT（JSON Web Token）
- **Token 传递**：通过请求头 `Authorization: Bearer {token}` 传递
- **内容类型**：`Content-Type: application/json`

---

## 统一响应格式

所有接口均采用以下统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段      | 类型            | 说明                      |
|---------|---------------|-------------------------|
| code    | Integer       | 状态码，200 表示成功，非 200 表示异常 |
| message | String        | 响应描述信息                  |
| data    | Object / null | 响应数据，失败时可为 null         |

### 通用错误码

| 状态码 | 说明               |
|-----|------------------|
| 200 | 请求成功             |
| 400 | 请求参数错误           |
| 401 | 未认证（Token 缺失或无效） |
| 403 | 权限不足             |
| 404 | 资源不存在            |
| 409 | 资源冲突（如用户名已存在）    |
| 500 | 服务器内部错误          |

---

## 一、认证模块（Auth）

### 1.1 学生注册

学生通过用户名和密码进行注册，注册成功后自动创建对应的学生画像记录。

- **路径**：`POST /api/auth/register`
- **认证**：无需认证
- **说明**：学生注册接口，注册成功后返回 JWT Token，可直接进入系统

#### 请求参数

| 参数       | 类型     | 必填 | 说明                        |
|----------|--------|----|---------------------------|
| username | String | 是  | 用户名，3-50 个字符，唯一           |
| password | String | 是  | 密码，6-128 个字符              |
| nickname | String | 否  | 昵称，1-50 个字符               |
| grade    | String | 是  | 年级，可选值：`高一` / `高二` / `高三` |
| school   | String | 否  | 学校名称，最大 100 个字符           |

#### 请求体示例

```json
{
  "username": "zhangsan2024",
  "password": "Abc123456",
  "nickname": "张三",
  "grade": "高二",
  "school": "北京市第一中学"
}
```

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1001,
    "username": "zhangsan2024",
    "nickname": "张三",
    "role": "student",
    "grade": "高二",
    "school": "北京市第一中学"
  }
}
```

**失败 - 用户名已存在（409）**：

```json
{
  "code": 409,
  "message": "用户名已存在",
  "data": null
}
```

**失败 - 参数校验失败（400）**：

```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": {
    "errors": [
      { "field": "username", "message": "用户名长度应为3-50个字符" },
      { "field": "password", "message": "密码长度不能少于6个字符" }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                     |
|-----|------------------------|
| 200 | 注册成功，返回 Token 和用户基本信息  |
| 400 | 请求参数校验失败（缺少必填字段或格式不正确） |
| 409 | 用户名已被占用                |
| 500 | 服务器内部错误                |

---

### 1.2 登录

学生和教师共用登录接口，通过 `role` 字段区分身份。登录成功返回 JWT Token。

- **路径**：`POST /api/auth/login`
- **认证**：无需认证
- **说明**：统一登录接口，支持学生和教师两种角色

#### 请求参数

| 参数       | 类型     | 必填 | 说明                           |
|----------|--------|----|------------------------------|
| username | String | 是  | 用户名                          |
| password | String | 是  | 密码                           |
| role     | String | 是  | 角色，可选值：`student` / `teacher` |

#### 请求体示例

```json
{
  "username": "zhangsan2024",
  "password": "Abc123456",
  "role": "student"
}
```

#### 响应体示例

**成功 - 学生登录（200）**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1001,
    "username": "zhangsan2024",
    "nickname": "张三",
    "role": "student",
    "grade": "高二",
    "school": "北京市第一中学",
    "avatarUrl": null
  }
}
```

**成功 - 教师登录（200）**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 2001,
    "username": "teacher_wang",
    "nickname": "王老师",
    "role": "teacher",
    "school": "北京市第一中学"
  }
}
```

**失败 - 用户名或密码错误（401）**：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

**失败 - 角色不匹配（400）**：

```json
{
  "code": 400,
  "message": "该用户不属于所选角色",
  "data": null
}
```

#### 状态码说明

| 状态码 | 说明                  |
|-----|---------------------|
| 200 | 登录成功，返回 Token 和用户信息 |
| 400 | 请求参数错误或角色不匹配        |
| 401 | 用户名或密码错误            |
| 500 | 服务器内部错误             |

---

### 1.3 获取当前用户信息

通过 JWT Token 获取当前登录用户的详细信息。

- **路径**：`GET /api/auth/me`
- **认证**：需要认证（Bearer Token）
- **说明**：返回当前已认证用户的完整信息，包含角色相关的扩展字段

#### 请求参数

无请求体。通过请求头传递 Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 请求体示例

无（GET 请求无请求体）

#### 响应体示例

**成功 - 学生用户（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "username": "zhangsan2024",
    "nickname": "张三",
    "role": "student",
    "grade": "高二",
    "school": "北京市第一中学",
    "avatarUrl": "https://cdn.smartmentor.com/avatars/1001.png",
    "createdAt": "2025-09-01T08:30:00",
    "profile": {
      "overallMastery": 0.45,
      "level": 3,
      "experiencePoints": 1250,
      "streakDays": 7,
      "totalStudyHours": 28.5,
      "lastDiagnosticAt": "2025-10-15T14:30:00"
    }
  }
}
```

**成功 - 教师用户（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 2001,
    "username": "teacher_wang",
    "nickname": "王老师",
    "role": "teacher",
    "school": "北京市第一中学",
    "createdAt": "2025-08-15T10:00:00",
    "classCount": 2,
    "studentCount": 45
  }
}
```

**失败 - Token 无效或过期（401）**：

```json
{
  "code": 401,
  "message": "Token 无效或已过期，请重新登录",
  "data": null
}
```

#### 状态码说明

| 状态码 | 说明              |
|-----|-----------------|
| 200 | 获取成功，返回用户详细信息   |
| 401 | Token 无效、过期或未提供 |
| 500 | 服务器内部错误         |

---

## 二、学生画像模块（Profile）

> 以下接口均需要认证（Bearer Token），且仅限学生角色访问。

### 2.1 获取学生五维画像总览

获取当前学生的五维画像综合数据，包括知识状态、错误模式、学习行为、认知风格和目标画像。

- **路径**：`GET /api/profile/overview`
- **认证**：需要认证（Bearer Token，学生角色）
- **说明**：返回学生五维画像的完整数据，用于前端仪表盘展示（雷达图等）

#### 请求参数

无请求体。通过请求头传递 Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 请求体示例

无（GET 请求无请求体）

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "overallMastery": 0.45,
    "level": 3,
    "experiencePoints": 1250,
    "streakDays": 7,
    "totalStudyHours": 28.5,
    "dimensions": {
      "knowledgeState": {
        "label": "知识状态",
        "score": 0.45,
        "description": "基于 BKT 模型的知识点掌握概率",
        "moduleMastery": {
          "函数": 0.62,
          "导数": 0.35,
          "三角函数": 0.48,
          "向量": 0.55,
          "数列": 0.60,
          "解析几何": 0.30
        },
        "totalKnowledgePoints": 95,
        "masteredCount": 38,
        "learningCount": 32,
        "weakCount": 25
      },
      "errorPatterns": {
        "label": "错误模式",
        "score": 0.40,
        "description": "常犯错误类型的分布与频率",
        "patterns": [
          { "type": "计算性错误", "subType": "符号错误", "frequency": 0.35, "trend": "decreasing" },
          { "type": "概念性错误", "subType": "概念混淆", "frequency": 0.25, "trend": "stable" },
          { "type": "逻辑性错误", "subType": "分类讨论遗漏", "frequency": 0.20, "trend": "increasing" },
          { "type": "方法性错误", "subType": "方法选择不当", "frequency": 0.12, "trend": "stable" },
          { "type": "审题性错误", "subType": "条件遗漏", "frequency": 0.08, "trend": "decreasing" }
        ],
        "totalErrors": 156,
        "recentErrorRate": 0.42
      },
      "learningBehavior": {
        "label": "学习行为",
        "score": 0.65,
        "description": "学习时段偏好、专注时长与做题速度",
        "preferredTimeSlot": "evening",
        "avgDailyStudyMinutes": 45,
        "avgFocusDuration": 25,
        "avgQuestionTime": 180,
        "weeklyActiveDays": 5,
        "studyPattern": {
          "morning": 0.10,
          "afternoon": 0.25,
          "evening": 0.65
        }
      },
      "cognitiveStyle": {
        "label": "认知风格",
        "score": 0.70,
        "description": "偏好的学习内容呈现方式",
        "primaryStyle": "visual",
        "styleDistribution": {
          "visual": 0.45,
          "logical": 0.25,
          "example": 0.20,
          "formula": 0.10
        },
        "preferredContentTypes": ["图表讲解", "动态演示", "实例分析"]
      },
      "goalProfile": {
        "label": "目标画像",
        "score": 0.50,
        "description": "目标院校与薄弱模块优先级设置",
        "targetSchool": "浙江大学",
        "targetScore": 130,
        "weakModulePriority": ["导数", "解析几何", "三角函数"],
        "examCountdown": 180,
        "studyMode": "systematic"
      }
    },
    "lastUpdatedAt": "2025-10-15T16:30:00"
  }
}
```

**失败 - 画像数据不存在（404）**：

```json
{
  "code": 404,
  "message": "学生画像数据不存在，请先完成诊断测试",
  "data": null
}
```

#### 状态码说明

| 状态码 | 说明                  |
|-----|---------------------|
| 200 | 获取成功，返回五维画像完整数据     |
| 401 | 未认证或 Token 无效       |
| 403 | 非学生角色无权访问           |
| 404 | 画像数据不存在（新用户未完成首次诊断） |
| 500 | 服务器内部错误             |

---

### 2.2 更新学生设置

更新学生的目标画像和学习偏好设置，包括目标学校、学习风格、学习时间等。

- **路径**：`PUT /api/profile/settings`
- **认证**：需要认证（Bearer Token，学生角色）
- **说明**：更新学生个性化设置，支持部分更新（只传需要修改的字段）

#### 请求参数

| 参数                 | 类型              | 必填 | 说明                                                      |
|--------------------|-----------------|----|---------------------------------------------------------|
| targetSchool       | String          | 否  | 目标学校名称，最大 100 个字符                                       |
| targetScore        | Integer         | 否  | 目标数学分数（满分 150）                                          |
| learningStyle      | String          | 否  | 学习风格偏好，可选值：`visual` / `logical` / `example` / `formula` |
| dailyStudyMinutes  | Integer         | 否  | 每日计划学习时长（分钟），范围 15-240                                  |
| preferredTimeSlot  | String          | 否  | 偏好学习时段，可选值：`morning` / `afternoon` / `evening`          |
| weakModulePriority | Array\<String\> | 否  | 薄弱模块优先级列表，如 `["导数", "解析几何"]`                            |
| studyMode          | String          | 否  | 学习模式，可选值：`systematic`（系统提升） / `urgent`（考前突击）            |
| nickname           | String          | 否  | 修改昵称，1-50 个字符                                           |
| avatarUrl          | String          | 否  | 头像 URL                                                  |

#### 请求体示例

```json
{
  "targetSchool": "浙江大学",
  "targetScore": 135,
  "learningStyle": "visual",
  "dailyStudyMinutes": 60,
  "preferredTimeSlot": "evening",
  "weakModulePriority": ["导数", "解析几何", "三角函数"],
  "studyMode": "systematic"
}
```

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "设置更新成功",
  "data": {
    "studentId": 1001,
    "targetSchool": "浙江大学",
    "targetScore": 135,
    "learningStyle": "visual",
    "dailyStudyMinutes": 60,
    "preferredTimeSlot": "evening",
    "weakModulePriority": ["导数", "解析几何", "三角函数"],
    "studyMode": "systematic",
    "updatedAt": "2025-10-15T17:00:00"
  }
}
```

**失败 - 参数校验失败（400）**：

```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": {
    "errors": [
      { "field": "dailyStudyMinutes", "message": "每日学习时长应在15-240分钟之间" },
      { "field": "learningStyle", "message": "学习风格仅支持 visual/logical/example/formula" }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                |
|-----|-------------------|
| 200 | 更新成功，返回更新后的完整设置信息 |
| 400 | 请求参数校验失败          |
| 401 | 未认证或 Token 无效     |
| 403 | 非学生角色无权访问         |
| 500 | 服务器内部错误           |

---

### 2.3 获取知识掌握度图谱数据

获取学生在各知识点上的掌握度数据，用于前端渲染交互式知识图谱（D3.js 力导向图）。

- **路径**：`GET /api/profile/knowledge-map`
- **认证**：需要认证（Bearer Token，学生角色）
- **说明**：返回知识点节点列表及其掌握度、节点间的前置依赖关系边，支持按模块筛选

#### 请求参数（Query Parameters）

| 参数     | 类型      | 必填 | 说明                                                              |
|--------|---------|----|-----------------------------------------------------------------|
| module | String  | 否  | 按模块筛选，可选值：`函数` / `导数` / `三角函数` / `向量` / `数列` / `解析几何`；不传则返回全部模块 |
| depth  | Integer | 否  | 图谱展示深度，默认 3，范围 1-5                                              |

请求示例：

```
GET /api/profile/knowledge-map?module=导数&depth=3
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 请求体示例

无（GET 请求无请求体）

#### 响应体示例

**成功（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "module": "导数",
    "totalNodes": 25,
    "masteredNodes": 8,
    "learningNodes": 10,
    "weakNodes": 7,
    "nodes": [
      {
        "id": "math_derivative_application",
        "name": "导数的应用",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.70,
        "importance": 0.90,
        "gaokaoFrequency": 0.85,
        "mastery": 0.35,
        "masteryLevel": "weak",
        "status": "learning",
        "lastPracticeAt": "2025-10-14T20:00:00",
        "errorCount": 12,
        "practiceCount": 18
      },
      {
        "id": "math_derivative_calc",
        "name": "导数的计算",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.50,
        "importance": 0.85,
        "gaokaoFrequency": 0.90,
        "mastery": 0.42,
        "masteryLevel": "weak",
        "status": "learning",
        "lastPracticeAt": "2025-10-13T19:30:00",
        "errorCount": 8,
        "practiceCount": 15
      },
      {
        "id": "math_derivative_concept",
        "name": "导数的概念与几何意义",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.45,
        "importance": 0.80,
        "gaokaoFrequency": 0.60,
        "mastery": 0.58,
        "masteryLevel": "learning",
        "status": "learning",
        "lastPracticeAt": "2025-10-12T21:00:00",
        "errorCount": 5,
        "practiceCount": 12
      },
      {
        "id": "math_rate_of_change",
        "name": "平均变化率与瞬时变化率",
        "module": "导数",
        "chapter": "第三章",
        "difficulty": 0.35,
        "importance": 0.70,
        "gaokaoFrequency": 0.40,
        "mastery": 0.25,
        "masteryLevel": "weak",
        "status": "root_cause",
        "lastPracticeAt": "2025-10-10T18:00:00",
        "errorCount": 6,
        "practiceCount": 8
      },
      {
        "id": "math_function_concept",
        "name": "函数的概念与表示法",
        "module": "函数",
        "chapter": "第二章",
        "difficulty": 0.30,
        "importance": 0.90,
        "gaokaoFrequency": 0.70,
        "mastery": 0.82,
        "masteryLevel": "mastered",
        "status": "completed",
        "lastPracticeAt": "2025-09-20T20:00:00",
        "errorCount": 2,
        "practiceCount": 20
      }
    ],
    "edges": [
      {
        "source": "math_function_concept",
        "target": "math_rate_of_change",
        "type": "prerequisite",
        "weight": 0.9,
        "crossModule": false
      },
      {
        "source": "math_rate_of_change",
        "target": "math_derivative_concept",
        "type": "prerequisite",
        "weight": 0.95,
        "crossModule": false
      },
      {
        "source": "math_derivative_concept",
        "target": "math_derivative_calc",
        "type": "prerequisite",
        "weight": 0.9,
        "crossModule": false
      },
      {
        "source": "math_derivative_calc",
        "target": "math_derivative_application",
        "type": "prerequisite",
        "weight": 0.9,
        "crossModule": false
      }
    ],
    "legend": {
      "masteryLevels": {
        "mastered": { "label": "已掌握", "color": "#52c41a", "threshold": "≥ 0.8" },
        "learning": { "label": "学习中", "color": "#faad14", "threshold": "0.5 - 0.8" },
        "weak": { "label": "薄弱", "color": "#ff4d4f", "threshold": "< 0.5" }
      }
    }
  }
}
```

**成功 - 无数据（200）**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "module": null,
    "totalNodes": 0,
    "masteredNodes": 0,
    "learningNodes": 0,
    "weakNodes": 0,
    "nodes": [],
    "edges": [],
    "legend": {
      "masteryLevels": {
        "mastered": { "label": "已掌握", "color": "#52c41a", "threshold": "≥ 0.8" },
        "learning": { "label": "学习中", "color": "#faad14", "threshold": "0.5 - 0.8" },
        "weak": { "label": "薄弱", "color": "#ff4d4f", "threshold": "< 0.5" }
      }
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明                    |
|-----|-----------------------|
| 200 | 获取成功，返回知识图谱节点和边数据     |
| 400 | 请求参数错误（如 module 值不合法） |
| 401 | 未认证或 Token 无效         |
| 403 | 非学生角色无权访问             |
| 500 | 服务器内部错误               |

---

## 三、数据模型参考

### 3.1 数据库表结构

以下为接口涉及的核心数据库表：

#### student 表（学生基本信息）

| 字段         | 类型           | 说明              |
|------------|--------------|-----------------|
| id         | BIGINT       | 主键，自增           |
| username   | VARCHAR(50)  | 用户名，唯一          |
| password   | VARCHAR(128) | 密码（BCrypt 加密存储） |
| nickname   | VARCHAR(50)  | 昵称              |
| grade      | VARCHAR(20)  | 年级：高一/高二/高三     |
| school     | VARCHAR(100) | 学校名称            |
| avatar_url | VARCHAR(255) | 头像 URL          |
| created_at | DATETIME     | 创建时间            |

#### student_profile 表（五维学生画像）

| 字段                   | 类型            | 说明                                  |
|----------------------|---------------|-------------------------------------|
| id                   | BIGINT        | 主键，自增                               |
| student_id           | BIGINT        | 关联学生 ID                             |
| learning_style       | VARCHAR(20)   | 认知风格：visual/logical/example/formula |
| daily_study_minutes  | INT           | 每日学习时长（分钟）                          |
| preferred_time_slot  | VARCHAR(20)   | 偏好时段：morning/afternoon/evening      |
| target_school        | VARCHAR(100)  | 目标学校                                |
| weak_module_priority | JSON          | 薄弱模块优先级列表                           |
| overall_mastery      | DECIMAL(3,2)  | 总体掌握度 0.00-1.00                     |
| ability_param        | DECIMAL(5,2)  | IRT 能力参数                            |
| error_patterns       | JSON          | 错误模式统计                              |
| knowledge_state      | JSON          | 各知识点掌握度 {kpId: mastery}             |
| streak_days          | INT           | 连续学习天数                              |
| total_study_hours    | DECIMAL(10,1) | 累计学习时长（小时）                          |
| level                | INT           | 等级                                  |
| experience_points    | INT           | 经验值                                 |
| last_diagnostic_at   | DATETIME      | 最近一次诊断时间                            |
| updated_at           | DATETIME      | 最后更新时间                              |

#### teacher 表（教师信息）

| 字段         | 类型           | 说明              |
|------------|--------------|-----------------|
| id         | BIGINT       | 主键，自增           |
| username   | VARCHAR(50)  | 用户名，唯一          |
| password   | VARCHAR(128) | 密码（BCrypt 加密存储） |
| name       | VARCHAR(50)  | 教师姓名            |
| school     | VARCHAR(100) | 学校名称            |
| created_at | DATETIME     | 创建时间            |

### 3.2 五维画像维度说明

| 维度       | 字段来源                                                                  | 数据采集方式          |
|----------|-----------------------------------------------------------------------|-----------------|
| **知识状态** | knowledge_state (JSON)、overall_mastery                                | 作答记录 + BKT 模型计算 |
| **错误模式** | error_patterns (JSON)                                                 | LLM 错因分析 + 分类统计 |
| **学习行为** | daily_study_minutes、preferred_time_slot、streak_days、total_study_hours | 行为日志自动采集        |
| **认知风格** | learning_style                                                        | 交互反馈 + 学生设置     |
| **目标画像** | target_school、weak_module_priority                                    | 学生手动设置          |

---

## 四、JWT Token 说明

### 4.1 Token 结构

JWT Token 的 Payload 中包含以下信息：

```json
{
  "sub": "1001",
  "username": "zhangsan2024",
  "role": "student",
  "iat": 1697356800,
  "exp": 1697443200
}
```

| 字段       | 说明                   |
|----------|----------------------|
| sub      | 用户 ID（字符串形式）         |
| username | 用户名                  |
| role     | 角色：student / teacher |
| iat      | Token 签发时间（Unix 时间戳） |
| exp      | Token 过期时间（Unix 时间戳） |

### 4.2 Token 有效期

- **Access Token**：24 小时
- **Token 刷新**：Token 过期后需重新登录获取新 Token

### 4.3 认证失败统一响应

当 Token 无效或过期时，所有需要认证的接口返回：

```json
{
  "code": 401,
  "message": "Token 无效或已过期，请重新登录",
  "data": null
}
```

---

## 五、接口调用示例

### 5.1 完整调用流程

```
1. 注册 → POST /api/auth/register → 获取 token
2. 登录 → POST /api/auth/login → 获取 token
3. 获取用户信息 → GET /api/auth/me (带 Authorization 头)
4. 获取画像总览 → GET /api/profile/overview (带 Authorization 头)
5. 更新设置 → PUT /api/profile/settings (带 Authorization 头)
6. 获取知识图谱 → GET /api/profile/knowledge-map?module=导数 (带 Authorization 头)
```

### 5.2 cURL 示例

**注册**：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan2024",
    "password": "Abc123456",
    "nickname": "张三",
    "grade": "高二",
    "school": "北京市第一中学"
  }'
```

**登录**：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan2024",
    "password": "Abc123456",
    "role": "student"
  }'
```

**获取当前用户信息**：

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**获取五维画像总览**：

```bash
curl -X GET http://localhost:8080/api/profile/overview \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**更新学生设置**：

```bash
curl -X PUT http://localhost:8080/api/profile/settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "targetSchool": "浙江大学",
    "dailyStudyMinutes": 60,
    "weakModulePriority": ["导数", "解析几何"]
  }'
```

**获取知识掌握度图谱（按模块筛选）**：

```bash
curl -X GET "http://localhost:8080/api/profile/knowledge-map?module=导数&depth=3" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

<!-- Source: api-diagnostic-tracing.md -->

# SmartMentor 智能诊断与溯源分析 接口文档

> 版本：v1.0
> 基础路径：`/api`
> 认证方式：JWT（所有接口均需在请求头携带 `Authorization: Bearer {token}`）
> 统一响应格式：`{ "code": int, "message": string, "data": object | null }`

---

## 目录

1. [POST /api/diagnostic/start — 启动诊断测试](#1-启动诊断测试)
2. [POST /api/diagnostic/submit — 提交答案](#2-提交答案)
3. [POST /api/diagnostic/finish — 结束诊断](#3-结束诊断)
4. [GET /api/diagnostic/history — 历史诊断记录列表](#4-历史诊断记录列表)
5. [GET /api/diagnostic/result/{diagnosticId} — 诊断详细结果](#5-诊断详细结果)
6. [POST /api/tracing/analyze — 溯源分析](#6-溯源分析)
7. [GET /api/tracing/result/{tracingId} — 溯源结果详情](#7-溯源结果详情)

---

## 公共说明

### 认证

所有接口均需要 JWT 认证，请在 HTTP 请求头中携带：

```
Authorization: Bearer {token}
```

Token 通过 `/api/auth/login` 接口获取，此处不再赘述。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段      | 类型     | 说明                         |
|---------|--------|----------------------------|
| code    | int    | 业务状态码，200 表示成功，其余见各接口状态码说明 |
| message | string | 状态描述信息                     |
| data    | object | 响应数据，失败时可能为 `null`         |

### 通用状态码

| 状态码 | 说明             |
|-----|----------------|
| 200 | 请求成功           |
| 400 | 请求参数错误         |
| 401 | 未认证或 Token 已过期 |
| 403 | 无权访问该资源        |
| 404 | 资源不存在          |
| 500 | 服务器内部错误        |

---

## 1. 启动诊断测试

启动一次新的自适应诊断测试。学生选择一个数学模块（如"导数""函数""三角函数"等），系统据此从知识图谱中选取关键知识点，生成第一道诊断题目。通常
5-8 道题即可完成一个模块的诊断。

- **路径**：`/api/diagnostic/start`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名        | 类型     | 必填 | 说明                                                                        |
|------------|--------|----|---------------------------------------------------------------------------|
| module     | string | 是  | 数学模块名称，可选值：`函数`、`导数`、`三角函数`、`向量`、`数列`、`解析几何`                              |
| difficulty | string | 否  | 初始难度偏好，可选值：`easy`(基础)、`medium`(中等，默认)、`hard`(较难)。系统会根据学生画像自动调整，此参数仅作为初始参考 |

### 请求体示例

```json
{
  "module": "导数",
  "difficulty": "medium"
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "module": "导数",
    "totalEstimatedQuestions": 6,
    "currentQuestionIndex": 1,
    "question": {
      "questionId": 1001,
      "knowledgePointId": "math_derivative_concept",
      "knowledgePointName": "导数的概念与几何意义",
      "type": "choice",
      "difficulty": 0.50,
      "content": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1)$ 的几何意义是（　　）",
      "options": [
        { "label": "A", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的切线斜率" },
        { "label": "B", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的纵坐标" },
        { "label": "C", "text": "函数 $f(x) = x^2$ 在 $x = 1$ 处的函数值" },
        { "label": "D", "text": "曲线 $y = x^2$ 过原点的割线斜率" }
      ],
      "timeLimit": 120
    }
  }
}
```

### 响应字段说明

| 字段                               | 类型     | 说明                                            |
|----------------------------------|--------|-----------------------------------------------|
| data.diagnosticId                | string | 本次诊断会话的唯一标识                                   |
| data.module                      | string | 诊断的数学模块                                       |
| data.totalEstimatedQuestions     | int    | 预计总题数（5-8 题，实际可能因自适应调整而变化）                    |
| data.currentQuestionIndex        | int    | 当前题目序号（从 1 开始）                                |
| data.question.questionId         | long   | 题目 ID                                         |
| data.question.knowledgePointId   | string | 所属知识点 ID                                      |
| data.question.knowledgePointName | string | 所属知识点名称                                       |
| data.question.type               | string | 题型：`choice`(选择题)、`fill`(填空题)、`solve`(解答题)     |
| data.question.difficulty         | float  | 难度系数（0.00-1.00，值越大越难）                         |
| data.question.content            | string | 题目内容（支持 LaTeX，使用 `$...$` 行内公式、`$$...$$` 块级公式） |
| data.question.options            | array  | 选项列表（仅选择题有此字段）                                |
| data.question.options[].label    | string | 选项标号（A/B/C/D）                                 |
| data.question.options[].text     | string | 选项内容（支持 LaTeX）                                |
| data.question.timeLimit          | int    | 建议作答时间（秒），仅供前端参考                              |

### 状态码说明

| 状态码 | 说明                      |
|-----|-------------------------|
| 200 | 诊断启动成功，返回第一道题目          |
| 400 | 参数错误（如 module 不在可选值范围内） |
| 401 | 未认证或 Token 已过期          |
| 409 | 存在未完成的诊断会话，请先完成或结束上一次诊断 |

---

## 2. 提交答案

提交当前诊断题目的答案。系统将：

1. 判断答案是否正确
2. 调用 LLM 进行错因分析（错误时返回错误类型与错因详细文本）
3. 根据自适应机制动态调整下一题的难度和知识点：
    - **答对** → 难度上调，跳过同级知识点，向更深层/更难的知识点探测
    - **答错** → 难度下调，深入当前知识点的子节点，进一步诊断薄弱环节
4. 返回下一道题目（若诊断尚未结束）

- **路径**：`/api/diagnostic/submit`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名          | 类型     | 必填 | 说明                                                    |
|--------------|--------|----|-------------------------------------------------------|
| diagnosticId | string | 是  | 诊断会话 ID（由 `/diagnostic/start` 返回）                     |
| questionId   | long   | 是  | 当前题目 ID                                               |
| answer       | string | 是  | 学生提交的答案。选择题为选项标号（如 `"A"`）；填空题为文本（支持 LaTeX）；解答题为完整解答文本 |
| timeSpent    | int    | 否  | 实际作答用时（秒），用于辅助评估                                      |

### 请求体示例

```json
{
  "diagnosticId": "diag_20260523_a1b2c3d4",
  "questionId": 1001,
  "answer": "A",
  "timeSpent": 45
}
```

### 响应体示例（答对，返回下一题）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "result": {
      "questionId": 1001,
      "isCorrect": true,
      "correctAnswer": "A",
      "errorType": null,
      "errorDetail": null,
      "errorAnalysis": null,
      "solution": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1) = 2$，其几何意义为曲线 $y = x^2$ 在点 $(1, 1)$ 处切线的斜率。"
    },
    "adaptiveInfo": {
      "previousDifficulty": 0.50,
      "nextDifficulty": 0.70,
      "adjustmentReason": "答对当前题目，难度上调，跳过同级知识点"
    },
    "isFinished": false,
    "currentQuestionIndex": 2,
    "totalEstimatedQuestions": 6,
    "nextQuestion": {
      "questionId": 1015,
      "knowledgePointId": "math_derivative_calc",
      "knowledgePointName": "导数的计算",
      "type": "choice",
      "difficulty": 0.70,
      "content": "求函数 $f(x) = x^3 - 3x^2 + 2$ 的导函数 $f'(x)$（　　）",
      "options": [
        { "label": "A", "text": "$3x^2 - 6x$" },
        { "label": "B", "text": "$3x^2 - 6x + 2$" },
        { "label": "C", "text": "$x^2 - 6x$" },
        { "label": "D", "text": "$3x^2 - 3x$" }
      ],
      "timeLimit": 90
    }
  }
}
```

### 响应体示例（答错，返回下一题 + 错因分析）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "result": {
      "questionId": 1015,
      "isCorrect": false,
      "correctAnswer": "A",
      "errorType": "计算性错误",
      "errorDetail": "运算法则错误-求导法则应用不当",
      "errorAnalysis": "你选择了 B 选项 $3x^2 - 6x + 2$，错误原因在于对常数项求导的处理：常数 $2$ 的导数应为 $0$，而非保留原值。求导法则要求：$(C)' = 0$（C 为常数）。正确的求导过程为：$f'(x) = (x^3)' - (3x^2)' + (2)' = 3x^2 - 6x + 0 = 3x^2 - 6x$。",
      "solution": "$f(x) = x^3 - 3x^2 + 2$\n\n逐项求导：\n- $(x^3)' = 3x^2$\n- $(3x^2)' = 6x$\n- $(2)' = 0$\n\n因此 $f'(x) = 3x^2 - 6x$"
    },
    "adaptiveInfo": {
      "previousDifficulty": 0.70,
      "nextDifficulty": 0.45,
      "adjustmentReason": "答错当前题目，难度下调，深入子知识点'基本求导法则'进一步诊断"
    },
    "isFinished": false,
    "currentQuestionIndex": 3,
    "totalEstimatedQuestions": 6,
    "nextQuestion": {
      "questionId": 1008,
      "knowledgePointId": "math_derivative_basic_rules",
      "knowledgePointName": "基本求导法则",
      "type": "choice",
      "difficulty": 0.45,
      "content": "下列求导结果正确的是（　　）",
      "options": [
        { "label": "A", "text": "$(x^4)' = 4x^3$" },
        { "label": "B", "text": "$(3x)' = 3x$" },
        { "label": "C", "text": "$(5)' = 5$" },
        { "label": "D", "text": "$(x^2 + 1)' = x^2$" }
      ],
      "timeLimit": 60
    }
  }
}
```

### 响应字段说明

| 字段                                   | 类型      | 说明                                                          |
|--------------------------------------|---------|-------------------------------------------------------------|
| data.diagnosticId                    | string  | 诊断会话 ID                                                     |
| data.result                          | object  | 当前题目的作答结果                                                   |
| data.result.questionId               | long    | 题目 ID                                                       |
| data.result.isCorrect                | boolean | 是否回答正确                                                      |
| data.result.correctAnswer            | string  | 正确答案                                                        |
| data.result.errorType                | string  | 错误类型（答对时为 null）。可选值：`概念性错误`、`计算性错误`、`逻辑性错误`、`方法性错误`、`审题性错误` |
| data.result.errorDetail              | string  | 错误子类描述（答对时为 null），如"符号错误-负号丢失"、"概念混淆-极值与最值"等                |
| data.result.errorAnalysis            | string  | LLM 生成的错因分析文本（答对时为 null），包含具体错在哪一步及原因分析，支持 LaTeX            |
| data.result.solution                 | string  | 完整题目解析（支持 LaTeX），无论对错均返回                                    |
| data.adaptiveInfo                    | object  | 自适应调整信息                                                     |
| data.adaptiveInfo.previousDifficulty | float   | 当前题目的难度                                                     |
| data.adaptiveInfo.nextDifficulty     | float   | 下一题的难度                                                      |
| data.adaptiveInfo.adjustmentReason   | string  | 难度调整原因说明                                                    |
| data.isFinished                      | boolean | 本次诊断是否已自动结束（达到足够题数且已覆盖关键知识点时系统可自动结束）                        |
| data.currentQuestionIndex            | int     | 下一题的序号                                                      |
| data.totalEstimatedQuestions         | int     | 预计总题数（可能动态调整）                                               |
| data.nextQuestion                    | object  | 下一道诊断题目（结构同启动接口中的 question 字段；若 isFinished 为 true 则为 null）  |

### 状态码说明

| 状态码 | 说明                               |
|-----|----------------------------------|
| 200 | 提交成功，返回判断结果和下一题                  |
| 400 | 参数错误（如 answer 为空、questionId 不匹配） |
| 401 | 未认证或 Token 已过期                   |
| 404 | 诊断会话不存在或已过期                      |
| 409 | 该题已提交过答案，不可重复提交                  |

---

## 3. 结束诊断

手动结束当前诊断会话（或系统自动结束后调用此接口获取诊断结论）。系统将汇总所有作答数据，利用 BKT/IRT
模型计算各知识点掌握度，生成完整的诊断结论，包括薄弱知识点列表、各知识点掌握度、错误模式统计以及后续学习建议。

- **路径**：`/api/diagnostic/finish`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名          | 类型     | 必填 | 说明      |
|--------------|--------|----|---------|
| diagnosticId | string | 是  | 诊断会话 ID |

### 请求体示例

```json
{
  "diagnosticId": "diag_20260523_a1b2c3d4"
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "module": "导数",
    "startTime": "2026-05-23T14:00:00",
    "endTime": "2026-05-23T14:12:35",
    "totalQuestions": 6,
    "correctCount": 3,
    "incorrectCount": 3,
    "accuracy": 0.50,
    "overallMastery": 0.45,
    "knowledgePointResults": [
      {
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "mastery": 0.78,
        "status": "基本掌握",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35,
        "status": "薄弱",
        "questionCount": 2,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_monotonicity",
        "knowledgePointName": "导数与单调性",
        "mastery": 0.60,
        "status": "一般",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      }
    ],
    "weakPoints": [
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35
      },
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42
      }
    ],
    "errorPatterns": {
      "概念性错误": 1,
      "计算性错误": 2,
      "逻辑性错误": 0,
      "方法性错误": 0,
      "审题性错误": 0
    },
    "suggestion": "本次诊断发现你在「导数的计算」和「导数的应用」两个知识点上较为薄弱，主要错误集中在计算性错误（求导法则应用不当）。建议先巩固基本求导法则，再逐步过渡到导数的综合应用。系统可为你进行溯源分析，找到更深层的薄弱根因。",
    "canTracing": true
  }
}
```

### 响应字段说明

| 字段                                              | 类型      | 说明                                                        |
|-------------------------------------------------|---------|-----------------------------------------------------------|
| data.diagnosticId                               | string  | 诊断会话 ID                                                   |
| data.module                                     | string  | 诊断的数学模块                                                   |
| data.startTime                                  | string  | 诊断开始时间（ISO 8601 格式）                                       |
| data.endTime                                    | string  | 诊断结束时间                                                    |
| data.totalQuestions                             | int     | 总题数                                                       |
| data.correctCount                               | int     | 答对题数                                                      |
| data.incorrectCount                             | int     | 答错题数                                                      |
| data.accuracy                                   | float   | 正确率（0.00-1.00）                                            |
| data.overallMastery                             | float   | 该模块总体掌握度（0.00-1.00，由 BKT/IRT 模型综合计算）                      |
| data.knowledgePointResults                      | array   | 各知识点诊断结果列表                                                |
| data.knowledgePointResults[].knowledgePointId   | string  | 知识点 ID                                                    |
| data.knowledgePointResults[].knowledgePointName | string  | 知识点名称                                                     |
| data.knowledgePointResults[].mastery            | float   | 掌握度（0.00-1.00）                                            |
| data.knowledgePointResults[].status             | string  | 掌握状态：`薄弱`(<0.5)、`一般`(0.5-0.7)、`基本掌握`(0.7-0.9)、`熟练`(>=0.9) |
| data.knowledgePointResults[].questionCount      | int     | 该知识点出题数量                                                  |
| data.knowledgePointResults[].correctCount       | int     | 该知识点答对数量                                                  |
| data.weakPoints                                 | array   | 薄弱知识点列表（掌握度 < 0.6 的知识点，按掌握度升序排列）                          |
| data.weakPoints[].knowledgePointId              | string  | 知识点 ID                                                    |
| data.weakPoints[].knowledgePointName            | string  | 知识点名称                                                     |
| data.weakPoints[].mastery                       | float   | 掌握度                                                       |
| data.errorPatterns                              | object  | 错误模式统计（五大类错误各出现的次数）                                       |
| data.suggestion                                 | string  | LLM 生成的诊断结论与学习建议文本                                        |
| data.canTracing                                 | boolean | 是否存在薄弱知识点可进行溯源分析                                          |

### 状态码说明

| 状态码 | 说明              |
|-----|-----------------|
| 200 | 诊断结束成功，返回完整诊断结论 |
| 400 | 参数错误            |
| 401 | 未认证或 Token 已过期  |
| 404 | 诊断会话不存在         |
| 409 | 该诊断会话已结束，请勿重复结束 |

---

## 4. 历史诊断记录列表

获取当前登录学生的历史诊断记录列表，支持分页查询。

- **路径**：`/api/diagnostic/history`
- **方法**：`GET`

### 请求参数（Query Parameters）

| 参数名      | 类型     | 必填 | 说明               |
|----------|--------|----|------------------|
| module   | string | 否  | 按模块筛选，可选值同启动接口   |
| page     | int    | 否  | 页码，从 1 开始，默认 1   |
| pageSize | int    | 否  | 每页数量，默认 10，最大 50 |

### 请求示例

```
GET /api/diagnostic/history?module=导数&page=1&pageSize=10
Authorization: Bearer {token}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "pageSize": 10,
    "records": [
      {
        "diagnosticId": "diag_20260523_a1b2c3d4",
        "module": "导数",
        "startTime": "2026-05-23T14:00:00",
        "endTime": "2026-05-23T14:12:35",
        "totalQuestions": 6,
        "correctCount": 3,
        "accuracy": 0.50,
        "overallMastery": 0.45,
        "weakPointCount": 3,
        "status": "completed"
      },
      {
        "diagnosticId": "diag_20260520_e5f6g7h8",
        "module": "函数",
        "startTime": "2026-05-20T10:30:00",
        "endTime": "2026-05-20T10:42:18",
        "totalQuestions": 5,
        "correctCount": 4,
        "accuracy": 0.80,
        "overallMastery": 0.72,
        "weakPointCount": 1,
        "status": "completed"
      }
    ]
  }
}
```

### 响应字段说明

| 字段                            | 类型     | 说明                                       |
|-------------------------------|--------|------------------------------------------|
| data.total                    | int    | 总记录数                                     |
| data.page                     | int    | 当前页码                                     |
| data.pageSize                 | int    | 每页数量                                     |
| data.records                  | array  | 诊断记录列表                                   |
| data.records[].diagnosticId   | string | 诊断会话 ID                                  |
| data.records[].module         | string | 诊断模块                                     |
| data.records[].startTime      | string | 开始时间                                     |
| data.records[].endTime        | string | 结束时间                                     |
| data.records[].totalQuestions | int    | 总题数                                      |
| data.records[].correctCount   | int    | 答对题数                                     |
| data.records[].accuracy       | float  | 正确率                                      |
| data.records[].overallMastery | float  | 模块总体掌握度                                  |
| data.records[].weakPointCount | int    | 薄弱知识点数量                                  |
| data.records[].status         | string | 诊断状态：`in_progress`(进行中)、`completed`(已完成) |

### 状态码说明

| 状态码 | 说明             |
|-----|----------------|
| 200 | 查询成功           |
| 401 | 未认证或 Token 已过期 |

---

## 5. 诊断详细结果

获取某次诊断的完整详细结果，包括每道题的作答记录、错因分析、各知识点掌握度等。

- **路径**：`/api/diagnostic/result/{diagnosticId}`
- **方法**：`GET`

### 路径参数

| 参数名          | 类型     | 必填 | 说明      |
|--------------|--------|----|---------|
| diagnosticId | string | 是  | 诊断会话 ID |

### 请求示例

```
GET /api/diagnostic/result/diag_20260523_a1b2c3d4
Authorization: Bearer {token}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "module": "导数",
    "startTime": "2026-05-23T14:00:00",
    "endTime": "2026-05-23T14:12:35",
    "totalQuestions": 6,
    "correctCount": 3,
    "accuracy": 0.50,
    "overallMastery": 0.45,
    "answerRecords": [
      {
        "index": 1,
        "questionId": 1001,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "type": "choice",
        "difficulty": 0.50,
        "content": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1)$ 的几何意义是（　　）",
        "options": [
          { "label": "A", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的切线斜率" },
          { "label": "B", "text": "曲线 $y = x^2$ 在点 $(1, 1)$ 处的纵坐标" },
          { "label": "C", "text": "函数 $f(x) = x^2$ 在 $x = 1$ 处的函数值" },
          { "label": "D", "text": "曲线 $y = x^2$ 过原点的割线斜率" }
        ],
        "studentAnswer": "A",
        "correctAnswer": "A",
        "isCorrect": true,
        "timeSpent": 45,
        "errorType": null,
        "errorDetail": null,
        "errorAnalysis": null,
        "solution": "函数 $f(x) = x^2$ 在 $x = 1$ 处的导数 $f'(1) = 2$，其几何意义为曲线 $y = x^2$ 在点 $(1, 1)$ 处切线的斜率。"
      },
      {
        "index": 2,
        "questionId": 1015,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "type": "choice",
        "difficulty": 0.70,
        "content": "求函数 $f(x) = x^3 - 3x^2 + 2$ 的导函数 $f'(x)$（　　）",
        "options": [
          { "label": "A", "text": "$3x^2 - 6x$" },
          { "label": "B", "text": "$3x^2 - 6x + 2$" },
          { "label": "C", "text": "$x^2 - 6x$" },
          { "label": "D", "text": "$3x^2 - 3x$" }
        ],
        "studentAnswer": "B",
        "correctAnswer": "A",
        "isCorrect": false,
        "timeSpent": 62,
        "errorType": "计算性错误",
        "errorDetail": "运算法则错误-求导法则应用不当",
        "errorAnalysis": "你选择了 B 选项 $3x^2 - 6x + 2$，错误原因在于对常数项求导的处理：常数 $2$ 的导数应为 $0$，而非保留原值。求导法则要求：$(C)' = 0$（C 为常数）。",
        "solution": "$f(x) = x^3 - 3x^2 + 2$\n\n逐项求导：\n- $(x^3)' = 3x^2$\n- $(3x^2)' = 6x$\n- $(2)' = 0$\n\n因此 $f'(x) = 3x^2 - 6x$"
      }
    ],
    "knowledgePointResults": [
      {
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "mastery": 0.78,
        "status": "基本掌握",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35,
        "status": "薄弱",
        "questionCount": 2,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      },
      {
        "knowledgePointId": "math_derivative_monotonicity",
        "knowledgePointName": "导数与单调性",
        "mastery": 0.60,
        "status": "一般",
        "questionCount": 1,
        "correctCount": 1
      },
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30,
        "status": "薄弱",
        "questionCount": 1,
        "correctCount": 0
      }
    ],
    "weakPoints": [
      {
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "mastery": 0.30
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "mastery": 0.35
      },
      {
        "knowledgePointId": "math_derivative_basic_rules",
        "knowledgePointName": "基本求导法则",
        "mastery": 0.42
      }
    ],
    "errorPatterns": {
      "概念性错误": 1,
      "计算性错误": 2,
      "逻辑性错误": 0,
      "方法性错误": 0,
      "审题性错误": 0
    },
    "suggestion": "本次诊断发现你在「导数的计算」和「导数的应用」两个知识点上较为薄弱，主要错误集中在计算性错误（求导法则应用不当）。建议先巩固基本求导法则，再逐步过渡到导数的综合应用。系统可为你进行溯源分析，找到更深层的薄弱根因。",
    "canTracing": true
  }
}
```

### 响应字段说明

响应中 `knowledgePointResults`、`weakPoints`、`errorPatterns`、`suggestion`、`canTracing`
等字段含义与 [结束诊断](#3-结束诊断) 接口一致。新增字段说明如下：

| 字段                                      | 类型      | 说明                    |
|-----------------------------------------|---------|-----------------------|
| data.answerRecords                      | array   | 完整的作答记录列表（按作答顺序排列）    |
| data.answerRecords[].index              | int     | 题目序号                  |
| data.answerRecords[].questionId         | long    | 题目 ID                 |
| data.answerRecords[].knowledgePointId   | string  | 所属知识点 ID              |
| data.answerRecords[].knowledgePointName | string  | 所属知识点名称               |
| data.answerRecords[].type               | string  | 题型                    |
| data.answerRecords[].difficulty         | float   | 难度系数                  |
| data.answerRecords[].content            | string  | 题目内容（支持 LaTeX）        |
| data.answerRecords[].options            | array   | 选项列表（选择题）             |
| data.answerRecords[].studentAnswer      | string  | 学生提交的答案               |
| data.answerRecords[].correctAnswer      | string  | 正确答案                  |
| data.answerRecords[].isCorrect          | boolean | 是否正确                  |
| data.answerRecords[].timeSpent          | int     | 作答用时（秒）               |
| data.answerRecords[].errorType          | string  | 错误类型（答对时为 null）       |
| data.answerRecords[].errorDetail        | string  | 错误子类描述（答对时为 null）     |
| data.answerRecords[].errorAnalysis      | string  | LLM 错因分析文本（答对时为 null） |
| data.answerRecords[].solution           | string  | 题目完整解析                |

### 状态码说明

| 状态码 | 说明               |
|-----|------------------|
| 200 | 查询成功             |
| 401 | 未认证或 Token 已过期   |
| 403 | 无权查看该诊断记录（非本人数据） |
| 404 | 诊断记录不存在          |

---

## 6. 溯源分析

对薄弱知识点进行溯源分析。这是系统的核心接口。溯源 Agent
将沿知识图谱的前置依赖链递归回溯，从目标薄弱知识点出发，逐层检查前置知识的掌握度，找到真正导致薄弱的根因知识点，并返回完整的溯源路径。

**溯源算法核心逻辑**：

1. 获取目标薄弱知识点的所有前置知识点
2. 对每个前置知识点检查掌握度：
    - 掌握度 < 0.6 → 继续递归溯源
    - 掌握度未知 → 触发快速诊断测试后再判定
3. 回溯终止条件：前置知识均已掌握（当前节点即为根因）、到达图谱叶节点、达到最大回溯深度
4. 支持跨模块溯源（如解析几何的问题根因可能在代数模块）

- **路径**：`/api/tracing/analyze`
- **方法**：`POST`
- **Content-Type**：`application/json`

### 请求参数

| 参数名               | 类型     | 必填 | 说明                                               |
|-------------------|--------|----|--------------------------------------------------|
| diagnosticId      | string | 否  | 关联的诊断会话 ID。若提供，将基于该诊断中发现的薄弱知识点进行溯源               |
| knowledgePointIds | array  | 否  | 指定需要溯源的知识点 ID 列表。与 diagnosticId 二选一，若同时提供则以此字段为准 |
| maxDepth          | int    | 否  | 最大溯源深度，默认 5，最大 10。用于防止无限递归                       |
| masteryThreshold  | float  | 否  | 掌握度阈值，低于此值判定为薄弱，默认 0.6                           |

### 请求体示例（基于诊断结果溯源）

```json
{
  "diagnosticId": "diag_20260523_a1b2c3d4",
  "maxDepth": 5,
  "masteryThreshold": 0.6
}
```

### 请求体示例（指定知识点溯源）

```json
{
  "knowledgePointIds": ["math_derivative_application", "math_derivative_calc"],
  "maxDepth": 5,
  "masteryThreshold": 0.6
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tracingId": "trace_20260523_x9y8z7w6",
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "createdAt": "2026-05-23T14:15:00",
    "analyzedPointCount": 2,
    "rootCauseCount": 1,
    "isCrossModule": false,
    "tracingResults": [
      {
        "targetPointId": "math_derivative_application",
        "targetPointName": "导数的应用",
        "targetMastery": 0.30,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 3,
          "reason": "该知识点为当前溯源链的最深层薄弱点：其所有前置知识均已掌握，但其自身掌握度仅为 0.25，说明对变化率核心概念理解不足，导致后续依赖的导数计算、导数应用连锁薄弱。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_application",
            "knowledgePointName": "导数的应用",
            "module": "导数",
            "mastery": 0.30,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_extremum_max",
            "knowledgePointName": "极值与最值",
            "module": "导数",
            "mastery": 0.40,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 2,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 3,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_application",
            "to": "math_extremum_max",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_extremum_max",
            "to": "math_derivative_calc",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      },
      {
        "targetPointId": "math_derivative_calc",
        "targetPointName": "导数的计算",
        "targetMastery": 0.35,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 1,
          "reason": "与「导数的应用」的溯源根因一致，均指向变化率概念的理解不足。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      }
    ],
    "mergedRootCauses": [
      {
        "knowledgePointId": "math_change_rate",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "module": "导数",
        "mastery": 0.25,
        "affectedPoints": ["math_derivative_calc", "math_extremum_max", "math_derivative_application"],
        "affectedPointNames": ["导数的计算", "极值与最值", "导数的应用"],
        "priority": 1
      }
    ],
    "suggestion": "溯源分析发现，你在「导数的应用」和「导数的计算」上的薄弱，根因均指向「平均变化率与瞬时变化率」的理解不足（掌握度仅 0.25）。建议从变化率的概念出发，理解导数的本质含义，再逐步推进到导数计算和应用。建议学习路径：平均变化率与瞬时变化率 → 导数的概念与几何意义 → 导数的计算 → 极值与最值 → 导数的应用。"
  }
}
```

### 响应字段说明

| 字段                                                     | 类型      | 说明                                                   |
|--------------------------------------------------------|---------|------------------------------------------------------|
| data.tracingId                                         | string  | 溯源分析唯一标识                                             |
| data.diagnosticId                                      | string  | 关联的诊断会话 ID（若从诊断结果发起溯源）                               |
| data.createdAt                                         | string  | 溯源分析时间                                               |
| data.analyzedPointCount                                | int     | 分析的薄弱知识点数量                                           |
| data.rootCauseCount                                    | int     | 去重后发现的根因知识点数量                                        |
| data.isCrossModule                                     | boolean | 是否存在跨模块溯源（如解析几何根因在代数模块）                              |
| data.tracingResults                                    | array   | 每个薄弱知识点的溯源结果列表                                       |
| data.tracingResults[].targetPointId                    | string  | 目标薄弱知识点 ID                                           |
| data.tracingResults[].targetPointName                  | string  | 目标薄弱知识点名称                                            |
| data.tracingResults[].targetMastery                    | float   | 目标知识点掌握度                                             |
| data.tracingResults[].rootCause                        | object  | 溯源发现的根因知识点                                           |
| data.tracingResults[].rootCause.knowledgePointId       | string  | 根因知识点 ID                                             |
| data.tracingResults[].rootCause.knowledgePointName     | string  | 根因知识点名称                                              |
| data.tracingResults[].rootCause.module                 | string  | 根因知识点所属模块                                            |
| data.tracingResults[].rootCause.mastery                | float   | 根因知识点掌握度                                             |
| data.tracingResults[].rootCause.depth                  | int     | 溯源深度（从目标知识点到根因的距离）                                   |
| data.tracingResults[].rootCause.reason                 | string  | LLM 生成的根因判定理由说明                                      |
| data.tracingResults[].tracingPath                      | array   | 完整溯源路径节点列表（从目标知识点到根因，按回溯顺序排列）                        |
| data.tracingResults[].tracingPath[].knowledgePointId   | string  | 知识点 ID                                               |
| data.tracingResults[].tracingPath[].knowledgePointName | string  | 知识点名称                                                |
| data.tracingResults[].tracingPath[].module             | string  | 所属模块                                                 |
| data.tracingResults[].tracingPath[].mastery            | float   | 掌握度                                                  |
| data.tracingResults[].tracingPath[].status             | string  | 掌握状态：`薄弱`/`一般`/`基本掌握`/`熟练`                           |
| data.tracingResults[].tracingPath[].depth              | int     | 在溯源路径中的深度（目标知识点为 0）                                  |
| data.tracingResults[].tracingPath[].isTarget           | boolean | 是否为目标薄弱知识点                                           |
| data.tracingResults[].tracingPath[].isRootCause        | boolean | 是否为根因知识点                                             |
| data.tracingResults[].pathEdges                        | array   | 溯源路径上的边（前置依赖关系），用于可视化连线                              |
| data.tracingResults[].pathEdges[].from                 | string  | 起始知识点 ID（较高层知识点）                                     |
| data.tracingResults[].pathEdges[].to                   | string  | 目标知识点 ID（前置知识点）                                      |
| data.tracingResults[].pathEdges[].relation             | string  | 关系类型，固定为 `PREREQUISITE_OF`                           |
| data.tracingResults[].pathEdges[].weight               | float   | 依赖权重（0.0-1.0，值越大依赖越强）                                |
| data.tracingResults[].pathEdges[].type                 | string  | 依赖类型：`strong`(强依赖)、`weak`(弱依赖)、`cross_module`(跨模块依赖) |
| data.mergedRootCauses                                  | array   | 去重合并后的根因知识点列表（多个薄弱点可能指向同一根因），按优先级排序                  |
| data.mergedRootCauses[].knowledgePointId               | string  | 根因知识点 ID                                             |
| data.mergedRootCauses[].knowledgePointName             | string  | 根因知识点名称                                              |
| data.mergedRootCauses[].module                         | string  | 所属模块                                                 |
| data.mergedRootCauses[].mastery                        | float   | 掌握度                                                  |
| data.mergedRootCauses[].affectedPoints                 | array   | 受该根因影响的薄弱知识点 ID 列表                                   |
| data.mergedRootCauses[].affectedPointNames             | array   | 受该根因影响的薄弱知识点名称列表                                     |
| data.mergedRootCauses[].priority                       | int     | 优先修复顺序（1 为最优先）                                       |
| data.suggestion                                        | string  | LLM 生成的综合溯源分析结论与学习建议                                 |

### 状态码说明

| 状态码 | 说明                                            |
|-----|-----------------------------------------------|
| 200 | 溯源分析成功                                        |
| 400 | 参数错误（如 diagnosticId 和 knowledgePointIds 均未提供） |
| 401 | 未认证或 Token 已过期                                |
| 404 | 诊断记录不存在或指定的知识点 ID 在图谱中不存在                     |
| 422 | 诊断尚未完成，无法进行溯源分析                               |

---

## 7. 溯源结果详情

获取某次溯源分析的完整结果详情，包含完整溯源路径及可视化数据。适用于前端进行知识图谱溯源路径的可视化渲染（如 D3.js
力导向图、树形图等）。

- **路径**：`/api/tracing/result/{tracingId}`
- **方法**：`GET`

### 路径参数

| 参数名       | 类型     | 必填 | 说明       |
|-----------|--------|----|----------|
| tracingId | string | 是  | 溯源分析唯一标识 |

### 请求示例

```
GET /api/tracing/result/trace_20260523_x9y8z7w6
Authorization: Bearer {token}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tracingId": "trace_20260523_x9y8z7w6",
    "diagnosticId": "diag_20260523_a1b2c3d4",
    "createdAt": "2026-05-23T14:15:00",
    "analyzedPointCount": 2,
    "rootCauseCount": 1,
    "isCrossModule": false,
    "tracingResults": [
      {
        "targetPointId": "math_derivative_application",
        "targetPointName": "导数的应用",
        "targetMastery": 0.30,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 3,
          "reason": "该知识点为当前溯源链的最深层薄弱点：其所有前置知识均已掌握，但其自身掌握度仅为 0.25，说明对变化率核心概念理解不足，导致后续依赖的导数计算、导数应用连锁薄弱。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_application",
            "knowledgePointName": "导数的应用",
            "module": "导数",
            "mastery": 0.30,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_extremum_max",
            "knowledgePointName": "极值与最值",
            "module": "导数",
            "mastery": 0.40,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 2,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 3,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_application",
            "to": "math_extremum_max",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_extremum_max",
            "to": "math_derivative_calc",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      },
      {
        "targetPointId": "math_derivative_calc",
        "targetPointName": "导数的计算",
        "targetMastery": 0.35,
        "rootCause": {
          "knowledgePointId": "math_change_rate",
          "knowledgePointName": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "depth": 1,
          "reason": "与「导数的应用」的溯源根因一致，均指向变化率概念的理解不足。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_derivative_calc",
            "knowledgePointName": "导数的计算",
            "module": "导数",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_change_rate",
            "knowledgePointName": "平均变化率与瞬时变化率",
            "module": "导数",
            "mastery": 0.25,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_derivative_calc",
            "to": "math_change_rate",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "strong"
          }
        ]
      }
    ],
    "mergedRootCauses": [
      {
        "knowledgePointId": "math_change_rate",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "module": "导数",
        "mastery": 0.25,
        "affectedPoints": ["math_derivative_calc", "math_extremum_max", "math_derivative_application"],
        "affectedPointNames": ["导数的计算", "极值与最值", "导数的应用"],
        "priority": 1
      }
    ],
    "graphVisualization": {
      "nodes": [
        {
          "id": "math_derivative_application",
          "label": "导数的应用",
          "module": "导数",
          "mastery": 0.30,
          "nodeType": "target",
          "color": "#F56C6C",
          "size": 40
        },
        {
          "id": "math_extremum_max",
          "label": "极值与最值",
          "module": "导数",
          "mastery": 0.40,
          "nodeType": "intermediate",
          "color": "#F56C6C",
          "size": 32
        },
        {
          "id": "math_derivative_calc",
          "label": "导数的计算",
          "module": "导数",
          "mastery": 0.35,
          "nodeType": "target",
          "color": "#F56C6C",
          "size": 40
        },
        {
          "id": "math_change_rate",
          "label": "平均变化率与瞬时变化率",
          "module": "导数",
          "mastery": 0.25,
          "nodeType": "rootCause",
          "color": "#FF0000",
          "size": 48
        }
      ],
      "edges": [
        {
          "source": "math_derivative_application",
          "target": "math_extremum_max",
          "label": "前置依赖",
          "weight": 0.9,
          "style": "solid"
        },
        {
          "source": "math_extremum_max",
          "target": "math_derivative_calc",
          "label": "前置依赖",
          "weight": 0.9,
          "style": "solid"
        },
        {
          "source": "math_derivative_calc",
          "target": "math_change_rate",
          "label": "前置依赖",
          "weight": 0.8,
          "style": "solid"
        }
      ],
      "colorLegend": {
        "nodeColors": {
          "#FF0000": "根因知识点",
          "#F56C6C": "薄弱知识点（掌握度 < 0.5）",
          "#E6A23C": "一般知识点（掌握度 0.5-0.7）",
          "#67C23A": "已掌握知识点（掌握度 >= 0.7）"
        },
        "edgeStyles": {
          "solid": "强依赖",
          "dashed": "弱依赖",
          "dotted": "跨模块依赖"
        }
      }
    },
    "suggestion": "溯源分析发现，你在「导数的应用」和「导数的计算」上的薄弱，根因均指向「平均变化率与瞬时变化率」的理解不足（掌握度仅 0.25）。建议从变化率的概念出发，理解导数的本质含义，再逐步推进到导数计算和应用。建议学习路径：平均变化率与瞬时变化率 → 导数的概念与几何意义 → 导数的计算 → 极值与最值 → 导数的应用。",
    "suggestedLearningPath": [
      {
        "order": 1,
        "knowledgePointId": "math_change_rate",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "currentMastery": 0.25,
        "estimatedMinutes": 45,
        "isRootCause": true
      },
      {
        "order": 2,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "currentMastery": 0.78,
        "estimatedMinutes": 15,
        "isRootCause": false
      },
      {
        "order": 3,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "currentMastery": 0.35,
        "estimatedMinutes": 50,
        "isRootCause": false
      },
      {
        "order": 4,
        "knowledgePointId": "math_extremum_max",
        "knowledgePointName": "极值与最值",
        "currentMastery": 0.40,
        "estimatedMinutes": 40,
        "isRootCause": false
      },
      {
        "order": 5,
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "currentMastery": 0.30,
        "estimatedMinutes": 45,
        "isRootCause": false
      }
    ]
  }
}
```

### 响应字段说明

本接口返回数据是 [溯源分析](#6-溯源分析) 接口响应的超集，在其基础上新增了 `graphVisualization`（图可视化数据）和
`suggestedLearningPath`（建议学习路径）两个字段。

| 字段                                              | 类型      | 说明                                                          |
|-------------------------------------------------|---------|-------------------------------------------------------------|
| data.graphVisualization                         | object  | 用于前端可视化渲染的图数据（D3.js / ECharts 等）                            |
| data.graphVisualization.nodes                   | array   | 图中所有节点（知识点）                                                 |
| data.graphVisualization.nodes[].id              | string  | 节点唯一标识（知识点 ID）                                              |
| data.graphVisualization.nodes[].label           | string  | 节点显示名称                                                      |
| data.graphVisualization.nodes[].module          | string  | 所属数学模块                                                      |
| data.graphVisualization.nodes[].mastery         | float   | 掌握度                                                         |
| data.graphVisualization.nodes[].nodeType        | string  | 节点类型：`target`(目标薄弱点)、`intermediate`(中间节点)、`rootCause`(根因节点) |
| data.graphVisualization.nodes[].color           | string  | 建议渲染颜色（HEX 格式），按掌握度和节点类型分配                                  |
| data.graphVisualization.nodes[].size            | int     | 建议节点大小（像素），根因节点最大、中间节点最小                                    |
| data.graphVisualization.edges                   | array   | 图中所有边（依赖关系）                                                 |
| data.graphVisualization.edges[].source          | string  | 起始节点 ID                                                     |
| data.graphVisualization.edges[].target          | string  | 目标节点 ID                                                     |
| data.graphVisualization.edges[].label           | string  | 边的标签说明                                                      |
| data.graphVisualization.edges[].weight          | float   | 依赖权重                                                        |
| data.graphVisualization.edges[].style           | string  | 线条样式：`solid`(实线-强依赖)、`dashed`(虚线-弱依赖)、`dotted`(点线-跨模块)      |
| data.graphVisualization.colorLegend             | object  | 颜色图例说明，供前端渲染图例使用                                            |
| data.graphVisualization.colorLegend.nodeColors  | object  | 节点颜色含义映射                                                    |
| data.graphVisualization.colorLegend.edgeStyles  | object  | 边样式含义映射                                                     |
| data.suggestedLearningPath                      | array   | 根据溯源结果生成的建议学习路径（拓扑排序，先学前置知识）                                |
| data.suggestedLearningPath[].order              | int     | 学习顺序                                                        |
| data.suggestedLearningPath[].knowledgePointId   | string  | 知识点 ID                                                      |
| data.suggestedLearningPath[].knowledgePointName | string  | 知识点名称                                                       |
| data.suggestedLearningPath[].currentMastery     | float   | 当前掌握度                                                       |
| data.suggestedLearningPath[].estimatedMinutes   | int     | 预计学习所需时间（分钟）                                                |
| data.suggestedLearningPath[].isRootCause        | boolean | 是否为根因知识点                                                    |

### 状态码说明

| 状态码 | 说明               |
|-----|------------------|
| 200 | 查询成功             |
| 401 | 未认证或 Token 已过期   |
| 403 | 无权查看该溯源记录（非本人数据） |
| 404 | 溯源记录不存在          |

---

## 附录 A：数据结构汇总

### A.1 诊断题目结构（Question）

```json
{
  "questionId": 1001,
  "knowledgePointId": "math_derivative_concept",
  "knowledgePointName": "导数的概念与几何意义",
  "type": "choice | fill | solve",
  "difficulty": 0.50,
  "content": "题目内容（支持 LaTeX：$...$ 行内公式、$$...$$ 块级公式）",
  "options": [
    { "label": "A", "text": "选项内容（支持 LaTeX）" }
  ],
  "timeLimit": 120
}
```

### A.2 作答结果结构（AnswerResult）

```json
{
  "questionId": 1001,
  "isCorrect": false,
  "correctAnswer": "A",
  "errorType": "概念性错误 | 计算性错误 | 逻辑性错误 | 方法性错误 | 审题性错误",
  "errorDetail": "错误子类描述，如：概念混淆-极值与最值、符号错误-负号丢失、分类讨论遗漏",
  "errorAnalysis": "LLM 生成的详细错因分析文本，说明具体错在哪一步、为什么错",
  "solution": "完整题目解析（支持 LaTeX）"
}
```

### A.3 溯源路径节点结构（TracingPathNode）

```json
{
  "knowledgePointId": "math_change_rate",
  "knowledgePointName": "平均变化率与瞬时变化率",
  "module": "导数",
  "mastery": 0.25,
  "status": "薄弱 | 一般 | 基本掌握 | 熟练",
  "depth": 3,
  "isTarget": false,
  "isRootCause": true
}
```

### A.4 溯源路径边结构（TracingPathEdge）

```json
{
  "from": "math_derivative_calc",
  "to": "math_change_rate",
  "relation": "PREREQUISITE_OF",
  "weight": 0.8,
  "type": "strong | weak | cross_module"
}
```

### A.5 错误类型枚举

| 错误大类  | 子类示例                            |
|-------|---------------------------------|
| 概念性错误 | 定义理解偏差、概念混淆（如混淆"极值"与"最值"）       |
| 计算性错误 | 符号错误（负号丢失）、运算法则错误、精度错误（分数化简不彻底） |
| 逻辑性错误 | 充要条件误判、分类讨论遗漏、推理跳步              |
| 方法性错误 | 方法选择不当、转化方向错误                   |
| 审题性错误 | 条件遗漏（忽略隐含条件）、问题误读               |

### A.6 自适应机制说明

| 作答情况 | 难度调整策略              | 知识点选择策略           |
|------|---------------------|-------------------|
| 答对   | 难度上调（+0.15 ~ +0.25） | 跳过同级知识点，向更高阶知识点探测 |
| 答错   | 难度下调（-0.15 ~ -0.25） | 深入该知识点的子节点/前置知识点  |
| 连续答对 | 加速跳过，减少同模块出题数量      | 快速完成该模块诊断         |
| 连续答错 | 大幅下调，从基础题重新开始       | 切换到更基础的前置知识点      |

每个模块通常 5-8 题完成诊断，系统根据信息增益自动判断何时结束。

---

## 附录 B：跨模块溯源示例

当溯源过程中发现根因知识点属于不同的数学模块时，响应中 `isCrossModule` 字段为 `true`，溯源路径中的 `module` 字段将体现模块切换。

### 请求体示例

```json
{
  "knowledgePointIds": ["math_conic_line_intersection"],
  "maxDepth": 6
}
```

### 响应体（关键片段）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tracingId": "trace_20260523_cross01",
    "isCrossModule": true,
    "tracingResults": [
      {
        "targetPointId": "math_conic_line_intersection",
        "targetPointName": "直线与圆锥曲线联立",
        "targetMastery": 0.28,
        "rootCause": {
          "knowledgePointId": "math_quadratic_equation_roots",
          "knowledgePointName": "一元二次方程求根",
          "module": "代数基础",
          "mastery": 0.30,
          "depth": 3,
          "reason": "溯源发现跨模块根因：解析几何中「直线与圆锥曲线联立」的薄弱根因在代数基础模块的「一元二次方程求根」。联立方程组最终需解一元二次方程，该基础能力不足导致上层知识连锁薄弱。"
        },
        "tracingPath": [
          {
            "knowledgePointId": "math_conic_line_intersection",
            "knowledgePointName": "直线与圆锥曲线联立",
            "module": "解析几何",
            "mastery": 0.28,
            "status": "薄弱",
            "depth": 0,
            "isTarget": true,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_simultaneous_equations",
            "knowledgePointName": "联立方程组求解",
            "module": "解析几何",
            "mastery": 0.35,
            "status": "薄弱",
            "depth": 1,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_vieta_discriminant",
            "knowledgePointName": "韦达定理与判别式",
            "module": "代数基础",
            "mastery": 0.40,
            "status": "薄弱",
            "depth": 2,
            "isTarget": false,
            "isRootCause": false
          },
          {
            "knowledgePointId": "math_quadratic_equation_roots",
            "knowledgePointName": "一元二次方程求根",
            "module": "代数基础",
            "mastery": 0.30,
            "status": "薄弱",
            "depth": 3,
            "isTarget": false,
            "isRootCause": true
          }
        ],
        "pathEdges": [
          {
            "from": "math_conic_line_intersection",
            "to": "math_simultaneous_equations",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          },
          {
            "from": "math_simultaneous_equations",
            "to": "math_vieta_discriminant",
            "relation": "PREREQUISITE_OF",
            "weight": 0.8,
            "type": "cross_module"
          },
          {
            "from": "math_vieta_discriminant",
            "to": "math_quadratic_equation_roots",
            "relation": "PREREQUISITE_OF",
            "weight": 0.9,
            "type": "strong"
          }
        ]
      }
    ],
    "suggestion": "溯源发现跨模块根因：你在解析几何「直线与圆锥曲线联立」上的困难，根因在代数基础模块的「一元二次方程求根」（掌握度仅 0.30）。建议先从代数基础补起：一元二次方程求根 → 韦达定理与判别式 → 联立方程组求解 → 直线与圆锥曲线联立。"
  }
}
```

---

<!-- Source: api-learning-chat.md -->

# SmartMentor 学习路径与AI对话辅导 接口文档

> 版本：v1.0
> 基础路径：`/api`
> 认证方式：JWT Bearer Token（所有接口均需在请求头中携带 `Authorization: Bearer {token}`）
> 统一响应格式：`{ "code": 200, "message": "success", "data": {...} }`

---

## 目录

1. [POST /api/learning/path/generate - 生成学习路径](#1-生成学习路径)
2. [GET /api/learning/path - 获取当前活跃学习路径](#2-获取当前活跃学习路径)
3. [GET /api/learning/path/{pathId} - 获取指定学习路径详情](#3-获取指定学习路径详情)
4. [GET /api/learning/lesson/{pathId}/{nodeId} - 获取个性化课程内容](#4-获取个性化课程内容)
5. [POST /api/learning/exercise/submit - 提交练习答案](#5-提交练习答案)
6. [POST /api/learning/checkpoint/submit - 提交检查点测试](#6-提交检查点测试)
7. [GET /api/chat/stream - SSE流式AI对话辅导](#7-sse流式ai对话辅导)
8. [GET /api/chat/history - 获取对话历史记录](#8-获取对话历史记录)

---

## 通用说明

### 认证方式

所有接口均需要在 HTTP 请求头中携带 JWT Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 通用状态码

| 状态码 | 说明          |
|-----|-------------|
| 200 | 请求成功        |
| 400 | 请求参数错误      |
| 401 | 未认证或Token过期 |
| 403 | 无权限访问       |
| 404 | 资源不存在       |
| 500 | 服务器内部错误     |

---

## 1. 生成学习路径

基于溯源Agent的分析结果，通过DAG拓扑排序生成从根因知识点到目标知识点的有序学习路径。

### 基本信息

| 项目               | 内容                                                                  |
|------------------|---------------------------------------------------------------------|
| **路径**           | `/api/learning/path/generate`                                       |
| **方法**           | `POST`                                                              |
| **说明**           | 根据溯源结果中的薄弱知识点集合，构建有向无环图（DAG），通过拓扑排序确定学习顺序，结合学生画像分配学习时间，生成完整的个性化学习路径 |
| **认证**           | 需要 JWT Token                                                        |
| **Content-Type** | `application/json`                                                  |

### 请求参数

#### 请求体（Request Body）

| 参数名                    | 类型      | 必填 | 说明                                               |
|------------------------|---------|----|--------------------------------------------------|
| tracingResultId        | Long    | 是  | 溯源结果ID，关联溯源Agent的输出                              |
| targetKnowledgePointId | String  | 是  | 目标知识点ID（学生最终要掌握的知识点）                             |
| mode                   | String  | 否  | 学习模式：`urgent`（考试临近，紧凑计划）/ `systematic`（系统提升，默认值） |
| dailyStudyMinutes      | Integer | 否  | 每日学习时长（分钟），默认从学生画像读取                             |

### 请求体示例

```json
{
  "tracingResultId": 1001,
  "targetKnowledgePointId": "math_derivative_application",
  "mode": "systematic",
  "dailyStudyMinutes": 60
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "studentId": 10001,
    "targetKnowledgePoint": {
      "id": "math_derivative_application",
      "name": "导数的应用"
    },
    "rootCausePoint": {
      "id": "math_average_rate_of_change",
      "name": "平均变化率与瞬时变化率"
    },
    "mode": "systematic",
    "totalEstimatedMinutes": 180,
    "totalNodes": 4,
    "nodes": [
      {
        "nodeId": "node_001",
        "order": 1,
        "knowledgePointId": "math_average_rate_of_change",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "currentMastery": 0.25,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "status": "pending",
        "prerequisites": [],
        "subtasks": [
          { "type": "lesson", "title": "概念讲解", "minutes": 15 },
          { "type": "exercise", "title": "基础练习", "minutes": 20 },
          { "type": "checkpoint", "title": "检查点测试", "minutes": 10 }
        ]
      },
      {
        "nodeId": "node_002",
        "order": 2,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "currentMastery": 0.38,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 40,
        "status": "locked",
        "prerequisites": ["node_001"],
        "subtasks": [
          { "type": "lesson", "title": "概念讲解", "minutes": 15 },
          { "type": "exercise", "title": "强化练习", "minutes": 20 },
          { "type": "checkpoint", "title": "检查点测试", "minutes": 5 }
        ]
      },
      {
        "nodeId": "node_003",
        "order": 3,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "currentMastery": 0.42,
        "teachingStrategy": "strengthening",
        "estimatedMinutes": 50,
        "status": "locked",
        "prerequisites": ["node_002"],
        "subtasks": [
          { "type": "lesson", "title": "错因针对性讲解", "minutes": 15 },
          { "type": "exercise", "title": "变式训练", "minutes": 25 },
          { "type": "checkpoint", "title": "检查点测试", "minutes": 10 }
        ]
      },
      {
        "nodeId": "node_004",
        "order": 4,
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "currentMastery": 0.30,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "status": "locked",
        "prerequisites": ["node_003"],
        "subtasks": [
          { "type": "lesson", "title": "综合讲解", "minutes": 15 },
          { "type": "exercise", "title": "综合练习", "minutes": 20 },
          { "type": "checkpoint", "title": "综合测试（含原始错题变式）", "minutes": 10 }
        ]
      }
    ],
    "tracingPath": [
      "math_derivative_application",
      "math_derivative_calc",
      "math_derivative_concept",
      "math_average_rate_of_change"
    ],
    "status": "active",
    "progress": 0.00,
    "createdAt": "2026-05-23T10:30:00"
  }
}
```

### 状态码说明

| 状态码 | 说明                                |
|-----|-----------------------------------|
| 200 | 学习路径生成成功                          |
| 400 | 参数错误（如tracingResultId无效、目标知识点不存在） |
| 401 | 未认证或Token过期                       |
| 404 | 溯源结果不存在                           |
| 409 | 已存在进行中的同目标学习路径（需先完成或暂停现有路径）       |
| 500 | 服务器内部错误（如知识图谱查询失败）                |

---

## 2. 获取当前活跃学习路径

获取当前学生正在进行中（status=active）的学习路径列表。

### 基本信息

| 项目     | 内容                                     |
|--------|----------------------------------------|
| **路径** | `/api/learning/path`                   |
| **方法** | `GET`                                  |
| **说明** | 获取当前登录学生所有状态为active的学习路径概览信息，按创建时间倒序排列 |
| **认证** | 需要 JWT Token                           |

### 请求参数

#### Query参数

| 参数名    | 类型      | 必填 | 说明                                                  |
|--------|---------|----|-----------------------------------------------------|
| status | String  | 否  | 路径状态筛选：`active`（默认）/ `completed` / `paused` / `all` |
| page   | Integer | 否  | 页码，默认1                                              |
| size   | Integer | 否  | 每页条数，默认10                                           |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/learning/path?status=active&page=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "page": 1,
    "size": 10,
    "paths": [
      {
        "pathId": 2001,
        "targetKnowledgePoint": {
          "id": "math_derivative_application",
          "name": "导数的应用",
          "module": "导数"
        },
        "rootCausePoint": {
          "id": "math_average_rate_of_change",
          "name": "平均变化率与瞬时变化率"
        },
        "totalNodes": 4,
        "completedNodes": 1,
        "currentNode": {
          "nodeId": "node_002",
          "knowledgePointName": "导数的概念与几何意义"
        },
        "progress": 0.25,
        "status": "active",
        "mode": "systematic",
        "totalEstimatedMinutes": 180,
        "actualStudyMinutes": 48,
        "createdAt": "2026-05-20T10:30:00",
        "lastStudyAt": "2026-05-22T19:15:00"
      },
      {
        "pathId": 2002,
        "targetKnowledgePoint": {
          "id": "math_trig_identity",
          "name": "三角恒等变换",
          "module": "三角函数"
        },
        "rootCausePoint": {
          "id": "math_angle_addition",
          "name": "和差角公式"
        },
        "totalNodes": 3,
        "completedNodes": 0,
        "currentNode": {
          "nodeId": "node_010",
          "knowledgePointName": "和差角公式"
        },
        "progress": 0.00,
        "status": "active",
        "mode": "urgent",
        "totalEstimatedMinutes": 90,
        "actualStudyMinutes": 0,
        "createdAt": "2026-05-22T08:00:00",
        "lastStudyAt": null
      }
    ]
  }
}
```

### 状态码说明

| 状态码 | 说明          |
|-----|-------------|
| 200 | 请求成功        |
| 401 | 未认证或Token过期 |
| 500 | 服务器内部错误     |

---

## 3. 获取指定学习路径详情

根据路径ID获取学习路径的完整详细信息，包括所有节点的状态和学习进度。

### 基本信息

| 项目     | 内容                                          |
|--------|---------------------------------------------|
| **路径** | `/api/learning/path/{pathId}`               |
| **方法** | `GET`                                       |
| **说明** | 获取指定学习路径的完整详情，包含所有知识点节点、各节点学习状态、掌握度变化、溯源路径等 |
| **认证** | 需要 JWT Token                                |

### 请求参数

#### 路径参数（Path Parameters）

| 参数名    | 类型   | 必填 | 说明     |
|--------|------|----|--------|
| pathId | Long | 是  | 学习路径ID |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/learning/path/2001
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "studentId": 10001,
    "targetKnowledgePoint": {
      "id": "math_derivative_application",
      "name": "导数的应用",
      "module": "导数",
      "difficulty": 0.7,
      "importance": 0.9
    },
    "rootCausePoint": {
      "id": "math_average_rate_of_change",
      "name": "平均变化率与瞬时变化率",
      "module": "导数",
      "difficulty": 0.4
    },
    "mode": "systematic",
    "status": "active",
    "progress": 0.25,
    "totalEstimatedMinutes": 180,
    "actualStudyMinutes": 48,
    "totalNodes": 4,
    "completedNodes": 1,
    "nodes": [
      {
        "nodeId": "node_001",
        "order": 1,
        "knowledgePointId": "math_average_rate_of_change",
        "knowledgePointName": "平均变化率与瞬时变化率",
        "module": "导数",
        "currentMastery": 0.82,
        "masteryBefore": 0.25,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "actualMinutes": 48,
        "status": "completed",
        "completedAt": "2026-05-21T20:30:00",
        "checkpointPassed": true,
        "checkpointScore": 0.85,
        "prerequisites": []
      },
      {
        "nodeId": "node_002",
        "order": 2,
        "knowledgePointId": "math_derivative_concept",
        "knowledgePointName": "导数的概念与几何意义",
        "module": "导数",
        "currentMastery": 0.38,
        "masteryBefore": 0.38,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 40,
        "actualMinutes": 0,
        "status": "in_progress",
        "completedAt": null,
        "checkpointPassed": null,
        "checkpointScore": null,
        "prerequisites": ["node_001"]
      },
      {
        "nodeId": "node_003",
        "order": 3,
        "knowledgePointId": "math_derivative_calc",
        "knowledgePointName": "导数的计算",
        "module": "导数",
        "currentMastery": 0.42,
        "masteryBefore": 0.42,
        "teachingStrategy": "strengthening",
        "estimatedMinutes": 50,
        "actualMinutes": 0,
        "status": "locked",
        "completedAt": null,
        "checkpointPassed": null,
        "checkpointScore": null,
        "prerequisites": ["node_002"]
      },
      {
        "nodeId": "node_004",
        "order": 4,
        "knowledgePointId": "math_derivative_application",
        "knowledgePointName": "导数的应用",
        "module": "导数",
        "currentMastery": 0.30,
        "masteryBefore": 0.30,
        "teachingStrategy": "basic_consolidation",
        "estimatedMinutes": 45,
        "actualMinutes": 0,
        "status": "locked",
        "completedAt": null,
        "checkpointPassed": null,
        "checkpointScore": null,
        "prerequisites": ["node_003"]
      }
    ],
    "tracingResult": {
      "tracingId": 1001,
      "tracingPath": [
        {
          "knowledgePointId": "math_derivative_application",
          "name": "导数的应用",
          "mastery": 0.30,
          "role": "target"
        },
        {
          "knowledgePointId": "math_derivative_calc",
          "name": "导数的计算",
          "mastery": 0.42,
          "role": "intermediate"
        },
        {
          "knowledgePointId": "math_derivative_concept",
          "name": "导数的概念与几何意义",
          "mastery": 0.38,
          "role": "intermediate"
        },
        {
          "knowledgePointId": "math_average_rate_of_change",
          "name": "平均变化率与瞬时变化率",
          "mastery": 0.25,
          "role": "root_cause"
        }
      ]
    },
    "createdAt": "2026-05-20T10:30:00",
    "lastStudyAt": "2026-05-22T19:15:00",
    "completedAt": null
  }
}
```

### 状态码说明

| 状态码 | 说明               |
|-----|------------------|
| 200 | 请求成功             |
| 401 | 未认证或Token过期      |
| 403 | 无权访问该学习路径（非本人路径） |
| 404 | 指定的学习路径不存在       |
| 500 | 服务器内部错误          |

---

## 4. 获取个性化课程内容

根据学生当前掌握度，采用三级教学策略（基础夯实/强化突破/拓展提升）生成个性化课程内容。

### 基本信息

| 项目     | 内容                                                                                                                            |
|--------|-------------------------------------------------------------------------------------------------------------------------------|
| **路径** | `/api/learning/lesson/{pathId}/{nodeId}`                                                                                      |
| **方法** | `GET`                                                                                                                         |
| **说明** | 获取学习路径中某个知识点节点的个性化课程内容。系统根据学生掌握度自动选择教学策略：掌握度<0.4采用"基础夯实"策略，0.4-0.7采用"强化突破"策略，>0.7采用"拓展提升"策略。内容由教学Agent结合学生画像（认知风格、错误模式等）动态生成。 |
| **认证** | 需要 JWT Token                                                                                                                  |

### 请求参数

#### 路径参数（Path Parameters）

| 参数名    | 类型     | 必填 | 说明                  |
|--------|--------|----|---------------------|
| pathId | Long   | 是  | 学习路径ID              |
| nodeId | String | 是  | 节点ID（学习路径中的知识点节点标识） |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/learning/lesson/2001/node_001
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "nodeId": "node_001",
    "knowledgePoint": {
      "id": "math_average_rate_of_change",
      "name": "平均变化率与瞬时变化率",
      "module": "导数",
      "difficulty": 0.4
    },
    "studentMastery": 0.25,
    "teachingStrategy": "basic_consolidation",
    "teachingStrategyLabel": "基础夯实",
    "cognitiveStyle": "visual",
    "lesson": {
      "title": "平均变化率与瞬时变化率 — 从生活现象理解变化",
      "sections": [
        {
          "type": "concept",
          "title": "核心概念",
          "content": "## 什么是变化率？\n\n想象你骑自行车上学，从家到学校共3公里，用了15分钟。\n\n**平均速度** = 总路程 ÷ 总时间 = $\\frac{3}{15}$ = 0.2 km/min\n\n这就是一个**平均变化率**。\n\n用函数语言表达：设路程函数为 $s(t)$，则在时间段 $[t_1, t_2]$ 上的平均变化率为：\n\n$$\\frac{\\Delta s}{\\Delta t} = \\frac{s(t_2) - s(t_1)}{t_2 - t_1}$$\n\n**瞬时变化率**就是当 $\\Delta t \\to 0$ 时的极限值，它描述的是某一瞬间的变化快慢。",
          "mediaType": "text_with_latex"
        },
        {
          "type": "visualization",
          "title": "图形理解",
          "content": "观察下面的函数图像，割线斜率就是平均变化率，切线斜率就是瞬时变化率。",
          "chartConfig": {
            "type": "function_graph",
            "function": "x^2",
            "highlights": [
              { "type": "secant", "x1": 1, "x2": 3, "label": "平均变化率=割线斜率" },
              { "type": "tangent", "x": 2, "label": "瞬时变化率=切线斜率" }
            ]
          },
          "mediaType": "interactive_chart"
        },
        {
          "type": "example",
          "title": "典型例题",
          "content": "**例题**：已知函数 $f(x) = x^2$，求在区间 $[1, 3]$ 上的平均变化率。\n\n**解答**：\n\n$$\\frac{f(3) - f(1)}{3 - 1} = \\frac{9 - 1}{2} = 4$$\n\n所以 $f(x) = x^2$ 在 $[1,3]$ 上的平均变化率为 **4**。",
          "mediaType": "text_with_latex"
        },
        {
          "type": "key_points",
          "title": "易错提醒",
          "content": "1. 平均变化率不等于中点处的瞬时变化率（只有线性函数才相等）\n2. 变化率可以是负值，表示函数在递减\n3. 注意分母是自变量的变化量 $\\Delta x$，不是函数值的变化量",
          "mediaType": "text"
        }
      ],
      "exercises": [
        {
          "exerciseId": "ex_001",
          "type": "fill",
          "difficulty": 0.3,
          "content": "函数 $f(x) = 2x + 1$ 在区间 $[0, 2]$ 上的平均变化率为 ______。",
          "answer": "2",
          "hint": "代入公式 $\\frac{f(2)-f(0)}{2-0}$"
        },
        {
          "exerciseId": "ex_002",
          "type": "choice",
          "difficulty": 0.4,
          "content": "函数 $f(x) = x^2$ 在区间 $[1, 1+\\Delta x]$ 上的平均变化率为：",
          "options": [
            "A. $2 + \\Delta x$",
            "B. $2\\Delta x$",
            "C. $1 + \\Delta x$",
            "D. $2$"
          ],
          "answer": "A",
          "solution": "$\\frac{(1+\\Delta x)^2 - 1}{\\Delta x} = \\frac{2\\Delta x + (\\Delta x)^2}{\\Delta x} = 2 + \\Delta x$"
        },
        {
          "exerciseId": "ex_003",
          "type": "solve",
          "difficulty": 0.4,
          "content": "已知物体运动路程函数 $s(t) = 3t^2 + 2t$（单位：米，秒），求在 $t=1$ 到 $t=3$ 时间段内的平均速度。",
          "answer": "14 m/s",
          "solution": "平均速度 = $\\frac{s(3)-s(1)}{3-1} = \\frac{(27+6)-(3+2)}{2} = \\frac{28}{2} = 14$ m/s"
        }
      ],
      "totalExercises": 3,
      "ragSources": [
        { "source": "人教版高中数学选择性必修第二册", "chapter": "第五章 一元函数的导数及其应用", "page": "P2-P5" }
      ]
    },
    "generatedAt": "2026-05-23T10:35:00"
  }
}
```

### 三级教学策略说明

| 策略   | 掌握度范围     | 策略标识                  | 内容特点                         |
|------|-----------|-----------------------|------------------------------|
| 基础夯实 | < 0.4     | `basic_consolidation` | 概念重讲 + 直观案例 + 基础练习题，注重从零建立理解 |
| 强化突破 | 0.4 - 0.7 | `strengthening`       | 错因针对性讲解 + 变式训练，聚焦学生具体错误模式    |
| 拓展提升 | > 0.7     | `expansion`           | 综合应用 + 高考真题 + 能力迁移，提升综合运用能力  |

### 状态码说明

| 状态码 | 说明                |
|-----|-------------------|
| 200 | 课程内容生成成功          |
| 401 | 未认证或Token过期       |
| 403 | 无权访问该学习路径         |
| 404 | 路径或节点不存在          |
| 423 | 该节点尚未解锁（前置节点未完成）  |
| 500 | 服务器内部错误（如LLM调用失败） |

---

## 5. 提交练习答案

提交练习题答案，系统进行即时批改并通过LLM进行错因分析。

### 基本信息

| 项目               | 内容                                                                      |
|------------------|-------------------------------------------------------------------------|
| **路径**           | `/api/learning/exercise/submit`                                         |
| **方法**           | `POST`                                                                  |
| **说明**           | 提交学生的练习题答案，系统即时批改并调用LLM对错误答案进行错因分析（分析错在哪一步、属于哪种错误类型）。同时更新BKT模型中的知识点掌握度。 |
| **认证**           | 需要 JWT Token                                                            |
| **Content-Type** | `application/json`                                                      |

### 请求参数

#### 请求体（Request Body）

| 参数名              | 类型              | 必填 | 说明                     |
|------------------|-----------------|----|------------------------|
| pathId           | Long            | 是  | 学习路径ID                 |
| nodeId           | String          | 是  | 节点ID                   |
| exerciseId       | String          | 是  | 练习题ID                  |
| answer           | String          | 是  | 学生提交的答案                |
| solvingSteps     | Array\<String\> | 否  | 学生的解题步骤记录（用于LLM分析解题过程） |
| timeSpentSeconds | Integer         | 否  | 答题用时（秒）                |

### 请求体示例

```json
{
  "pathId": 2001,
  "nodeId": "node_001",
  "exerciseId": "ex_002",
  "answer": "B",
  "solvingSteps": [
    "f(1+Δx) = (1+Δx)^2 = 1 + 2Δx + Δx^2",
    "f(1) = 1",
    "平均变化率 = (f(1+Δx) - f(1)) / Δx = (2Δx + Δx^2) / Δx",
    "= 2Δx"
  ],
  "timeSpentSeconds": 120
}
```

### 响应体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "exerciseId": "ex_002",
    "isCorrect": false,
    "correctAnswer": "A",
    "studentAnswer": "B",
    "score": 0,
    "errorAnalysis": {
      "errorType": "calculation",
      "errorSubType": "约分错误",
      "errorStep": 4,
      "errorDescription": "在最后一步约分时，将 $(2\\Delta x + (\\Delta x)^2) / \\Delta x$ 错误地化简为 $2\\Delta x$。正确做法是分子每一项都除以 $\\Delta x$：$\\frac{2\\Delta x}{\\Delta x} + \\frac{(\\Delta x)^2}{\\Delta x} = 2 + \\Delta x$。",
      "suggestion": "约分时注意分子的每一项都需要除以分母，不能只对部分项进行约分。建议逐项展开后再约分。"
    },
    "correctSolution": "$$\\frac{(1+\\Delta x)^2 - 1}{\\Delta x} = \\frac{1 + 2\\Delta x + (\\Delta x)^2 - 1}{\\Delta x} = \\frac{2\\Delta x + (\\Delta x)^2}{\\Delta x} = 2 + \\Delta x$$\n\n答案选A。",
    "masteryUpdate": {
      "before": 0.25,
      "after": 0.22,
      "delta": -0.03
    },
    "encouragement": "这道题的关键在于多项式除以单项式要逐项相除，下次注意这个细节就好了！"
  }
}
```

### 状态码说明

| 状态码 | 说明                            |
|-----|-------------------------------|
| 200 | 提交成功，批改完成                     |
| 400 | 参数错误（如exerciseId不存在、answer为空） |
| 401 | 未认证或Token过期                   |
| 403 | 无权访问该学习路径                     |
| 404 | 路径、节点或练习题不存在                  |
| 423 | 该节点尚未解锁                       |
| 500 | 服务器内部错误                       |

---

## 6. 提交检查点测试

提交检查点测试答案（3-5题），判断学生是否达标（掌握度>=0.8），未通过将触发重新诊断。

### 基本信息

| 项目               | 内容                                                                                     |
|------------------|----------------------------------------------------------------------------------------|
| **路径**           | `/api/learning/checkpoint/submit`                                                      |
| **方法**           | `POST`                                                                                 |
| **说明**           | 提交知识点学习后的检查点测试（3-5题）。系统综合评估学生掌握度，判断是否达标（掌握度≥0.8）。通过则解锁下一节点；未通过则触发评估Agent重新诊断，可能调整学习路径。 |
| **认证**           | 需要 JWT Token                                                                           |
| **Content-Type** | `application/json`                                                                     |

### 请求参数

#### 请求体（Request Body）

| 参数名                        | 类型              | 必填 | 说明         |
|----------------------------|-----------------|----|------------|
| pathId                     | Long            | 是  | 学习路径ID     |
| nodeId                     | String          | 是  | 节点ID       |
| answers                    | Array\<Object\> | 是  | 答案数组（3-5题） |
| answers[].questionId       | String          | 是  | 题目ID       |
| answers[].answer           | String          | 是  | 学生答案       |
| answers[].solvingSteps     | Array\<String\> | 否  | 解题步骤       |
| answers[].timeSpentSeconds | Integer         | 否  | 答题用时（秒）    |
| totalTimeSeconds           | Integer         | 否  | 测试总用时（秒）   |

### 请求体示例

```json
{
  "pathId": 2001,
  "nodeId": "node_001",
  "answers": [
    {
      "questionId": "cp_001",
      "answer": "4",
      "solvingSteps": ["f(3)=9, f(1)=1", "(9-1)/(3-1)=4"],
      "timeSpentSeconds": 60
    },
    {
      "questionId": "cp_002",
      "answer": "A",
      "timeSpentSeconds": 45
    },
    {
      "questionId": "cp_003",
      "answer": "v(2)=12",
      "solvingSteps": ["s(t)=3t^2", "平均变化率=(s(2+Δt)-s(2))/Δt", "=(3(2+Δt)^2-12)/Δt", "=12+3Δt", "当Δt→0时，瞬时速度=12"],
      "timeSpentSeconds": 180
    },
    {
      "questionId": "cp_004",
      "answer": "C",
      "timeSpentSeconds": 50
    }
  ],
  "totalTimeSeconds": 335
}
```

### 响应体示例（通过）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "nodeId": "node_001",
    "knowledgePointId": "math_average_rate_of_change",
    "knowledgePointName": "平均变化率与瞬时变化率",
    "passed": true,
    "score": 0.85,
    "masteryAfter": 0.82,
    "masteryThreshold": 0.80,
    "totalQuestions": 4,
    "correctCount": 3,
    "results": [
      {
        "questionId": "cp_001",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_002",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_003",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_004",
        "isCorrect": false,
        "score": 0.0,
        "errorAnalysis": {
          "errorType": "concept",
          "errorSubType": "概念混淆",
          "errorDescription": "混淆了平均变化率和瞬时变化率的区别，在区间足够小时二者近似但不相等。"
        }
      }
    ],
    "nextAction": {
      "type": "unlock_next_node",
      "nextNodeId": "node_002",
      "nextKnowledgePointName": "导数的概念与几何意义",
      "message": "恭喜！你已掌握「平均变化率与瞬时变化率」，接下来学习「导数的概念与几何意义」。"
    },
    "pathProgress": 0.25,
    "completedAt": "2026-05-23T15:20:00"
  }
}
```

### 响应体示例（未通过）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathId": 2001,
    "nodeId": "node_001",
    "knowledgePointId": "math_average_rate_of_change",
    "knowledgePointName": "平均变化率与瞬时变化率",
    "passed": false,
    "score": 0.50,
    "masteryAfter": 0.55,
    "masteryThreshold": 0.80,
    "totalQuestions": 4,
    "correctCount": 2,
    "results": [
      {
        "questionId": "cp_001",
        "isCorrect": true,
        "score": 1.0
      },
      {
        "questionId": "cp_002",
        "isCorrect": false,
        "score": 0.0,
        "errorAnalysis": {
          "errorType": "concept",
          "errorSubType": "定义理解偏差",
          "errorDescription": "对平均变化率的几何意义（割线斜率）理解不清。"
        }
      },
      {
        "questionId": "cp_003",
        "isCorrect": false,
        "score": 0.0,
        "errorAnalysis": {
          "errorType": "calculation",
          "errorSubType": "运算法则错误",
          "errorDescription": "展开 $(2+\\Delta t)^2$ 时遗漏了交叉项 $2 \\cdot 2 \\cdot \\Delta t$。"
        }
      },
      {
        "questionId": "cp_004",
        "isCorrect": true,
        "score": 1.0
      }
    ],
    "nextAction": {
      "type": "re_diagnose",
      "message": "检查点未通过，系统将重新诊断该知识点的薄弱环节，为你调整学习内容。",
      "reDiagnosisReason": "掌握度0.55未达到阈值0.80，主要问题集中在几何意义理解和代数展开计算。",
      "suggestedActions": [
        "重新学习割线与切线的几何直观",
        "加强完全平方公式的计算练习"
      ]
    },
    "pathProgress": 0.00,
    "completedAt": null
  }
}
```

### 状态码说明

| 状态码 | 说明                          |
|-----|-----------------------------|
| 200 | 提交成功（无论是否通过检查点）             |
| 400 | 参数错误（如答案数组为空、题目数量不满足3-5题要求） |
| 401 | 未认证或Token过期                 |
| 403 | 无权访问该学习路径                   |
| 404 | 路径或节点不存在                    |
| 423 | 该节点尚未解锁或尚未完成课程学习            |
| 500 | 服务器内部错误                     |

---

## 7. SSE流式AI对话辅导

通过Server-Sent Events实现流式AI对话辅导，结合学生画像和当前学习上下文提供个性化回答。

### 基本信息

| 项目                   | 内容                                                                                                                                      |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **路径**               | `/api/chat/stream`                                                                                                                      |
| **方法**               | `GET`                                                                                                                                   |
| **说明**               | 基于SSE（Server-Sent Events）实现流式AI对话辅导。系统结合学生五维画像（知识状态、错误模式、认知风格等）和当前学习上下文（正在学习的知识点、最近的错题等），生成个性化的辅导回答。每个SSE事件携带一个token，前端逐token渲染实现打字机效果。 |
| **认证**               | 需要 JWT Token                                                                                                                            |
| **Content-Type（响应）** | `text/event-stream`                                                                                                                     |

### 请求参数

#### Query参数

| 参数名       | 类型     | 必填 | 说明                       |
|-----------|--------|----|--------------------------|
| message   | String | 是  | 学生发送的消息内容                |
| sessionId | String | 否  | 会话ID（用于多轮对话，首次对话不传则自动创建） |
| pathId    | Long   | 否  | 当前学习路径ID（提供学习上下文）        |
| nodeId    | String | 否  | 当前学习节点ID（提供知识点上下文）       |

### 请求体示例

无请求体（GET请求）。

请求示例：

```
GET /api/chat/stream?message=什么是瞬时变化率？和平均变化率有什么区别？&sessionId=sess_abc123&pathId=2001&nodeId=node_001
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Accept: text/event-stream
```

### 响应格式说明

响应为 `text/event-stream` 格式，每个事件包含一个token或控制信息：

**事件类型：**

| 事件类型       | 说明                         |
|------------|----------------------------|
| `message`  | 正常的文本token                 |
| `metadata` | 对话元数据（会话ID、上下文信息等，在流开始时发送） |
| `done`     | 流式输出结束信号                   |
| `error`    | 错误信息                       |

### 响应体示例（SSE流）

```
event: metadata
data: {"sessionId":"sess_abc123","knowledgeContext":"平均变化率与瞬时变化率","studentLevel":"basic_consolidation"}

event: message
data: {"token":"好"}

event: message
data: {"token":"问题"}

event: message
data: {"token":"！"}

event: message
data: {"token":"我"}

event: message
data: {"token":"用"}

event: message
data: {"token":"一个"}

event: message
data: {"token":"生活"}

event: message
data: {"token":"中的"}

event: message
data: {"token":"例子"}

event: message
data: {"token":"来帮"}

event: message
data: {"token":"你"}

event: message
data: {"token":"理解"}

event: message
data: {"token":"：\n\n"}

event: message
data: {"token":"想象"}

event: message
data: {"token":"你"}

event: message
data: {"token":"坐"}

event: message
data: {"token":"高铁"}

event: message
data: {"token":"从"}

event: message
data: {"token":"北京"}

event: message
data: {"token":"到"}

event: message
data: {"token":"上海"}

event: message
data: {"token":"，"}

event: message
data: {"token":"全程"}

event: message
data: {"token":"1318"}

event: message
data: {"token":"公里"}

event: message
data: {"token":"用了"}

event: message
data: {"token":"4.5"}

event: message
data: {"token":"小时"}

event: message
data: {"token":"。\n\n"}

event: message
data: {"token":"**平均变化率**"}

event: message
data: {"token":"就是"}

event: message
data: {"token":"整段"}

event: message
data: {"token":"旅程的"}

event: message
data: {"token":"平均"}

event: message
data: {"token":"速度"}

event: message
data: {"token":"：$\\frac{1318}{4.5} \\approx 293$ km/h\n\n"}

event: message
data: {"token":"**瞬时变化率**"}

event: message
data: {"token":"就是"}

event: message
data: {"token":"车厢"}

event: message
data: {"token":"显示屏上"}

event: message
data: {"token":"实时"}

event: message
data: {"token":"显示的"}

event: message
data: {"token":"速度"}

event: message
data: {"token":"，比如"}

event: message
data: {"token":"某一刻"}

event: message
data: {"token":"显示"}

event: message
data: {"token":"350 km/h"}

event: message
data: {"token":"。\n\n"}

event: message
data: {"token":"数学"}

event: message
data: {"token":"上的"}

event: message
data: {"token":"区别"}

event: message
data: {"token":"：\n- "}

event: message
data: {"token":"平均变化率"}

event: message
data: {"token":"= $\\frac{f(b)-f(a)}{b-a}$，描述一段区间上的整体变化趋势\n- "}

event: message
data: {"token":"瞬时变化率"}

event: message
data: {"token":"= $\\lim_{\\Delta x \\to 0}\\frac{f(x+\\Delta x)-f(x)}{\\Delta x}$，描述某一点的精确变化快慢"}

event: done
data: {"sessionId":"sess_abc123","totalTokens":256,"messageId":"msg_20260523_001"}

```

### 前端处理示例

```javascript
const eventSource = new EventSource(
  '/api/chat/stream?message=什么是瞬时变化率&sessionId=sess_abc123&pathId=2001&nodeId=node_001',
  { headers: { 'Authorization': 'Bearer ' + token } }
);

eventSource.addEventListener('message', (event) => {
  const data = JSON.parse(event.data);
  appendToChat(data.token); // 逐token追加到对话界面
});

eventSource.addEventListener('done', (event) => {
  eventSource.close();
});

eventSource.addEventListener('error', (event) => {
  console.error('SSE连接出错');
  eventSource.close();
});
```

### 状态码说明

| 状态码 | 说明                 |
|-----|--------------------|
| 200 | SSE连接建立成功，开始流式输出   |
| 400 | 参数错误（如message为空）   |
| 401 | 未认证或Token过期        |
| 429 | 请求过于频繁（限流）         |
| 500 | 服务器内部错误（如LLM服务不可用） |

### 注意事项

- 客户端需要设置 `Accept: text/event-stream` 请求头
- 连接超时时间建议设为60秒以上（LLM生成可能较慢）
- 异常断开时，可通过 `sessionId` 恢复上下文继续对话
- 每个SSE事件的 `data` 字段为JSON格式字符串
- 对话内容支持LaTeX数学公式（以 `$...$` 或 `$$...$$` 包裹）

---

## 8. 获取对话历史记录

获取学生的AI对话辅导历史记录，支持按会话查询。

### 基本信息

| 项目     | 内容                                                          |
|--------|-------------------------------------------------------------|
| **路径** | `/api/chat/history`                                         |
| **方法** | `GET`                                                       |
| **说明** | 获取学生与AI对话辅导的历史记录。支持按会话ID查询特定对话，也可获取所有会话列表。对话记录保存用于复习和上下文关联。 |
| **认证** | 需要 JWT Token                                                |

### 请求参数

#### Query参数

| 参数名       | 类型      | 必填 | 说明                             |
|-----------|---------|----|--------------------------------|
| sessionId | String  | 否  | 会话ID，传入则返回该会话的完整对话记录；不传则返回会话列表 |
| pathId    | Long    | 否  | 学习路径ID筛选（只返回与指定学习路径相关的对话）      |
| page      | Integer | 否  | 页码，默认1                         |
| size      | Integer | 否  | 每页条数，默认20                      |

### 请求体示例

无请求体（GET请求）。

请求示例（获取特定会话详情）：

```
GET /api/chat/history?sessionId=sess_abc123
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

请求示例（获取会话列表）：

```
GET /api/chat/history?page=1&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 响应体示例（会话列表，不传sessionId时）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 15,
    "page": 1,
    "size": 20,
    "sessions": [
      {
        "sessionId": "sess_abc123",
        "title": "关于瞬时变化率的疑问",
        "relatedPath": {
          "pathId": 2001,
          "knowledgePointName": "平均变化率与瞬时变化率"
        },
        "messageCount": 6,
        "lastMessage": "谢谢老师，我明白了！",
        "createdAt": "2026-05-23T10:30:00",
        "lastActiveAt": "2026-05-23T10:45:00"
      },
      {
        "sessionId": "sess_def456",
        "title": "导数计算公式推导",
        "relatedPath": {
          "pathId": 2001,
          "knowledgePointName": "导数的计算"
        },
        "messageCount": 4,
        "lastMessage": "那复合函数的求导法则呢？",
        "createdAt": "2026-05-22T19:00:00",
        "lastActiveAt": "2026-05-22T19:20:00"
      }
    ]
  }
}
```

### 响应体示例（特定会话详情，传入sessionId时）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "sess_abc123",
    "title": "关于瞬时变化率的疑问",
    "relatedPath": {
      "pathId": 2001,
      "targetKnowledgePoint": "导数的应用",
      "currentNode": "平均变化率与瞬时变化率"
    },
    "messages": [
      {
        "messageId": "msg_001",
        "role": "student",
        "content": "什么是瞬时变化率？和平均变化率有什么区别？",
        "timestamp": "2026-05-23T10:30:00"
      },
      {
        "messageId": "msg_002",
        "role": "ai",
        "content": "好问题！我用一个生活中的例子来帮你理解：\n\n想象你坐高铁从北京到上海，全程1318公里用了4.5小时。\n\n**平均变化率**就是整段旅程的平均速度：$\\frac{1318}{4.5} \\approx 293$ km/h\n\n**瞬时变化率**就是车厢显示屏上实时显示的速度，比如某一刻显示350 km/h。\n\n数学上的区别：\n- 平均变化率 = $\\frac{f(b)-f(a)}{b-a}$，描述一段区间上的整体变化趋势\n- 瞬时变化率 = $\\lim_{\\Delta x \\to 0}\\frac{f(x+\\Delta x)-f(x)}{\\Delta x}$，描述某一点的精确变化快慢",
        "timestamp": "2026-05-23T10:30:15"
      },
      {
        "messageId": "msg_003",
        "role": "student",
        "content": "那瞬时变化率是不是就是导数？",
        "timestamp": "2026-05-23T10:32:00"
      },
      {
        "messageId": "msg_004",
        "role": "ai",
        "content": "没错！你的直觉很好。\n\n**瞬时变化率就是导数的本质含义。**\n\n准确地说：函数 $f(x)$ 在 $x=x_0$ 处的导数 $f'(x_0)$ 就是函数在该点的瞬时变化率。\n\n$$f'(x_0) = \\lim_{\\Delta x \\to 0}\\frac{f(x_0+\\Delta x)-f(x_0)}{\\Delta x}$$\n\n几何意义上：\n- 平均变化率 → 割线的斜率\n- 瞬时变化率（导数）→ 切线的斜率\n\n当割线的两个点无限接近时，割线就变成了切线，平均变化率就变成了瞬时变化率（导数）。",
        "timestamp": "2026-05-23T10:32:20"
      },
      {
        "messageId": "msg_005",
        "role": "student",
        "content": "明白了，那导数为0代表什么？",
        "timestamp": "2026-05-23T10:35:00"
      },
      {
        "messageId": "msg_006",
        "role": "ai",
        "content": "很好的追问！\n\n导数为0（即 $f'(x_0) = 0$）意味着**函数在 $x_0$ 处的瞬时变化率为零**——函数值在这一瞬间"停止变化"了。\n\n几何上：切线斜率为0，即**切线是水平的**。\n\n这种点通常出现在：\n1. **极大值点**：函数从递增变为递减，"山顶"\n2. **极小值点**：函数从递减变为递增，"山谷"\n3. **拐点（驻点但非极值）**：如 $f(x)=x^3$ 在 $x=0$ 处\n\n⚠️ 注意：$f'(x_0)=0$ 是极值的**必要不充分条件**——导数为0不一定是极值点（如 $x^3$ 在原点），但极值点的导数一定为0（在导数存在的情况下）。\n\n这个知识点在后面学"导数与单调性""极值与最值"时会深入学习！",
        "timestamp": "2026-05-23T10:35:25"
      }
    ],
    "totalMessages": 6,
    "createdAt": "2026-05-23T10:30:00",
    "lastActiveAt": "2026-05-23T10:35:25"
  }
}
```

### 状态码说明

| 状态码 | 说明              |
|-----|-----------------|
| 200 | 请求成功            |
| 401 | 未认证或Token过期     |
| 404 | 指定的sessionId不存在 |
| 500 | 服务器内部错误         |

---

## 附录

### A. 节点状态（Node Status）枚举

| 状态值           | 说明           |
|---------------|--------------|
| `locked`      | 锁定状态，前置节点未完成 |
| `pending`     | 待学习，已解锁但尚未开始 |
| `in_progress` | 学习中          |
| `completed`   | 已完成（检查点通过）   |
| `failed`      | 检查点未通过，待重新学习 |

### B. 教学策略（Teaching Strategy）枚举

| 策略标识                  | 中文名称 | 触发条件（掌握度） | 教学方式               |
|-----------------------|------|-----------|--------------------|
| `basic_consolidation` | 基础夯实 | < 0.4     | 概念重讲 + 直观案例 + 基础练习 |
| `strengthening`       | 强化突破 | 0.4 - 0.7 | 错因针对性讲解 + 变式训练     |
| `expansion`           | 拓展提升 | > 0.7     | 综合应用 + 高考真题 + 能力迁移 |

### C. 错误类型（Error Type）枚举

| 错误大类  | 标识              | 子类示例               |
|-------|-----------------|--------------------|
| 概念性错误 | `concept`       | 定义理解偏差、概念混淆        |
| 计算性错误 | `calculation`   | 符号错误、运算法则错误、精度错误   |
| 逻辑性错误 | `logic`         | 充要条件误判、分类讨论遗漏、推理跳步 |
| 方法性错误 | `method`        | 方法选择不当、转化方向错误      |
| 审题性错误 | `comprehension` | 条件遗漏、问题误读          |

### D. 学习路径状态（Path Status）枚举

| 状态值         | 说明             |
|-------------|----------------|
| `active`    | 进行中            |
| `completed` | 已完成（所有节点通过检查点） |
| `paused`    | 已暂停            |

### E. 认知风格（Cognitive Style）枚举

| 标识        | 说明  | 教学内容适配           |
|-----------|-----|------------------|
| `visual`  | 视觉型 | 多用图表、函数图像、几何图形辅助 |
| `logical` | 逻辑型 | 多用严格推导、形式化证明     |
| `example` | 案例型 | 多用生活实例、具体数值计算引入  |
| `formula` | 公式型 | 多用公式归纳、结构化总结     |

---

<!-- Source: api-teacher-report.md -->

# SmartMentor API 文档 —— 教师端、效果报告与学习激励模块

> 本文档覆盖教师端班级管理、班级热力图、分层作业、学生预警、周报，以及学生端效果报告和学习激励相关接口。

---

## 全局约定

### 基础路径

```
https://{host}/api
```

### 认证方式

所有接口均需在请求头中携带 JWT Token：

```
Authorization: Bearer {token}
```

- 教师端接口（`/api/teacher/**`）要求用户角色为 **TEACHER**，否则返回 `403 Forbidden`。
- 学生端接口（`/api/report/**`、`/api/engagement/**`）要求用户角色为 **STUDENT**。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 通用错误码

| 状态码 | code | 说明             |
|-----|------|----------------|
| 200 | 200  | 请求成功           |
| 400 | 400  | 请求参数错误         |
| 401 | 401  | 未认证 / Token 过期 |
| 403 | 403  | 角色权限不足         |
| 404 | 404  | 资源不存在          |
| 500 | 500  | 服务器内部错误        |

---

## 一、教师端接口

---

### 1. 获取班级学生列表

获取当前教师所管理班级的学生列表及其学习概况。

- **路径**：`/api/teacher/class/students`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型      | 必填 | 说明                   |
|-----------|---------|----|----------------------|
| className | String  | 否  | 班级名称，不传则返回该教师所有班级的学生 |
| page      | Integer | 否  | 页码，默认 1              |
| pageSize  | Integer | 否  | 每页条数，默认 20，最大 100    |
| keyword   | String  | 否  | 搜索关键词，匹配学生姓名或用户名     |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 42,
    "page": 1,
    "pageSize": 20,
    "students": [
      {
        "studentId": 1001,
        "username": "xiaoming",
        "nickname": "小明",
        "grade": "高二",
        "school": "市第一中学",
        "className": "高二3班",
        "overallMastery": 0.65,
        "streakDays": 12,
        "level": 5,
        "lastActiveAt": "2026-05-22T18:30:00",
        "weakModules": ["导数", "解析几何"]
      },
      {
        "studentId": 1002,
        "username": "xiaohong",
        "nickname": "小红",
        "grade": "高二",
        "school": "市第一中学",
        "className": "高二3班",
        "overallMastery": 0.82,
        "streakDays": 30,
        "level": 8,
        "lastActiveAt": "2026-05-23T09:15:00",
        "weakModules": ["数列"]
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明         |
|-----|------------|
| 200 | 成功返回学生列表   |
| 401 | 未认证        |
| 403 | 非教师角色，无权访问 |

---

### 2. 添加学生到班级

将一个或多个学生添加到教师管理的班级中。

- **路径**：`/api/teacher/class/students`
- **方法**：`POST`
- **角色要求**：TEACHER
- **Content-Type**：`application/json`

#### 请求参数（Body）

| 参数名        | 类型       | 必填 | 说明                             |
|------------|----------|----|--------------------------------|
| className  | String   | 是  | 目标班级名称                         |
| studentIds | Long[]   | 否  | 学生ID列表（与 usernames 二选一，至少提供一个） |
| usernames  | String[] | 否  | 学生用户名列表（与 studentIds 二选一）      |

#### 请求体示例 JSON

```json
{
  "className": "高二3班",
  "studentIds": [1001, 1002, 1003],
  "usernames": []
}
```

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "added": 2,
    "skipped": 1,
    "details": [
      { "studentId": 1001, "status": "added" },
      { "studentId": 1002, "status": "added" },
      { "studentId": 1003, "status": "already_exists" }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                              |
|-----|---------------------------------|
| 200 | 添加完成（含部分已存在的情况）                 |
| 400 | 参数缺失，studentIds 和 usernames 均为空 |
| 401 | 未认证                             |
| 403 | 非教师角色，无权操作                      |
| 404 | 指定的学生ID不存在                      |

---

### 3. 班级薄弱知识热力图

返回各知识点在班级中的平均掌握度，用于可视化薄弱知识热力图。颜色编码规则：红色（< 0.5）、黄色（0.5 - 0.8）、绿色（>= 0.8）。

- **路径**：`/api/teacher/class/heatmap`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型     | 必填 | 说明                                         |
|-----------|--------|----|--------------------------------------------|
| className | String | 否  | 班级名称，不传则汇总该教师所有班级                          |
| module    | String | 否  | 数学模块筛选，如"导数""解析几何"，不传返回全部模块                |
| groupBy   | String | 否  | 分组方式：`none`（默认，班级整体）/ `level`（按优秀/中等/薄弱分组） |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "className": "高二3班",
    "totalStudents": 42,
    "generatedAt": "2026-05-23T10:00:00",
    "colorRule": {
      "red": "< 0.5",
      "yellow": "0.5 - 0.8",
      "green": ">= 0.8"
    },
    "modules": [
      {
        "module": "函数",
        "avgMastery": 0.72,
        "color": "yellow",
        "knowledgePoints": [
          {
            "knowledgePointId": "math_function_concept",
            "name": "函数的概念与表示法",
            "avgMastery": 0.85,
            "color": "green",
            "weakStudentCount": 3
          },
          {
            "knowledgePointId": "math_function_monotonicity",
            "name": "单调性与最值",
            "avgMastery": 0.68,
            "color": "yellow",
            "weakStudentCount": 12
          }
        ]
      },
      {
        "module": "导数",
        "avgMastery": 0.43,
        "color": "red",
        "knowledgePoints": [
          {
            "knowledgePointId": "math_derivative_concept",
            "name": "导数的概念与几何意义",
            "avgMastery": 0.55,
            "color": "yellow",
            "weakStudentCount": 18
          },
          {
            "knowledgePointId": "math_derivative_application",
            "name": "导数的应用",
            "avgMastery": 0.35,
            "color": "red",
            "weakStudentCount": 28
          }
        ]
      },
      {
        "module": "三角函数",
        "avgMastery": 0.38,
        "color": "red",
        "knowledgePoints": [
          {
            "knowledgePointId": "math_trig_definition",
            "name": "三角函数定义",
            "avgMastery": 0.60,
            "color": "yellow",
            "weakStudentCount": 15
          },
          {
            "knowledgePointId": "math_trig_sum_diff",
            "name": "和差角公式",
            "avgMastery": 0.30,
            "color": "red",
            "weakStudentCount": 32
          }
        ]
      }
    ],
    "groups": [
      {
        "groupName": "A组(优秀)",
        "studentCount": 10,
        "moduleMasteries": {
          "函数": 0.91,
          "导数": 0.72,
          "三角函数": 0.65
        }
      },
      {
        "groupName": "B组(中等)",
        "studentCount": 20,
        "moduleMasteries": {
          "函数": 0.70,
          "导数": 0.42,
          "三角函数": 0.38
        }
      },
      {
        "groupName": "C组(薄弱)",
        "studentCount": 12,
        "moduleMasteries": {
          "函数": 0.48,
          "导数": 0.22,
          "三角函数": 0.18
        }
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明         |
|-----|------------|
| 200 | 成功返回热力图数据  |
| 401 | 未认证        |
| 403 | 非教师角色，无权访问 |

---

### 4. AI 生成分层作业

根据班级学生掌握度自动生成基础 / 提高 / 拓展三层难度的作业。系统根据每位学生的知识点掌握度自动将其分到对应层级。

分层规则：

- **基础层**：掌握度 < 0.5，题目侧重概念巩固
- **提高层**：掌握度 0.5 - 0.8，题目侧重综合应用
- **拓展层**：掌握度 > 0.8，题目侧重高考真题 / 竞赛变式

- **路径**：`/api/teacher/homework/generate`
- **方法**：`POST`
- **角色要求**：TEACHER
- **Content-Type**：`application/json`

#### 请求参数（Body）

| 参数名               | 类型       | 必填 | 说明               |
|-------------------|----------|----|------------------|
| className         | String   | 是  | 班级名称             |
| knowledgePointIds | String[] | 是  | 目标知识点ID列表，至少1个   |
| questionsPerLayer | Integer  | 否  | 每层题目数，默认 5       |
| deadline          | String   | 否  | 截止时间，ISO 8601 格式 |
| note              | String   | 否  | 教师备注 / 附加要求      |

#### 请求体示例 JSON

```json
{
  "className": "高二3班",
  "knowledgePointIds": [
    "math_derivative_calc",
    "math_derivative_application"
  ],
  "questionsPerLayer": 5,
  "deadline": "2026-05-26T23:59:59",
  "note": "请注意导数计算中链式法则的考查"
}
```

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "homeworkId": "hw_20260523_001",
    "className": "高二3班",
    "knowledgePoints": ["导数的计算", "导数的应用"],
    "deadline": "2026-05-26T23:59:59",
    "generatedAt": "2026-05-23T10:05:00",
    "layers": [
      {
        "layer": "基础层",
        "description": "概念巩固，注重基础计算和定义理解",
        "masteryRange": "< 0.5",
        "assignedStudents": [
          { "studentId": 1005, "nickname": "小刚", "mastery": 0.32 },
          { "studentId": 1008, "nickname": "小丽", "mastery": 0.41 }
        ],
        "questions": [
          {
            "questionId": 501,
            "type": "choice",
            "difficulty": 0.3,
            "knowledgePointId": "math_derivative_calc",
            "content": "求函数 $f(x) = x^3 - 2x + 1$ 的导数 $f'(x)$ 为：",
            "options": [
              "A. $3x^2 - 2$",
              "B. $3x^2 + 2$",
              "C. $x^2 - 2$",
              "D. $3x - 2$"
            ],
            "answer": "A",
            "source": "textbook"
          },
          {
            "questionId": 502,
            "type": "fill",
            "difficulty": 0.35,
            "knowledgePointId": "math_derivative_calc",
            "content": "已知 $f(x) = \\sin x + \\cos x$，则 $f'(\\frac{\\pi}{4})$ = ______。",
            "answer": "0",
            "source": "textbook"
          }
        ]
      },
      {
        "layer": "提高层",
        "description": "综合应用，注重方法选择和规范解题",
        "masteryRange": "0.5 - 0.8",
        "assignedStudents": [
          { "studentId": 1001, "nickname": "小明", "mastery": 0.65 },
          { "studentId": 1003, "nickname": "小华", "mastery": 0.58 }
        ],
        "questions": [
          {
            "questionId": 503,
            "type": "solve",
            "difficulty": 0.6,
            "knowledgePointId": "math_derivative_application",
            "content": "已知函数 $f(x) = x^3 - 3x^2 + 4$，求 $f(x)$ 的单调递增区间和单调递减区间。",
            "answer": "单调递增区间为 $(-\\infty, 0)$ 和 $(2, +\\infty)$；单调递减区间为 $(0, 2)$。",
            "source": "mock"
          }
        ]
      },
      {
        "layer": "拓展层",
        "description": "高考真题与竞赛变式，注重创新思维与综合能力",
        "masteryRange": "> 0.8",
        "assignedStudents": [
          { "studentId": 1002, "nickname": "小红", "mastery": 0.88 },
          { "studentId": 1010, "nickname": "小强", "mastery": 0.92 }
        ],
        "questions": [
          {
            "questionId": 504,
            "type": "solve",
            "difficulty": 0.85,
            "knowledgePointId": "math_derivative_application",
            "content": "已知函数 $f(x) = e^x - ax - a$，讨论 $f(x)$ 零点的个数。（2024年全国甲卷）",
            "answer": "当 $a \\leq 0$ 时，$f(x)$ 有且仅有一个零点；当 $0 < a < 1$ 时无零点；当 $a = 1$ 时有一个零点；当 $a > 1$ 时有两个零点。",
            "source": "gaokao",
            "gaokaoYear": 2024
          }
        ]
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明                |
|-----|-------------------|
| 200 | 分层作业生成成功          |
| 400 | 参数错误，如知识点ID列表为空   |
| 401 | 未认证               |
| 403 | 非教师角色，无权操作        |
| 404 | 指定班级不存在           |
| 500 | AI 生成失败（如大模型调用超时） |

---

### 5. 学生预警列表

获取需要教师关注的异常学生列表，包括连续不登录、成绩下滑、学习路径停滞等情况。

- **路径**：`/api/teacher/alerts`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型      | 必填 | 说明                                                                 |
|-----------|---------|----|--------------------------------------------------------------------|
| className | String  | 否  | 班级名称筛选                                                             |
| alertType | String  | 否  | 预警类型筛选：`inactive`（不活跃）/ `decline`（成绩下滑）/ `stagnant`（学习停滞）/ 不传则返回全部 |
| page      | Integer | 否  | 页码，默认 1                                                            |
| pageSize  | Integer | 否  | 每页条数，默认 20                                                         |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "pageSize": 20,
    "alerts": [
      {
        "alertId": "alert_001",
        "studentId": 1005,
        "nickname": "小刚",
        "className": "高二3班",
        "alertType": "inactive",
        "severity": "high",
        "title": "连续7天未登录",
        "description": "该学生上次登录时间为2026-05-16，已连续7天未登录系统，学习路径中断。",
        "lastActiveAt": "2026-05-16T20:30:00",
        "suggestion": "建议联系该学生了解情况，并鼓励其恢复学习。",
        "createdAt": "2026-05-23T08:00:00"
      },
      {
        "alertId": "alert_002",
        "studentId": 1008,
        "nickname": "小丽",
        "className": "高二3班",
        "alertType": "decline",
        "severity": "medium",
        "title": "导数模块掌握度下滑",
        "description": "该学生导数模块近两周掌握度从0.62下降至0.45，下降幅度27.4%。",
        "masteryBefore": 0.62,
        "masteryAfter": 0.45,
        "declineRate": 0.274,
        "suggestion": "建议关注其导数模块学习情况，可安排针对性辅导。",
        "createdAt": "2026-05-23T08:00:00"
      },
      {
        "alertId": "alert_003",
        "studentId": 1012,
        "nickname": "小伟",
        "className": "高二3班",
        "alertType": "stagnant",
        "severity": "low",
        "title": "学习路径停滞5天",
        "description": "该学生当前学习路径'三角函数突破'已5天无进展，进度停留在40%。",
        "pathName": "三角函数突破",
        "pathProgress": 0.40,
        "stagnantDays": 5,
        "suggestion": "建议了解学习卡点，该学生可能在'和差角公式'处遇到困难。",
        "createdAt": "2026-05-23T08:00:00"
      }
    ]
  }
}
```

#### 状态码说明

| 状态码 | 说明         |
|-----|------------|
| 200 | 成功返回预警列表   |
| 401 | 未认证        |
| 403 | 非教师角色，无权访问 |

---

### 6. 班级学习情况周报

获取 AI 生成的班级学习情况周报，包含本周学习数据汇总、进步学生、需关注学生、班级薄弱点及教学建议。

- **路径**：`/api/teacher/report/weekly`
- **方法**：`GET`
- **角色要求**：TEACHER

#### 请求参数（Query）

| 参数名       | 类型     | 必填 | 说明                          |
|-----------|--------|----|-----------------------------|
| className | String | 是  | 班级名称                        |
| weekStart | String | 否  | 周起始日期，格式 `yyyy-MM-dd`，默认本周一 |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "className": "高二3班",
    "weekStart": "2026-05-18",
    "weekEnd": "2026-05-24",
    "generatedAt": "2026-05-23T10:30:00",
    "summary": {
      "totalStudents": 42,
      "activeStudents": 38,
      "activeRate": 0.905,
      "totalStudyHours": 156.5,
      "avgStudyHoursPerStudent": 4.12,
      "totalQuestionsAnswered": 1260,
      "avgAccuracy": 0.68,
      "avgMasteryChange": 0.05
    },
    "topProgressStudents": [
      {
        "studentId": 1001,
        "nickname": "小明",
        "masteryBefore": 0.55,
        "masteryAfter": 0.72,
        "improvement": 0.17,
        "highlights": "导数模块掌握度从0.40提升至0.68，进步显著"
      },
      {
        "studentId": 1015,
        "nickname": "小芳",
        "masteryBefore": 0.60,
        "masteryAfter": 0.75,
        "improvement": 0.15,
        "highlights": "完成三角函数学习路径，连续打卡7天"
      }
    ],
    "needAttentionStudents": [
      {
        "studentId": 1005,
        "nickname": "小刚",
        "issue": "本周未登录",
        "suggestion": "联系了解情况"
      },
      {
        "studentId": 1008,
        "nickname": "小丽",
        "issue": "导数掌握度下滑0.17",
        "suggestion": "安排课后辅导"
      }
    ],
    "classWeakPoints": [
      {
        "knowledgePointId": "math_trig_sum_diff",
        "name": "和差角公式",
        "avgMastery": 0.30,
        "color": "red",
        "suggestion": "建议课堂安排专项复习"
      },
      {
        "knowledgePointId": "math_derivative_application",
        "name": "导数的应用",
        "avgMastery": 0.38,
        "color": "red",
        "suggestion": "建议分层布置作业强化训练"
      }
    ],
    "aiTeachingSuggestion": "本周班级在导数和三角函数两个模块的表现较弱，建议下周课堂重点讲解'和差角公式'的推导与应用，并通过分层作业对导数应用进行强化训练。活跃度方面有4名学生本周未登录，建议及时了解情况。"
  }
}
```

#### 状态码说明

| 状态码 | 说明            |
|-----|---------------|
| 200 | 成功返回周报数据      |
| 400 | 参数错误，如日期格式不正确 |
| 401 | 未认证           |
| 403 | 非教师角色，无权访问    |
| 404 | 指定班级不存在       |

---

## 二、学生端 —— 效果报告接口

---

### 7. 学习效果报告

获取学生的学习效果报告，包含前测-后测对比、掌握度变化趋势、错误消除率和能力雷达图数据。对应项目设计中"四维效果评估"
体系（掌握提升、学习曲线、错误消除、能力雷达）。

- **路径**：`/api/report/effectiveness`
- **方法**：`GET`
- **角色要求**：STUDENT

#### 请求参数（Query）

| 参数名    | 类型     | 必填 | 说明                                                   |
|--------|--------|----|------------------------------------------------------|
| module | String | 否  | 数学模块筛选，如"导数""三角函数"，不传返回全部                            |
| period | String | 否  | 时间范围：`week`（近一周）/ `month`（近一月）/ `all`（全部），默认 `month` |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "generatedAt": "2026-05-23T11:00:00",
    "period": "month",
    "overallSummary": {
      "masteryBefore": 0.28,
      "masteryAfter": 0.82,
      "improvementRate": 1.928,
      "totalStudyHours": 18.5,
      "totalQuestionsAnswered": 156,
      "accuracy": 0.74
    },
    "masteryComparison": [
      {
        "knowledgePointId": "math_derivative_application",
        "name": "导数的应用",
        "module": "导数",
        "preTestMastery": 0.25,
        "postTestMastery": 0.82,
        "improvement": 0.57
      },
      {
        "knowledgePointId": "math_derivative_calc",
        "name": "导数的计算",
        "module": "导数",
        "preTestMastery": 0.40,
        "postTestMastery": 0.88,
        "improvement": 0.48
      },
      {
        "knowledgePointId": "math_change_rate",
        "name": "平均变化率与瞬时变化率",
        "module": "导数",
        "preTestMastery": 0.20,
        "postTestMastery": 0.75,
        "improvement": 0.55
      }
    ],
    "masteryCurve": [
      { "date": "2026-04-23", "mastery": 0.28 },
      { "date": "2026-04-30", "mastery": 0.38 },
      { "date": "2026-05-07", "mastery": 0.52 },
      { "date": "2026-05-14", "mastery": 0.68 },
      { "date": "2026-05-21", "mastery": 0.82 }
    ],
    "errorElimination": [
      {
        "errorType": "计算性错误",
        "subType": "符号错误",
        "countBefore": 15,
        "countAfter": 3,
        "eliminationRate": 0.80
      },
      {
        "errorType": "概念性错误",
        "subType": "概念混淆",
        "countBefore": 8,
        "countAfter": 1,
        "eliminationRate": 0.875
      },
      {
        "errorType": "计算性错误",
        "subType": "运算法则错误",
        "countBefore": 6,
        "countAfter": 2,
        "eliminationRate": 0.667
      }
    ],
    "abilityRadar": {
      "dimensions": ["函数", "导数", "三角函数", "向量", "数列", "解析几何"],
      "before": [0.55, 0.28, 0.42, 0.60, 0.70, 0.35],
      "after":  [0.72, 0.82, 0.55, 0.68, 0.75, 0.50]
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明       |
|-----|----------|
| 200 | 成功返回效果报告 |
| 401 | 未认证      |
| 403 | 非学生角色    |

---

### 8. 学生首页仪表盘数据

获取学生首页仪表盘所需的全部数据，包含今日任务概要、连续打卡天数、各模块掌握度（雷达图数据）和最近学习动态。

- **路径**：`/api/report/dashboard`
- **方法**：`GET`
- **角色要求**：STUDENT

#### 请求参数（Query）

无。

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1001,
    "nickname": "小明",
    "level": 5,
    "levelTitle": "数学进阶者",
    "experiencePoints": 2350,
    "nextLevelExp": 3000,
    "streakDays": 12,
    "todayStudyMinutes": 35,
    "totalStudyHours": 48.5,
    "todayTasks": {
      "total": 3,
      "completed": 1,
      "tasks": [
        {
          "missionId": "mission_daily_001",
          "title": "完成1组诊断测试",
          "type": "diagnostic",
          "status": "completed",
          "rewardExp": 50
        },
        {
          "missionId": "mission_daily_002",
          "title": "学习路径推进1个知识点",
          "type": "learning",
          "status": "pending",
          "rewardExp": 80
        },
        {
          "missionId": "mission_daily_003",
          "title": "复习昨日错题",
          "type": "review",
          "status": "pending",
          "rewardExp": 30
        }
      ]
    },
    "moduleMastery": {
      "dimensions": ["函数", "导数", "三角函数", "向量", "数列", "解析几何"],
      "values": [0.72, 0.65, 0.55, 0.68, 0.75, 0.42]
    },
    "recentActivities": [
      {
        "date": "2026-05-23",
        "activities": [
          {
            "type": "diagnostic",
            "description": "完成导数模块诊断测试",
            "duration": "15分钟",
            "result": "掌握度从0.60提升至0.65"
          }
        ]
      },
      {
        "date": "2026-05-22",
        "activities": [
          {
            "type": "lesson",
            "description": "学习知识点：导数的计算",
            "duration": "25分钟",
            "result": "通过检查点测试"
          },
          {
            "type": "exercise",
            "description": "完成导数计算练习",
            "duration": "20分钟",
            "result": "正确率 4/5"
          }
        ]
      }
    ],
    "currentPath": {
      "pathId": 101,
      "name": "导数突破之路",
      "progress": 0.60,
      "currentNode": "导数与单调性",
      "totalNodes": 5,
      "completedNodes": 3
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明        |
|-----|-----------|
| 200 | 成功返回仪表盘数据 |
| 401 | 未认证       |
| 403 | 非学生角色     |

---

## 三、学生端 —— 学习激励接口

---

### 9. 每日任务列表

获取当日的任务清单。每天系统自动生成3个可选任务（诊断 / 练习 / 复习），完成后可获得经验值。

- **路径**：`/api/engagement/missions`
- **方法**：`GET`
- **角色要求**：STUDENT

#### 请求参数（Query）

| 参数名  | 类型     | 必填 | 说明                        |
|------|--------|----|---------------------------|
| date | String | 否  | 查询日期，格式 `yyyy-MM-dd`，默认今天 |

#### 请求体示例 JSON

无（GET 请求）

#### 响应体示例 JSON

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "date": "2026-05-23",
    "streakDays": 12,
    "todayExpEarned": 50,
    "todayExpAvailable": 160,
    "missions": [
      {
        "missionId": "mission_20260523_diag",
        "title": "完成1组诊断测试",
        "description": "选择任意模块完成一次诊断测试（5-8题）",
        "type": "diagnostic",
        "rewardExp": 50,
        "status": "completed",
        "completedAt": "2026-05-23T09:30:00"
      },
      {
        "missionId": "mission_20260523_learn",
        "title": "学习路径推进1个知识点",
        "description": "在当前学习路径中完成一个知识点的学习并通过检查点测试",
        "type": "learning",
        "rewardExp": 80,
        "status": "pending",
        "completedAt": null
      },
      {
        "missionId": "mission_20260523_review",
        "title": "复习昨日错题",
        "description": "重做昨天做错的题目，巩固薄弱点",
        "type": "review",
        "rewardExp": 30,
        "status": "pending",
        "completedAt": null
      }
    ],
    "streakBonus": {
      "currentStreak": 12,
      "nextMilestone": 14,
      "nextMilestoneReward": "额外100经验值 + '两周坚持'成就勋章"
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明       |
|-----|----------|
| 200 | 成功返回任务列表 |
| 401 | 未认证      |
| 403 | 非学生角色    |

---

### 10. 完成任务获得经验值

标记指定任务为已完成，系统验证完成条件后发放经验值奖励，并更新学生成长等级。

- **路径**：`/api/engagement/missions/{missionId}/complete`
- **方法**：`POST`
- **角色要求**：STUDENT

#### 请求参数

**路径参数**：

| 参数名       | 类型     | 必填 | 说明   |
|-----------|--------|----|------|
| missionId | String | 是  | 任务ID |

**请求体参数（Body）**：

| 参数名                       | 类型     | 必填 | 说明                         |
|---------------------------|--------|----|----------------------------|
| evidence                  | Object | 否  | 完成凭据，系统也会自动校验，此字段用于客户端辅助上报 |
| evidence.sessionId        | String | 否  | 关联的诊断/学习会话ID               |
| evidence.knowledgePointId | String | 否  | 关联的知识点ID                   |

#### 请求体示例 JSON

```json
{
  "evidence": {
    "sessionId": "diag_session_20260523_001",
    "knowledgePointId": "math_derivative_calc"
  }
}
```

#### 响应体示例 JSON

**成功完成**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "missionId": "mission_20260523_learn",
    "status": "completed",
    "completedAt": "2026-05-23T14:20:00",
    "rewardExp": 80,
    "expSummary": {
      "previousExp": 2350,
      "earnedExp": 80,
      "currentExp": 2430,
      "nextLevelExp": 3000
    },
    "levelUp": false,
    "currentLevel": 5,
    "currentLevelTitle": "数学进阶者",
    "streakDays": 12,
    "newAchievements": [],
    "todayProgress": {
      "completedMissions": 2,
      "totalMissions": 3,
      "todayTotalExp": 130
    }
  }
}
```

**触发升级和成就**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "missionId": "mission_20260523_review",
    "status": "completed",
    "completedAt": "2026-05-23T20:10:00",
    "rewardExp": 30,
    "expSummary": {
      "previousExp": 2980,
      "earnedExp": 30,
      "bonusExp": 100,
      "currentExp": 3110,
      "nextLevelExp": 4500
    },
    "levelUp": true,
    "previousLevel": 5,
    "currentLevel": 6,
    "currentLevelTitle": "数学挑战者",
    "streakDays": 14,
    "newAchievements": [
      {
        "achievementId": "ach_streak_14",
        "title": "两周坚持",
        "description": "连续学习14天",
        "icon": "streak_14",
        "bonusExp": 100
      }
    ],
    "todayProgress": {
      "completedMissions": 3,
      "totalMissions": 3,
      "todayTotalExp": 260
    }
  }
}
```

#### 状态码说明

| 状态码 | 说明                    |
|-----|-----------------------|
| 200 | 任务完成，经验值发放成功          |
| 400 | 任务已完成，不可重复提交；或完成条件不满足 |
| 401 | 未认证                   |
| 403 | 非学生角色                 |
| 404 | 任务ID不存在               |

---

## 附录：字段枚举值参考

### 预警类型（alertType）

| 值          | 说明         |
|------------|------------|
| `inactive` | 连续多天未登录    |
| `decline`  | 掌握度 / 成绩下滑 |
| `stagnant` | 学习路径停滞无进展  |

### 预警严重程度（severity）

| 值        | 说明              |
|----------|-----------------|
| `high`   | 高危（如连续7天以上未登录）  |
| `medium` | 中度（如掌握度下滑超过15%） |
| `low`    | 低度（如学习路径停滞3-5天） |

### 任务类型（mission type）

| 值            | 说明     |
|--------------|--------|
| `diagnostic` | 完成诊断测试 |
| `learning`   | 学习路径推进 |
| `review`     | 错题复习   |

### 任务状态（mission status）

| 值           | 说明  |
|-------------|-----|
| `pending`   | 待完成 |
| `completed` | 已完成 |

### 热力图颜色编码

| 颜色         | 掌握度区间     | 含义       |
|------------|-----------|----------|
| 红色（red）    | < 0.5     | 薄弱，需重点关注 |
| 黄色（yellow） | 0.5 - 0.8 | 一般，有提升空间 |
| 绿色（green）  | >= 0.8    | 良好，已基本掌握 |

### 分层作业层级

| 层级  | 掌握度区间     | 题目特点             |
|-----|-----------|------------------|
| 基础层 | < 0.5     | 概念巩固题，注重基础       |
| 提高层 | 0.5 - 0.8 | 综合应用题，注重方法       |
| 拓展层 | > 0.8     | 高考真题 / 竞赛变式，注重创新 |

### 成长等级体系

| 等级   | 称号    | 所需经验值 |
|------|-------|-------|
| Lv1  | 数学学徒  | 0     |
| Lv2  | 数学入门者 | 200   |
| Lv3  | 数学探索者 | 500   |
| Lv4  | 数学实践者 | 1000  |
| Lv5  | 数学进阶者 | 2000  |
| Lv6  | 数学挑战者 | 3000  |
| Lv7  | 数学突破者 | 5000  |
| Lv8  | 数学达人  | 8000  |
| Lv9  | 数学高手  | 12000 |
| Lv10 | 数学学霸  | 20000 |

---
