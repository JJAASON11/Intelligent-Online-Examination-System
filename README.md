# 智能在线考试系统

## 后端启动
- 根目录执行 `mvn -f server/pom.xml -DskipTests package`
- 运行 `server/app/target/app-0.0.1-SNAPSHOT.jar`
- 默认使用 H2 内存库；开发环境使用 `application-dev.yml` 连接 MySQL

## 前端启动
- 进入 `web` 执行 `npm i`，再执行 `npm run dev`
- 通过 Vite 代理访问后端 `http://localhost:8080`

## 接口契约
- 认证：
  - `POST /api/auth/login` 请求 `{ username, password }` 响应 `{ code, message, data: { token } }`
  - `POST /api/auth/logout`
- 健康检查：`GET /api/health` 返回 `ok`
- 题库：
  - `GET /api/questions` 返回题目列表
  - `POST /api/questions` 创建题目
  - `PUT /api/questions/{id}` 更新题目
  - `DELETE /api/questions/{id}` 删除题目
- 组卷：`POST /api/papers/assemble` 请求 `{ count }` 返回候选题目列表
- 考试：
  - `POST /api/exams/sessions` 创建考试会话
  - `POST /api/exams/events` 上报事件
- 监考：`WS /ws/proctor` 文本消息回显 `echo:<payload>`
- 阅卷：`POST /api/grading/auto` 请求 `{ answer, standard, score }` 返回 `{ autoScore }`

## 统一返回与错误码
- 统一结构：`{ code: number, message: string, data: any }`
- 约定：
  - `0` 成功
  - `400xx` 参数错误
  - `40100` 未认证或令牌失效
  - `40300` 无权限
  - `40400` 资源不存在
  - `500xx` 服务器错误

## 模块说明
- `common`：统一返回、基础模型
- `auth`：安全配置与认证接口
- `bank`：题库与题目 CRUD
- `paper`：组卷策略与候选生成
- `exam`：考试会话与事件记录接口
- `proctor`：WebSocket 监考端点
- `grading`：智能阅卷规则评分接口
- `app`：启动与全局配置

