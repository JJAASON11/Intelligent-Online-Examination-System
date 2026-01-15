# 豆包AI配置说明

## 功能说明
本系统已集成豆包大模型API，用于帮助学生解答错题。学生可以在查看错题时，点击"AI解答"按钮，系统会自动调用豆包AI分析错题原因并给出详细解答。

## 配置步骤

### 1. 获取豆包API密钥和接入点ID

1. 访问 [火山引擎官网](https://www.volcengine.com/product/doubao/)
2. 注册并完成实名认证
3. 在控制台的"API Key 管理"中创建新的API密钥
4. 在"模型广场"中选择豆包模型（推荐使用 `doubao-lite-4k` 免费版）
5. 在模型推理页面创建接入点，记录接入点ID（格式：`ep-xxxxxx`）

### 2. 配置API密钥

有两种配置方式：

#### 方式一：通过环境变量配置（推荐）

在启动应用前设置环境变量：
```bash
export DOUBAO_API_KEY=你的API密钥
export DOUBAO_ENDPOINT_ID=你的接入点ID
```

Windows系统：
```cmd
set DOUBAO_API_KEY=你的API密钥
set DOUBAO_ENDPOINT_ID=你的接入点ID
```

#### 方式二：直接修改配置文件

编辑 `server/app/src/main/resources/application.yml` 或 `application-dev.yml`：

```yaml
doubao:
  api-key: 你的API密钥
  endpoint-id: 你的接入点ID
  model: doubao-lite-4k
  api-url: https://ark.cn-beijing.volces.com/api/v3/endpoint
```

### 3. 重启应用

配置完成后，重启Spring Boot应用即可使用AI解答功能。

## API接口说明

### 1. 解答错题
- **接口**：`POST /api/ai/explain-wrong-answer`
- **权限**：所有登录用户（学生和教师）
- **请求体**：
```json
{
  "questionStem": "题目内容",
  "studentAnswer": "学生的答案",
  "correctAnswer": "正确答案",
  "questionType": "SINGLE"  // 题目类型：SINGLE, MULTI, JUDGE, FILL, SHORT, CODE
}
```

### 2. 简单问答（测试用）
- **接口**：`POST /api/ai/ask`
- **权限**：仅管理员和教师
- **请求体**：
```json
{
  "question": "你的问题"
}
```

### 3. 检查配置状态
- **接口**：`GET /api/ai/status`
- **权限**：仅管理员和教师

## 注意事项

1. 豆包API有免费额度，超出后可能需要付费
2. 请妥善保管API密钥，不要提交到代码仓库
3. 如果API调用失败，系统会返回错误提示，请检查配置是否正确
4. 建议在生产环境中使用环境变量配置，避免密钥泄露

