-- ############################################################
-- SmartMentor 数据库脚本合集（由以下 3 个脚本按执行顺序合并）
--   1) smartmentor.sql
--   2) SmartMentor_A3_profile_migration.sql
--   3) SmartMentor_A3_cleanup_legacy_data.sql
-- 执行顺序：先建库建表，再做画像字段迁移，最后按需执行清理。
-- 注意：第 3 部分为破坏性清理，仅在需要重置学生学习数据时执行。
-- ############################################################


-- ############################################################
-- 第 1 部分 / 建库建表 + 初始数据（基础结构，最先执行）
-- 源文件：smartmentor.sql
-- ############################################################

-- ============================================================
-- SmartMentor 高校课程 AI 伴学平台 - MySQL 数据库初始化脚本
-- 适用版本: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

DROP DATABASE IF EXISTS smartmentor;
CREATE DATABASE smartmentor
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE smartmentor;

-- ============================================================
-- 1. 用户与认证
-- ============================================================

-- 学生表
CREATE TABLE student (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
  password VARCHAR(128) NOT NULL COMMENT '密码（bcrypt哈希）',
  nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  grade VARCHAR(20) DEFAULT NULL COMMENT '学历层次，如 高职/本科/研究生',
  school VARCHAR(100) DEFAULT NULL COMMENT '学校',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  role VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色标识',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB COMMENT='学生用户表';

-- 教师表
CREATE TABLE teacher (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
  password VARCHAR(128) NOT NULL COMMENT '密码（bcrypt哈希）',
  name VARCHAR(50) DEFAULT NULL COMMENT '教师姓名',
  school VARCHAR(100) DEFAULT NULL COMMENT '学校',
  role VARCHAR(20) NOT NULL DEFAULT 'teacher' COMMENT '角色标识',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB COMMENT='教师用户表';

-- 短信验证码
CREATE TABLE sms_code (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL,
  code VARCHAR(10) NOT NULL,
  purpose VARCHAR(20) NOT NULL DEFAULT 'login' COMMENT 'login/register/reset',
  used TINYINT(1) NOT NULL DEFAULT 0,
  expired_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_phone_purpose (phone, purpose)
) ENGINE=InnoDB COMMENT='短信验证码表';

-- ============================================================
-- 2. 学生画像
-- ============================================================

-- 学生五维画像
CREATE TABLE student_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  -- 五维画像
  knowledge_state DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '知识状态维度 0-1',
  error_pattern DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '错误模式维度 0-1',
  learning_behavior DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '学习行为维度 0-1',
  cognitive_style DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '认知风格维度 0-1',
  goal_profile DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '目标画像维度 0-1',
  -- 学习偏好
  learning_style VARCHAR(20) DEFAULT NULL COMMENT '学习风格: visual/auditory/kinesthetic',
  daily_study_minutes INT DEFAULT 0 COMMENT '每日学习时长(分钟)',
  preferred_time_slot VARCHAR(20) DEFAULT NULL COMMENT '偏好时段: morning/afternoon/evening',
  target_school VARCHAR(100) DEFAULT NULL COMMENT '目标院校',
  target_score INT DEFAULT NULL COMMENT '目标分数或目标等级',
  major_direction VARCHAR(50) DEFAULT NULL COMMENT '专业方向',
  education_level VARCHAR(20) DEFAULT NULL COMMENT '学历层次',
  current_course VARCHAR(100) DEFAULT NULL COMMENT '当前课程',
  learning_goal VARCHAR(50) DEFAULT NULL COMMENT '学习目标',
  foundation_level VARCHAR(20) DEFAULT NULL COMMENT '基础水平',
  resource_preference JSON DEFAULT NULL COMMENT '资源偏好',
  academic_interest VARCHAR(255) DEFAULT NULL COMMENT '学术或职业兴趣',
  weak_module_priority JSON DEFAULT NULL COMMENT '薄弱课程模块优先级',
  study_mode VARCHAR(20) DEFAULT 'systematic' COMMENT '学习模式',
  -- 全局能力参数
  overall_mastery DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '综合掌握度 0-1',
  ability_param DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'IRT能力参数θ',
  error_patterns JSON DEFAULT NULL COMMENT '错误模式统计JSON',
  knowledge_state_json JSON DEFAULT NULL COMMENT '知识状态快照JSON',
  -- 游戏化
  streak_days INT NOT NULL DEFAULT 0 COMMENT '连续学习天数',
  total_study_hours DECIMAL(10,1) NOT NULL DEFAULT 0.0 COMMENT '累计学习小时数',
  level INT NOT NULL DEFAULT 1 COMMENT '用户等级',
  experience_points INT NOT NULL DEFAULT 0 COMMENT '经验值',
  -- 统计
  total_study_days INT NOT NULL DEFAULT 0 COMMENT '学习天数',
  total_questions INT NOT NULL DEFAULT 0 COMMENT '答题总数',
  -- 时间
  last_diagnostic_at DATETIME DEFAULT NULL COMMENT '最近一次诊断时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student (student_id),
  CONSTRAINT fk_profile_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学生五维画像表';

-- ============================================================
-- 3. 知识图谱
-- ============================================================

-- 知识点
CREATE TABLE knowledge_point (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  knowledge_id VARCHAR(50) NOT NULL COMMENT '知识点编号，如 func_001',
  module VARCHAR(30) NOT NULL COMMENT '所属模块：函数/导数/三角函数/向量/数列/解析几何',
  name VARCHAR(100) NOT NULL COMMENT '知识点名称',
  description TEXT DEFAULT NULL COMMENT '知识点描述',
  difficulty DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '难度系数 0-1',
  importance INT NOT NULL DEFAULT 5 COMMENT '重要度 1-10',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '模块内排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_knowledge_id (knowledge_id),
  INDEX idx_module (module)
) ENGINE=InnoDB COMMENT='知识点表';

-- 知识点前驱边（DAG）
CREATE TABLE knowledge_edge (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  from_knowledge_id VARCHAR(50) NOT NULL COMMENT '前驱知识点ID',
  to_knowledge_id VARCHAR(50) NOT NULL COMMENT '后继知识点ID',
  weight DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '边权重（依赖强度）',
  edge_type VARCHAR(20) NOT NULL DEFAULT 'prerequisite' COMMENT 'prerequisite/related/cross_module',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_edge (from_knowledge_id, to_knowledge_id),
  INDEX idx_from (from_knowledge_id),
  INDEX idx_to (to_knowledge_id)
) ENGINE=InnoDB COMMENT='知识图谱边表（前驱关系）';

-- 学生知识点掌握度
CREATE TABLE student_knowledge_mastery (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  knowledge_id VARCHAR(50) NOT NULL,
  mastery DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT 'BKT掌握度 0-1',
  status VARCHAR(20) NOT NULL DEFAULT 'not_started' COMMENT 'not_started/learning/mastered/weak',
  attempt_count INT NOT NULL DEFAULT 0 COMMENT '作答次数',
  correct_count INT NOT NULL DEFAULT 0 COMMENT '正确次数',
  last_attempt_at DATETIME DEFAULT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_knowledge (student_id, knowledge_id),
  INDEX idx_student (student_id),
  INDEX idx_knowledge (knowledge_id),
  CONSTRAINT fk_mastery_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学生知识点掌握度表';

-- ============================================================
-- 4. 自适应诊断
-- ============================================================

-- 诊断会话
CREATE TABLE diagnostic_session (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
  student_id BIGINT UNSIGNED NOT NULL,
  module VARCHAR(30) NOT NULL COMMENT '诊断模块',
  status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT 'in_progress/completed/abandoned',
  total_questions INT NOT NULL DEFAULT 0 COMMENT '总题数',
  correct_count INT NOT NULL DEFAULT 0 COMMENT '正确数',
  accuracy DECIMAL(3,2) DEFAULT NULL COMMENT '正确率',
  estimated_mastery DECIMAL(3,2) DEFAULT NULL COMMENT '诊断后估计掌握度',
  weak_points JSON DEFAULT NULL COMMENT '诊断出的薄弱点列表',
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_session_id (session_id),
  INDEX idx_student (student_id),
  INDEX idx_student_module (student_id, module),
  CONSTRAINT fk_diag_session_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='诊断会话表';

-- 题目库
CREATE TABLE question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  question_id VARCHAR(64) NOT NULL COMMENT '题目唯一标识',
  module VARCHAR(30) NOT NULL,
  knowledge_id VARCHAR(50) NOT NULL COMMENT '关联知识点',
  content TEXT NOT NULL COMMENT '题目内容（支持LaTeX）',
  question_type VARCHAR(20) NOT NULL DEFAULT 'single_choice' COMMENT 'single_choice/multi_choice/fill_blank/short_answer',
  options JSON DEFAULT NULL COMMENT '选项JSON [{label,content}]',
  correct_answer VARCHAR(255) NOT NULL COMMENT '正确答案',
  explanation TEXT DEFAULT NULL COMMENT '解析',
  difficulty DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT 'IRT难度参数',
  discrimination DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT 'IRT区分度',
  guessing DECIMAL(3,2) NOT NULL DEFAULT 0.25 COMMENT 'IRT猜测参数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_question_id (question_id),
  INDEX idx_module (module),
  INDEX idx_knowledge (knowledge_id),
  INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB COMMENT='题目库';

-- 诊断答题记录
CREATE TABLE diagnostic_answer (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  question_id VARCHAR(64) NOT NULL,
  question_index INT NOT NULL DEFAULT 0 COMMENT '题目在本次诊断中的序号',
  student_answer VARCHAR(500) DEFAULT NULL COMMENT '学生答案',
  is_correct TINYINT(1) NOT NULL DEFAULT 0,
  time_spent INT DEFAULT NULL COMMENT '答题耗时(秒)',
  confidence DECIMAL(3,2) DEFAULT NULL COMMENT '学生自评信心 0-1',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id),
  INDEX idx_student (student_id),
  CONSTRAINT fk_diag_answer_session FOREIGN KEY (session_id) REFERENCES diagnostic_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='诊断答题记录表';

-- ============================================================
-- 5. 溯因分析
-- ============================================================

-- 溯因分析记录
CREATE TABLE tracing_analysis (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  analysis_id VARCHAR(64) NOT NULL COMMENT '分析唯一标识',
  student_id BIGINT UNSIGNED NOT NULL,
  module VARCHAR(30) NOT NULL,
  trigger_knowledge_id VARCHAR(50) NOT NULL COMMENT '触发分析的知识点',
  root_causes JSON NOT NULL COMMENT '根因知识点列表 [{knowledgeId, name, mastery, depth}]',
  trace_path JSON DEFAULT NULL COMMENT '追溯路径可视化数据',
  suggestion TEXT DEFAULT NULL COMMENT 'AI建议',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analysis_id (analysis_id),
  INDEX idx_student (student_id),
  INDEX idx_student_module (student_id, module),
  CONSTRAINT fk_tracing_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='溯因分析记录表';

-- ============================================================
-- 6. 个性化学习路径
-- ============================================================

-- 学习路径
CREATE TABLE learning_path (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  path_id VARCHAR(64) NOT NULL COMMENT '路径唯一标识',
  student_id BIGINT UNSIGNED NOT NULL,
  module VARCHAR(30) NOT NULL,
  title VARCHAR(100) NOT NULL COMMENT '路径标题',
  strategy VARCHAR(30) NOT NULL DEFAULT 'basic_consolidation' COMMENT '教学策略: basic_consolidation/strengthening/expansion',
  status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/completed/paused',
  total_nodes INT NOT NULL DEFAULT 0 COMMENT '总节点数',
  completed_nodes INT NOT NULL DEFAULT 0 COMMENT '已完成节点数',
  progress DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '完成进度 0-1',
  estimated_hours DECIMAL(5,1) DEFAULT NULL COMMENT '预计学习时长(小时)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_path_id (path_id),
  INDEX idx_student (student_id),
  INDEX idx_student_status (student_id, status),
  CONSTRAINT fk_path_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学习路径表';

-- 学习路径节点
CREATE TABLE learning_path_node (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  path_id VARCHAR(64) NOT NULL,
  node_index INT NOT NULL DEFAULT 0 COMMENT '节点在路径中的顺序',
  knowledge_id VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  node_type VARCHAR(20) NOT NULL DEFAULT 'lesson' COMMENT 'lesson/exercise/checkpoint',
  status VARCHAR(20) NOT NULL DEFAULT 'locked' COMMENT 'locked/available/in_progress/completed',
  mastery_before DECIMAL(3,2) DEFAULT NULL COMMENT '学习前掌握度',
  mastery_after DECIMAL(3,2) DEFAULT NULL COMMENT '学习后掌握度',
  completed_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_path_node (path_id, node_index),
  INDEX idx_path (path_id),
  CONSTRAINT fk_node_path FOREIGN KEY (path_id) REFERENCES learning_path(path_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学习路径节点表';

-- 课程内容
CREATE TABLE lesson (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  lesson_id VARCHAR(64) NOT NULL COMMENT '课程唯一标识',
  knowledge_id VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content_type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT 'text/video/interactive',
  content MEDIUMTEXT NOT NULL COMMENT '课程内容（支持LaTeX/Markdown）',
  key_points JSON DEFAULT NULL COMMENT '知识要点列表',
  examples JSON DEFAULT NULL COMMENT '例题列表',
  difficulty DECIMAL(3,2) NOT NULL DEFAULT 0.50,
  estimated_minutes INT NOT NULL DEFAULT 15 COMMENT '预计学习时长(分钟)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_lesson_id (lesson_id),
  INDEX idx_knowledge (knowledge_id)
) ENGINE=InnoDB COMMENT='课程内容表';

-- 练习提交记录
CREATE TABLE exercise_submission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  path_id VARCHAR(64) DEFAULT NULL,
  knowledge_id VARCHAR(50) NOT NULL,
  question_id VARCHAR(64) NOT NULL,
  student_answer VARCHAR(500) DEFAULT NULL,
  is_correct TINYINT(1) NOT NULL DEFAULT 0,
  time_spent INT DEFAULT NULL COMMENT '耗时(秒)',
  feedback TEXT DEFAULT NULL COMMENT 'AI反馈',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_path (path_id),
  INDEX idx_knowledge (knowledge_id),
  CONSTRAINT fk_exercise_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='练习提交记录表';

-- 检查点提交记录
CREATE TABLE checkpoint_submission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  path_id VARCHAR(64) NOT NULL,
  node_index INT NOT NULL,
  knowledge_id VARCHAR(50) NOT NULL,
  answers JSON NOT NULL COMMENT '答题记录 [{questionId, answer, isCorrect}]',
  score DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '得分',
  passed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通过',
  mastery_after DECIMAL(3,2) DEFAULT NULL COMMENT '检查点后掌握度',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_path (path_id),
  CONSTRAINT fk_checkpoint_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='检查点提交记录表';

-- ============================================================
-- 7. AI 对话
-- ============================================================

-- 对话会话
CREATE TABLE chat_session (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL COMMENT '对话会话唯一标识',
  student_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(100) DEFAULT NULL COMMENT '对话标题',
  context_module VARCHAR(30) DEFAULT NULL COMMENT '上下文模块',
  context_knowledge_id VARCHAR(50) DEFAULT NULL COMMENT '上下文知识点',
  message_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_session_id (session_id),
  INDEX idx_student (student_id),
  CONSTRAINT fk_chat_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='对话会话表';

-- 对话消息
CREATE TABLE chat_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL,
  role VARCHAR(10) NOT NULL COMMENT 'user/assistant/system',
  content TEXT NOT NULL COMMENT '消息内容（支持LaTeX）',
  tokens_used INT DEFAULT NULL COMMENT 'token消耗',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id),
  INDEX idx_session_time (session_id, created_at),
  CONSTRAINT fk_msg_session FOREIGN KEY (session_id) REFERENCES chat_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='对话消息表';

-- ============================================================
-- 8. 教师模块
-- ============================================================

-- 教师-学生关系
CREATE TABLE teacher_student (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_student (teacher_id, student_id),
  INDEX idx_teacher (teacher_id),
  INDEX idx_student (student_id),
  CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  CONSTRAINT fk_ts_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='教师-学生关系表';

-- 学生预警
CREATE TABLE student_alert (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  alert_id VARCHAR(64) NOT NULL COMMENT '预警唯一标识',
  teacher_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  student_name VARCHAR(50) DEFAULT NULL COMMENT '学生姓名（冗余）',
  type VARCHAR(20) NOT NULL COMMENT 'inactive/decline/stagnant',
  description TEXT NOT NULL COMMENT '预警描述',
  suggestion TEXT DEFAULT NULL COMMENT '处理建议',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/handled/dismissed',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_alert_id (alert_id),
  INDEX idx_teacher (teacher_id),
  INDEX idx_student (student_id),
  CONSTRAINT fk_alert_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  CONSTRAINT fk_alert_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学生预警表';

-- 分层作业
CREATE TABLE homework_task (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  homework_id VARCHAR(64) NOT NULL COMMENT '作业唯一标识',
  teacher_id BIGINT UNSIGNED NOT NULL,
  module VARCHAR(30) NOT NULL,
  knowledge_points JSON NOT NULL COMMENT '关联知识点列表',
  layers JSON NOT NULL COMMENT '分层题目 [{level, title, questions:[{content, difficulty, knowledgeId}]}]',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_homework_id (homework_id),
  INDEX idx_teacher (teacher_id),
  CONSTRAINT fk_homework_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='分层作业表';

-- 班级周报
CREATE TABLE weekly_report (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT UNSIGNED NOT NULL,
  week_label VARCHAR(20) NOT NULL COMMENT '周标识，如 2026-W21',
  summary JSON NOT NULL COMMENT '{avgMasteryChange, activeStudents, totalQuestions}',
  top_improvers JSON DEFAULT NULL COMMENT '[{name, improvement}]',
  struggling JSON DEFAULT NULL COMMENT '[{name, reason}]',
  details JSON DEFAULT NULL COMMENT '详细数据',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_week (teacher_id, week_label),
  CONSTRAINT fk_report_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='班级周报表';

-- ============================================================
-- 9. 学习报告与效果追踪
-- ============================================================

-- 学习活动日志
CREATE TABLE study_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  activity_type VARCHAR(30) NOT NULL COMMENT 'diagnostic/lesson/exercise/chat/checkpoint',
  module VARCHAR(30) DEFAULT NULL,
  knowledge_id VARCHAR(50) DEFAULT NULL,
  duration_minutes INT NOT NULL DEFAULT 0 COMMENT '学习时长(分钟)',
  questions_done INT NOT NULL DEFAULT 0 COMMENT '完成题数',
  correct_count INT NOT NULL DEFAULT 0 COMMENT '正确数',
  mastery_change DECIMAL(4,3) DEFAULT NULL COMMENT '掌握度变化',
  log_date DATE NOT NULL COMMENT '活动日期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student_date (student_id, log_date),
  INDEX idx_student_type (student_id, activity_type),
  CONSTRAINT fk_log_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学习活动日志表';

-- 前后测对比快照
CREATE TABLE mastery_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  module VARCHAR(30) NOT NULL,
  snapshot_type VARCHAR(10) NOT NULL COMMENT 'pre/post',
  mastery DECIMAL(3,2) NOT NULL,
  snapshot_date DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student_module (student_id, module),
  CONSTRAINT fk_snapshot_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='掌握度快照（前后测对比）';

-- ============================================================
-- 10. 游戏化与任务系统
-- ============================================================

-- 每日任务定义
CREATE TABLE mission_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  mission_type VARCHAR(30) NOT NULL COMMENT '任务类型标识',
  title VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  target_value INT NOT NULL DEFAULT 1 COMMENT '目标值',
  reward_xp INT NOT NULL DEFAULT 10 COMMENT '奖励经验值',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='任务模板表';

-- 学生每日任务实例
CREATE TABLE student_mission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  mission_type VARCHAR(30) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  target_value INT NOT NULL DEFAULT 1,
  current_value INT NOT NULL DEFAULT 0,
  reward_xp INT NOT NULL DEFAULT 10,
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/completed/claimed',
  mission_date DATE NOT NULL COMMENT '任务所属日期',
  completed_at DATETIME DEFAULT NULL,
  claimed_at DATETIME DEFAULT NULL,
  INDEX idx_student_date (student_id, mission_date),
  INDEX idx_student_status (student_id, status),
  CONSTRAINT fk_mission_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='学生每日任务表';

-- ============================================================
-- 11. 初始知识点数据（六大模块示例）
-- ============================================================

INSERT INTO knowledge_point (knowledge_id, module, name, difficulty, importance, sort_order) VALUES
-- 函数模块
('func_001', '函数', '函数的概念与表示', 0.30, 9, 1),
('func_002', '函数', '函数的定义域与值域', 0.35, 8, 2),
('func_003', '函数', '函数的单调性', 0.40, 9, 3),
('func_004', '函数', '函数的奇偶性', 0.40, 8, 4),
('func_005', '函数', '指数函数', 0.45, 8, 5),
('func_006', '函数', '对数函数', 0.50, 8, 6),
('func_007', '函数', '幂函数', 0.45, 7, 7),
('func_008', '函数', '函数与方程', 0.55, 7, 8),
('func_009', '函数', '函数的应用', 0.60, 8, 9),
-- 导数模块
('deriv_001', '导数', '导数的概念', 0.45, 9, 1),
('deriv_002', '导数', '导数的运算法则', 0.50, 9, 2),
('deriv_003', '导数', '复合函数求导', 0.60, 8, 3),
('deriv_004', '导数', '导数与函数单调性', 0.55, 9, 4),
('deriv_005', '导数', '导数与极值最值', 0.65, 9, 5),
('deriv_006', '导数', '导数的应用', 0.70, 8, 6),
-- 三角函数模块
('trig_001', '三角函数', '角的概念推广', 0.30, 7, 1),
('trig_002', '三角函数', '三角函数定义', 0.35, 9, 2),
('trig_003', '三角函数', '三角函数图像与性质', 0.50, 9, 3),
('trig_004', '三角函数', '三角恒等变换', 0.55, 8, 4),
('trig_005', '三角函数', '正弦定理与余弦定理', 0.55, 9, 5),
('trig_006', '三角函数', '解三角形应用', 0.60, 8, 6),
-- 向量模块
('vec_001', '向量', '向量的概念', 0.30, 8, 1),
('vec_002', '向量', '向量的加减运算', 0.35, 8, 2),
('vec_003', '向量', '向量的数乘', 0.35, 7, 3),
('vec_004', '向量', '向量的数量积', 0.50, 9, 4),
('vec_005', '向量', '向量的坐标运算', 0.45, 8, 5),
('vec_006', '向量', '向量的应用', 0.60, 8, 6),
-- 数列模块
('seq_001', '数列', '数列的概念', 0.30, 8, 1),
('seq_002', '数列', '等差数列', 0.40, 9, 2),
('seq_003', '数列', '等比数列', 0.45, 9, 3),
('seq_004', '数列', '数列求和', 0.55, 8, 4),
('seq_005', '数列', '数列递推与通项', 0.60, 8, 5),
('seq_006', '数列', '数列的应用', 0.65, 7, 6),
-- 解析几何模块
('geom_001', '解析几何', '直线的方程', 0.35, 8, 1),
('geom_002', '解析几何', '圆的方程', 0.40, 8, 2),
('geom_003', '解析几何', '椭圆', 0.55, 9, 3),
('geom_004', '解析几何', '双曲线', 0.60, 8, 4),
('geom_005', '解析几何', '抛物线', 0.60, 8, 5),
('geom_006', '解析几何', '直线与圆锥曲线', 0.75, 9, 6);

-- 知识点前驱关系示例
INSERT INTO knowledge_edge (from_knowledge_id, to_knowledge_id, weight, edge_type) VALUES
-- 函数内部
('func_001', 'func_002', 1.00, 'prerequisite'),
('func_002', 'func_003', 0.90, 'prerequisite'),
('func_002', 'func_004', 0.85, 'prerequisite'),
('func_003', 'func_005', 0.80, 'prerequisite'),
('func_003', 'func_006', 0.80, 'prerequisite'),
('func_005', 'func_007', 0.70, 'prerequisite'),
('func_006', 'func_008', 0.75, 'prerequisite'),
('func_008', 'func_009', 0.80, 'prerequisite'),
-- 函数→导数（跨模块）
('func_003', 'deriv_001', 0.90, 'cross_module'),
('func_005', 'deriv_002', 0.70, 'cross_module'),
('func_006', 'deriv_002', 0.70, 'cross_module'),
-- 导数内部
('deriv_001', 'deriv_002', 1.00, 'prerequisite'),
('deriv_002', 'deriv_003', 0.90, 'prerequisite'),
('deriv_003', 'deriv_004', 0.85, 'prerequisite'),
('deriv_004', 'deriv_005', 0.90, 'prerequisite'),
('deriv_005', 'deriv_006', 0.80, 'prerequisite'),
-- 三角函数内部
('trig_001', 'trig_002', 1.00, 'prerequisite'),
('trig_002', 'trig_003', 0.90, 'prerequisite'),
('trig_003', 'trig_004', 0.85, 'prerequisite'),
('trig_004', 'trig_005', 0.80, 'prerequisite'),
('trig_005', 'trig_006', 0.85, 'prerequisite'),
-- 向量内部
('vec_001', 'vec_002', 1.00, 'prerequisite'),
('vec_002', 'vec_003', 0.90, 'prerequisite'),
('vec_003', 'vec_004', 0.85, 'prerequisite'),
('vec_004', 'vec_005', 0.80, 'prerequisite'),
('vec_005', 'vec_006', 0.80, 'prerequisite'),
-- 数列内部
('seq_001', 'seq_002', 1.00, 'prerequisite'),
('seq_002', 'seq_003', 0.85, 'prerequisite'),
('seq_003', 'seq_004', 0.80, 'prerequisite'),
('seq_004', 'seq_005', 0.75, 'prerequisite'),
('seq_005', 'seq_006', 0.80, 'prerequisite'),
-- 解析几何内部
('geom_001', 'geom_002', 0.90, 'prerequisite'),
('geom_002', 'geom_003', 0.85, 'prerequisite'),
('geom_003', 'geom_004', 0.80, 'prerequisite'),
('geom_003', 'geom_005', 0.80, 'prerequisite'),
('geom_004', 'geom_006', 0.85, 'prerequisite'),
('geom_005', 'geom_006', 0.85, 'prerequisite'),
-- 跨模块依赖
('trig_002', 'vec_004', 0.60, 'cross_module'),
('func_003', 'seq_002', 0.50, 'cross_module'),
('vec_005', 'geom_001', 0.70, 'cross_module');

-- ============================================================
-- 12. 任务模板初始数据
-- ============================================================

INSERT INTO mission_template (mission_type, title, description, target_value, reward_xp) VALUES
('daily_diagnostic', '完成一次诊断', '今日完成一次自适应诊断测试', 1, 20),
('daily_questions', '答题达人', '今日累计完成5道练习题', 5, 15),
('daily_chat', 'AI问答', '与AI辅导助手对话3次', 3, 10),
('daily_lesson', '知识学习', '完成一个课程节点的学习', 1, 15),
('daily_streak', '连续学习', '保持连续学习记录', 1, 25);

-- ============================================================
-- 完成
-- ============================================================

-- ============================================================
-- 13. 2026-06-02 当前项目合并更新
-- 说明:
-- 1) 当前后端实体已从手机号登录切换为 username/email 注册登录；
-- 2) 教师-学生关系以 class_student 记录班级维度；
-- 3) 教师分层作业落表 teacher_homework，学生通过 /api/homework 读取分配给自己的层级；
-- 4) 以下语句用于让本初始化脚本与当前 SmartMentor/ smartmentor-web 代码保持一致。
-- ============================================================

-- 当前 Student/Teacher 实体字段兼容
ALTER TABLE student
  ADD COLUMN username VARCHAR(50) NULL COMMENT '用户名（当前登录账号）' AFTER id,
  ADD COLUMN email VARCHAR(100) NULL COMMENT '邮箱' AFTER school,
  MODIFY phone VARCHAR(20) NULL COMMENT '手机号（旧版登录账号，当前可为空）',
  ADD UNIQUE KEY uk_student_username_current (username);

ALTER TABLE teacher
  ADD COLUMN username VARCHAR(50) NULL COMMENT '用户名（当前登录账号）' AFTER id,
  ADD COLUMN email VARCHAR(100) NULL COMMENT '邮箱' AFTER school,
  MODIFY phone VARCHAR(20) NULL COMMENT '手机号（可为空）',
  ADD UNIQUE KEY uk_teacher_username_current (username);

ALTER TABLE diagnostic_session
  ADD COLUMN diagnostic_id VARCHAR(100) NULL COMMENT '当前诊断唯一标识' AFTER id,
  ADD COLUMN overall_mastery DECIMAL(3,2) DEFAULT NULL COMMENT '整体掌握度' AFTER accuracy,
  ADD COLUMN current_difficulty DECIMAL(3,2) DEFAULT NULL COMMENT '当前难度' AFTER overall_mastery,
  ADD COLUMN current_question_index INT DEFAULT 0 COMMENT '当前题序' AFTER current_difficulty,
  ADD COLUMN knowledge_point_results JSON DEFAULT NULL COMMENT '知识点诊断结果' AFTER current_question_index,
  ADD COLUMN error_patterns JSON DEFAULT NULL COMMENT '错误模式' AFTER weak_points,
  ADD COLUMN question_snapshots JSON DEFAULT NULL COMMENT '诊断题目快照' AFTER error_patterns,
  ADD COLUMN start_time DATETIME DEFAULT NULL COMMENT '当前诊断开始时间' AFTER question_snapshots,
  ADD COLUMN end_time DATETIME DEFAULT NULL COMMENT '当前诊断结束时间' AFTER start_time,
  MODIFY session_id VARCHAR(64) NULL COMMENT '旧版会话唯一标识，当前使用 diagnostic_id',
  ADD UNIQUE KEY uk_diagnostic_id_current (diagnostic_id);

ALTER TABLE learning_path
  ADD COLUMN tracing_result_id BIGINT DEFAULT NULL COMMENT '溯因结果ID' AFTER student_id,
  ADD COLUMN target_knowledge_point_id VARCHAR(100) DEFAULT NULL COMMENT '目标知识点ID' AFTER tracing_result_id,
  ADD COLUMN target_knowledge_point_name VARCHAR(100) DEFAULT NULL COMMENT '目标知识点名称' AFTER target_knowledge_point_id,
  ADD COLUMN root_cause_point_id VARCHAR(100) DEFAULT NULL COMMENT '根因知识点ID' AFTER target_knowledge_point_name,
  ADD COLUMN root_cause_point_name VARCHAR(100) DEFAULT NULL COMMENT '根因知识点名称' AFTER root_cause_point_id,
  ADD COLUMN path_name VARCHAR(200) DEFAULT NULL COMMENT '路径名称' AFTER root_cause_point_name,
  ADD COLUMN current_node_id VARCHAR(100) DEFAULT NULL COMMENT '当前节点ID' AFTER path_name,
  ADD COLUMN mode VARCHAR(20) DEFAULT NULL COMMENT '路径模式' AFTER current_node_id,
  ADD COLUMN total_estimated_minutes INT DEFAULT NULL COMMENT '预计总学习分钟' AFTER progress,
  ADD COLUMN actual_study_minutes INT DEFAULT 0 COMMENT '实际学习分钟' AFTER total_estimated_minutes,
  ADD COLUMN nodes JSON DEFAULT NULL COMMENT '路径节点快照' AFTER completed_nodes,
  ADD COLUMN tracing_path JSON DEFAULT NULL COMMENT '溯因路径快照' AFTER nodes,
  ADD COLUMN lesson_snapshots JSON DEFAULT NULL COMMENT '课程与练习快照' AFTER tracing_path,
  ADD COLUMN last_study_at DATETIME DEFAULT NULL COMMENT '最近学习时间' AFTER created_at,
  ADD COLUMN completed_at DATETIME DEFAULT NULL COMMENT '完成时间' AFTER last_study_at,
  MODIFY path_id VARCHAR(64) NULL COMMENT '旧版路径唯一标识，当前使用自增 id',
  MODIFY module VARCHAR(30) NULL COMMENT '旧版模块字段，当前由目标知识点推导',
  MODIFY title VARCHAR(100) NULL COMMENT '旧版路径标题，当前使用 path_name',
  MODIFY strategy VARCHAR(30) NULL COMMENT '旧版教学策略字段';

CREATE TABLE IF NOT EXISTS answer_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  diagnostic_id VARCHAR(100) DEFAULT NULL COMMENT '关联诊断会话ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  question_index INT DEFAULT NULL COMMENT '题目序号',
  question_id BIGINT DEFAULT NULL COMMENT '题目ID',
  knowledge_point_id VARCHAR(100) DEFAULT NULL COMMENT '知识点ID',
  knowledge_point_name VARCHAR(100) DEFAULT NULL COMMENT '知识点名称',
  question_type VARCHAR(20) DEFAULT NULL COMMENT '题目类型',
  difficulty DECIMAL(3,2) DEFAULT NULL COMMENT '难度系数',
  content TEXT DEFAULT NULL COMMENT '题目内容',
  options JSON DEFAULT NULL COMMENT '选项JSON',
  student_answer TEXT DEFAULT NULL COMMENT '学生答案',
  correct_answer VARCHAR(500) DEFAULT NULL COMMENT '正确答案',
  is_correct TINYINT(1) DEFAULT NULL COMMENT '是否正确',
  time_spent INT DEFAULT NULL COMMENT '答题耗时秒',
  error_type VARCHAR(50) DEFAULT NULL COMMENT '错误类型',
  error_detail VARCHAR(200) DEFAULT NULL COMMENT '错误详情',
  error_analysis TEXT DEFAULT NULL COMMENT '错误分析',
  solution TEXT DEFAULT NULL COMMENT '解题过程',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_answer_student (student_id),
  INDEX idx_answer_diagnostic (diagnostic_id),
  INDEX idx_answer_student_time (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题记录表';

CREATE TABLE IF NOT EXISTS class_student (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT NOT NULL COMMENT '教师ID',
  class_name VARCHAR(50) NOT NULL COMMENT '班级名称',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_class_teacher (teacher_id),
  INDEX idx_class_teacher_name (teacher_id, class_name),
  INDEX idx_class_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级学生关系表';

CREATE TABLE IF NOT EXISTS teacher_homework (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  homework_id VARCHAR(100) NOT NULL COMMENT '作业唯一标识',
  teacher_id BIGINT NOT NULL COMMENT '教师ID',
  class_name VARCHAR(50) NOT NULL COMMENT '班级名称',
  knowledge_point_ids JSON DEFAULT NULL COMMENT '知识点ID列表',
  knowledge_point_names JSON DEFAULT NULL COMMENT '知识点名称列表',
  layers JSON DEFAULT NULL COMMENT '分层题目与分配信息',
  student_assignments JSON DEFAULT NULL COMMENT '学生到层级的分配映射',
  status VARCHAR(20) DEFAULT 'published' COMMENT 'published/draft/archived',
  deadline DATETIME DEFAULT NULL COMMENT '截止时间',
  note VARCHAR(500) DEFAULT NULL COMMENT '教师备注',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_homework_id (homework_id),
  INDEX idx_teacher_homework_class (teacher_id, class_name),
  INDEX idx_teacher_homework_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师分层作业表';

CREATE TABLE IF NOT EXISTS mastery_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL COMMENT '学生ID',
  knowledge_point_id VARCHAR(100) DEFAULT NULL COMMENT '知识点ID',
  module VARCHAR(50) DEFAULT NULL COMMENT '所属模块',
  mastery DECIMAL(5,4) DEFAULT NULL COMMENT '知识点掌握度',
  overall_mastery DECIMAL(5,4) DEFAULT NULL COMMENT '整体掌握度',
  source VARCHAR(30) DEFAULT NULL COMMENT 'diagnostic/practice/teaching/checkpoint',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_mastery_student (student_id),
  INDEX idx_mastery_student_time (student_id, created_at),
  INDEX idx_mastery_student_kp (student_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='掌握度变化历史表';

CREATE TABLE IF NOT EXISTS study_activity (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL COMMENT '学生ID',
  activity_type VARCHAR(30) DEFAULT NULL COMMENT '活动类型',
  description VARCHAR(200) DEFAULT NULL COMMENT '活动描述',
  duration_minutes INT DEFAULT NULL COMMENT '活动时长分钟',
  result_summary VARCHAR(500) DEFAULT NULL COMMENT '结果摘要',
  knowledge_point_id VARCHAR(100) DEFAULT NULL COMMENT '关联知识点ID',
  module VARCHAR(50) DEFAULT NULL COMMENT '所属模块',
  activity_date DATE DEFAULT NULL COMMENT '活动日期',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_activity_student (student_id),
  INDEX idx_activity_student_date (student_id, activity_date),
  INDEX idx_activity_student_type (student_id, activity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习活动记录表';

CREATE TABLE IF NOT EXISTS tracing_result (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tracing_id VARCHAR(100) NOT NULL COMMENT '溯因分析唯一标识',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  diagnostic_id VARCHAR(100) DEFAULT NULL COMMENT '关联诊断ID',
  analyzed_point_count INT DEFAULT NULL COMMENT '分析知识点数量',
  root_cause_count INT DEFAULT NULL COMMENT '根因数量',
  is_cross_module TINYINT(1) DEFAULT NULL COMMENT '是否跨模块',
  tracing_results JSON DEFAULT NULL COMMENT '溯因结果详情',
  merged_root_causes JSON DEFAULT NULL COMMENT '合并根因列表',
  graph_visualization JSON DEFAULT NULL COMMENT '图谱可视化数据',
  suggested_learning_path JSON DEFAULT NULL COMMENT '建议学习路径',
  suggestion TEXT DEFAULT NULL COMMENT 'AI建议',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tracing_result_id (tracing_id),
  INDEX idx_tracing_student (student_id),
  INDEX idx_tracing_diagnostic (diagnostic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='溯因分析结果表';

CREATE TABLE IF NOT EXISTS mission (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  mission_id VARCHAR(100) DEFAULT NULL COMMENT '任务唯一标识',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  title VARCHAR(100) DEFAULT NULL COMMENT '任务标题',
  description VARCHAR(500) DEFAULT NULL COMMENT '任务描述',
  type VARCHAR(20) DEFAULT NULL COMMENT '任务类型',
  reward_exp INT DEFAULT NULL COMMENT '奖励经验值',
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/completed/claimed',
  mission_date DATE DEFAULT NULL COMMENT '任务日期',
  completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mission_id_current (mission_id),
  INDEX idx_mission_student (student_id),
  INDEX idx_mission_student_date (student_id, mission_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生任务表';

CREATE TABLE IF NOT EXISTS question_bank (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  question_hash VARCHAR(64) NOT NULL COMMENT '题目内容哈希',
  source VARCHAR(40) DEFAULT NULL COMMENT '来源',
  source_ref VARCHAR(100) DEFAULT NULL COMMENT '来源引用',
  module VARCHAR(50) DEFAULT NULL COMMENT '模块',
  knowledge_point_id VARCHAR(100) DEFAULT NULL COMMENT '知识点ID',
  knowledge_point_name VARCHAR(100) DEFAULT NULL COMMENT '知识点名称',
  question_type VARCHAR(30) DEFAULT NULL COMMENT '题型',
  difficulty DECIMAL(4,2) DEFAULT NULL COMMENT '难度',
  content TEXT DEFAULT NULL COMMENT '题干',
  options JSON DEFAULT NULL COMMENT '选项',
  correct_answer TEXT DEFAULT NULL COMMENT '答案',
  explanation TEXT DEFAULT NULL COMMENT '解析',
  error_type VARCHAR(100) DEFAULT NULL COMMENT '错误类型',
  quality_score DECIMAL(4,2) DEFAULT NULL COMMENT '质量分',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_question_hash (question_hash),
  INDEX idx_question_bank_kp (knowledge_point_id),
  INDEX idx_question_bank_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库缓存表';

CREATE TABLE IF NOT EXISTS agent_run_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  agent_name VARCHAR(80) NOT NULL COMMENT 'Agent名称',
  student_id BIGINT DEFAULT NULL COMMENT '学生ID',
  diagnostic_id VARCHAR(100) DEFAULT NULL COMMENT '诊断ID',
  module VARCHAR(100) DEFAULT NULL COMMENT '模块',
  prompt_hash VARCHAR(64) DEFAULT NULL COMMENT 'Prompt哈希',
  prompt_version VARCHAR(80) DEFAULT NULL COMMENT 'Prompt版本',
  prompt_length INT DEFAULT NULL COMMENT 'Prompt长度',
  response_length INT DEFAULT NULL COMMENT '响应长度',
  model VARCHAR(100) DEFAULT NULL COMMENT '模型',
  latency_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  success TINYINT(1) DEFAULT NULL COMMENT '是否成功',
  fallback_used TINYINT(1) DEFAULT NULL COMMENT '是否使用兜底',
  quality_score DOUBLE DEFAULT NULL COMMENT '质量分',
  event VARCHAR(80) DEFAULT NULL COMMENT '触发事件',
  message VARCHAR(500) DEFAULT NULL COMMENT '消息',
  input_summary VARCHAR(1000) DEFAULT NULL COMMENT '输入摘要',
  output_summary VARCHAR(1000) DEFAULT NULL COMMENT '输出摘要',
  error_message TEXT DEFAULT NULL COMMENT '错误信息',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_agent_run_student (student_id),
  INDEX idx_agent_run_agent (agent_name),
  INDEX idx_agent_run_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent运行日志表';


-- ############################################################
-- 第 2 部分 / A3 画像字段迁移（幂等，可重复执行）
-- 源文件：SmartMentor_A3_profile_migration.sql
-- ############################################################

-- ============================================================
-- SmartMentor A3 赛题：student_profile 画像字段迁移（幂等）
-- 适用：MySQL 8.x
-- 说明：逐列检查 information_schema，缺失才新增，可安全重复执行；
--      不会因为列已存在而中断（标准 MySQL 不支持 ADD COLUMN IF NOT EXISTS）。
-- 注意：参数统一用单引号字符串，内部单引号用 '' 转义，避免 ANSI_QUOTES 模式
--      把双引号当成标识符解析（报"无法解析列"）。
-- ============================================================

use smartmentor;
DROP PROCEDURE IF EXISTS smartmentor_add_profile_column;

DELIMITER $$
CREATE PROCEDURE smartmentor_add_profile_column(
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'student_profile'
          AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE student_profile ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- A3 新增画像字段
CALL smartmentor_add_profile_column('major_direction',     'VARCHAR(50) DEFAULT NULL COMMENT ''专业方向''');
CALL smartmentor_add_profile_column('education_level',      'VARCHAR(20) DEFAULT NULL COMMENT ''学历层次''');
CALL smartmentor_add_profile_column('current_course',       'VARCHAR(100) DEFAULT NULL COMMENT ''当前课程''');
CALL smartmentor_add_profile_column('learning_goal',        'VARCHAR(50) DEFAULT NULL COMMENT ''学习目标''');
CALL smartmentor_add_profile_column('foundation_level',     'VARCHAR(20) DEFAULT NULL COMMENT ''基础水平''');
CALL smartmentor_add_profile_column('resource_preference',  'JSON DEFAULT NULL COMMENT ''资源偏好''');
CALL smartmentor_add_profile_column('academic_interest',    'VARCHAR(255) DEFAULT NULL COMMENT ''学术或职业兴趣''');

-- 兼容旧库：以下字段若早期版本缺失则补齐，已存在则跳过
CALL smartmentor_add_profile_column('target_score',         'INT DEFAULT NULL COMMENT ''目标分数或目标等级''');
CALL smartmentor_add_profile_column('weak_module_priority', 'JSON DEFAULT NULL COMMENT ''薄弱课程模块优先级''');
CALL smartmentor_add_profile_column('study_mode',           'VARCHAR(20) DEFAULT ''systematic'' COMMENT ''学习模式''');

DROP PROCEDURE IF EXISTS smartmentor_add_profile_column;


-- ############################################################
-- 第 3 部分 / A3 旧版残留数据清理（破坏性，按需执行）
-- 源文件：SmartMentor_A3_cleanup_legacy_data.sql
-- ############################################################

-- ============================================================
-- SmartMentor A3 赛题：清理旧版（高中数学）残留学习数据
-- 适用：MySQL 8.x
--
-- 背景：知识图谱已从高中数学切换为高校课程，但旧的诊断/溯因/
--      学习路径等学生数据仍引用已不存在的数学知识点，导致前端
--      仍显示"集合的概念/函数单调性/数列与极限/导数"等内容。
--
-- 作用：清空"按知识点生成"的可再生学生数据（含学习路径、诊断、溯因、
--      分层作业、AI 对话等），保留账号与画像基础信息。
--      执行后让学生重新走 诊断 -> 溯因 -> 学习路径，即按高校课程重建。
--
-- 健壮性：采用"表存在才清空"的方式，某张表在当前库中不存在时自动跳过，
--        不会因 question_bank 等表缺失而中断（也消除 IDE"无法解析表"导致的执行担忧）。
--
-- ⚠️ 破坏性操作：会删除所有学生的学习/诊断历史。执行前请先备份：
--      mysqldump -u <user> -p <db> > backup_before_cleanup.sql
-- ============================================================

DROP PROCEDURE IF EXISTS smartmentor_truncate_if_exists;

DELIMITER $$
CREATE PROCEDURE smartmentor_truncate_if_exists(IN p_table VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
    ) THEN
        SET @ddl = CONCAT('TRUNCATE TABLE ', p_table);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- A. 全量清理（所有学生）
-- ------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

CALL smartmentor_truncate_if_exists('learning_path');      -- 学习路径
CALL smartmentor_truncate_if_exists('tracing_result');     -- 溯因分析结果
CALL smartmentor_truncate_if_exists('diagnostic_session'); -- 诊断会话
CALL smartmentor_truncate_if_exists('answer_record');      -- 诊断答题记录
CALL smartmentor_truncate_if_exists('mastery_history');    -- 掌握度变化历史
CALL smartmentor_truncate_if_exists('study_activity');     -- 学习活动记录
CALL smartmentor_truncate_if_exists('question_bank');      -- 题库缓存（旧数学题）
CALL smartmentor_truncate_if_exists('mission');            -- 学生任务
CALL smartmentor_truncate_if_exists('teacher_homework');   -- 教师分层作业（旧数学知识点）
CALL smartmentor_truncate_if_exists('chat_message');       -- AI 对话消息
CALL smartmentor_truncate_if_exists('chat_session');       -- AI 对话会话

SET FOREIGN_KEY_CHECKS = 1;

DROP PROCEDURE IF EXISTS smartmentor_truncate_if_exists;

-- 重置画像中的掌握度快照（保留账号、专业/学历/课程等画像设置）
UPDATE student_profile
SET knowledge_state_json = NULL,
    error_patterns       = NULL,
    weak_module_priority = NULL,
    overall_mastery      = 0.00,
    ability_param        = 0.00,
    last_diagnostic_at   = NULL;

-- ------------------------------------------------------------
-- B. 按单个学生清理（可选）
--    用法：把上面 A 段（含存储过程定义与 CALL）整体注释掉，
--    将 @sid 改成目标学生ID后执行本段。
--    注：仅在确认相关表都存在时使用；question_bank 为全局缓存不区分学生。
-- ------------------------------------------------------------
-- SET @sid = 1;
-- DELETE FROM learning_path      WHERE student_id = @sid;
-- DELETE FROM tracing_result     WHERE student_id = @sid;
-- DELETE FROM diagnostic_session WHERE student_id = @sid;
-- DELETE FROM answer_record      WHERE student_id = @sid;
-- DELETE FROM mastery_history    WHERE student_id = @sid;
-- DELETE FROM study_activity     WHERE student_id = @sid;
-- DELETE FROM mission            WHERE student_id = @sid;
-- DELETE FROM chat_message       WHERE session_id IN (SELECT session_id FROM chat_session WHERE student_id = @sid);
-- DELETE FROM chat_session       WHERE student_id = @sid;
-- UPDATE student_profile
--   SET knowledge_state_json = NULL,
--       error_patterns       = NULL,
--       weak_module_priority = NULL,
--       overall_mastery      = 0.00,
--       ability_param        = 0.00,
--       last_diagnostic_at   = NULL
-- WHERE student_id = @sid;
-- 注：teacher_homework 按教师/班级维度存储，不区分单个学生，无法按 @sid 过滤，
--    如需清理请在 A 段统一 TRUNCATE，或按 teacher_id 单独删

CREATE DATABASE IF NOT EXISTS iot_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
