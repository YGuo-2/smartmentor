# SmartMentor Backend

Spring Boot backend for the SmartMentor intelligent tutoring system.

## Prerequisites

- JDK 17
- MySQL 8
- Redis 6+
- PowerShell on Windows

## Configuration

Runtime secrets are read from environment variables. See `.env.example` for the full list.

PowerShell example:

```powershell
$env:SMARTMENTOR_DB_PASSWORD="your-db-password"
$env:SMARTMENTOR_REDIS_PASSWORD="your-redis-password"
$env:SMARTMENTOR_MAIL_USERNAME="your-mail-account"
$env:SMARTMENTOR_MAIL_PASSWORD="your-mail-token"
$env:SMARTMENTOR_DEEPSEEK_API_KEY="your-api-key"
$env:SMARTMENTOR_JWT_SECRET="replace-with-a-long-random-secret"
$env:SMARTMENTOR_OFFLINE_DEMO_ENABLED="false"
```

Do not commit real passwords, mail tokens, or model API keys.

## Run

```powershell
cd D:\Idea\中国软件杯\SmartMentor
.\mvnw.cmd spring-boot:run
```

The backend listens on `http://localhost:8080` by default.

## Test

```powershell
cd D:\Idea\中国软件杯\SmartMentor
.\mvnw.cmd clean test
```

## Notes

- Maven is the sole build tool for this project. Use `mvnw.cmd` for building, testing, and demo startup.
- `target/` is build output and should not be committed.
- Generated directories across both repos can be reviewed with `..\scripts\clean-generated.ps1` and removed with `..\scripts\clean-generated.ps1 -Apply`.
- Agent 协作分为事件链路和专职生成器两类。事件协作主链是 `DiagnosticAgent -> TracingAgent -> PlanningAgent -> TeachingAgent/EvaluationAgent`；`ProfileAgent`、`PresentationAgent`、`ResourceAgent` 是由 Service 编排调用的专职生成器，不等同于每条事件链都会自动经过的节点。
- 诊断 -> 溯源 -> 规划保留用户在环节奏：学生先查看溯源结果，再生成学习路径，这是教育场景下的产品设计。自动干预链路用于检查点失败（`MASTERY_NOT_REACHED`）和同一路径节点连续练习错误（`CONSECUTIVE_ERRORS`）。`NEW_WEAKNESS_FOUND` 仍是预留事件，不应作为已落地自动能力宣传。
- The AI lesson exercises are snapshotted on the learning path before answer submission, so exercise grading uses server-side data instead of client-provided answers.
- Checkpoint submission also grades against server-side lesson snapshots; client-provided `correctAnswer` values are ignored.
- Diagnostic questions are also saved as server-side snapshots on `diagnostic_session`, so grading does not depend on frontend state or Redis availability.
- Agent calls are audited in `agent_run_log` with prompt hash/version, model, latency, success, fallback flag, quality score, input summary, and output summary.
- Set `SMARTMENTOR_OFFLINE_DEMO_ENABLED=true` to use local structured Agent responses for demos when the model API is unavailable.
- Generated diagnostic questions are saved into `question_bank` for quality review; later diagnostic sessions can fall back to banked questions.
- Knowledge graph metadata such as prerequisites, common errors, exam weight, and estimated minutes is loaded from `src/main/resources/knowledge-graph/` and used by the learning path flow.
