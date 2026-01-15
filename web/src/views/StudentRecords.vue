<template>
  <div class="record-container">
    <el-page-header content="我的考试记录" @back="$router.back()" title="返回" class="page-header" />

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">历史考试列表</span>
          <el-button :icon="Refresh" circle @click="loadData" :loading="loading" title="刷新列表" />
        </div>
      </template>

      <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="sessionId" label="场次编号" min-width="160" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">#{{ row.sessionId }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="paperId" label="试卷ID" min-width="90" align="center" />

        <el-table-column prop="status" label="当前状态" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="成绩概览" align="center">
          <el-table-column prop="objective" label="客观题" min-width="90" align="center">
            <template #default="{ row }">
              <span v-if="row.objective != null">{{ row.objective }}</span>
              <span v-else class="placeholder">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="subjective" label="主观题" min-width="90" align="center">
            <template #default="{ row }">
              <span v-if="row.subjective != null">{{ row.subjective }}</span>
              <span v-else class="placeholder">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="total" label="总分" min-width="90" align="center">
            <template #default="{ row }">
              <span v-if="row.total != null" class="total-score">{{ row.total }}</span>
              <span v-else class="placeholder">待阅卷</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Document" @click="openDetail(row)">成绩详情</el-button>
            <el-button
                size="small"
                type="primary"
                :icon="Right"
                :disabled="row.status === 'FINISHED' || row.status === 'GRADED'"
                @click="retake(row)"
            >
              {{ (row.status === 'FINISHED' || row.status === 'GRADED') ? '已交卷' : '继续考试' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="open" title="考试成绩单" width="800px" custom-class="score-dialog">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="成绩概览" name="summary">
          <div v-loading="detailLoading">
            <div v-if="summary" class="score-report">
              <div class="score-banner">
                <div class="label">最终得分</div>
                <div class="number">{{ summary.total ?? 0 }}</div>
                <el-progress
                    :percentage="Math.min((summary.total || 0), 100)"
                    :show-text="false"
                    :stroke-width="10"
                    :color="customColors"
                    style="width: 80%; margin-top: 10px;"
                />
              </div>

              <el-descriptions :column="2" border class="mt-4">
                <el-descriptions-item label="考试场次">#{{ current?.sessionId }}</el-descriptions-item>
                <el-descriptions-item label="试卷编号">{{ current?.paperId }}</el-descriptions-item>
                <el-descriptions-item label="客观题得分">
                  <span class="score-part">{{ summary.objective ?? 0 }} 分</span>
                </el-descriptions-item>
                <el-descriptions-item label="主观题得分">
                  <span class="score-part">{{ summary.subjective ?? 0 }} 分</span>
                </el-descriptions-item>
              </el-descriptions>

              <el-alert
                  v-if="summary.total == null"
                  title="阅卷提示"
                  type="warning"
                  description="当前试卷可能尚未完成阅卷，主观题分数可能未计入总分。"
                  show-icon
                  :closable="false"
                  style="margin-top: 20px;"
              />
            </div>
            <el-empty v-else description="暂无详细成绩数据" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="错题解析" name="wrong-answers">
          <div v-loading="loadingAnswers">
            <div v-if="wrongAnswers.length === 0 && !loadingAnswers" class="no-wrong-answers">
              <el-empty description="恭喜！本次考试没有错题" />
            </div>
            <div v-else class="wrong-answers-list">
              <div v-for="(item, index) in wrongAnswers" :key="item.questionId" class="wrong-answer-item">
                <div class="question-header">
                  <span class="q-index">错题 {{ index + 1 }}</span>
                  <el-tag :type="getTypeTag(item.questionType)" size="small">{{ item.questionType }}</el-tag>
                  <el-tag type="danger" size="small" effect="plain">得分: {{ item.score }} / {{ item.fullScore }}</el-tag>
                </div>
                <div class="question-content">
                  <div class="q-stem" v-html="formatText(item.questionStem)"></div>
                  <div class="answer-section">
                    <div class="answer-row">
                      <span class="label">我的答案：</span>
                      <span class="value wrong">{{ formatAnswer(item.studentAnswer, item.questionType) || '未作答' }}</span>
                    </div>
                    <div class="answer-row">
                      <span class="label">正确答案：</span>
                      <span class="value correct">{{ formatAnswer(item.correctAnswer, item.questionType) }}</span>
                    </div>
                  </div>
                  <div class="ai-section">
                    <el-button 
                      type="primary" 
                      :icon="ChatDotRound" 
                      :loading="item.aiLoading"
                      @click="getAIExplanation(item)"
                      size="small"
                    >
                      {{ item.aiExplanation ? '重新获取AI解答' : '豆包AI解答' }}
                    </el-button>
                    <el-button
                      size="small"
                      type="default"
                      @click="openChat()"
                    >
                      聊天测试
                    </el-button>
                    <div v-if="item.aiExplanation" class="ai-explanation">
                      <el-divider content-position="left">AI解答</el-divider>
                      <div class="explanation-content" v-html="formatText(item.aiExplanation)"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="open=false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="chatOpen" title="豆包聊天测试" width="720px">
      <div class="chat-wrap">
        <div class="chat-history">
          <div v-if="chatMessages.length === 0" class="chat-empty">
            <el-empty description="先发一句话测试豆包是否可用" />
          </div>
          <div v-else>
            <div v-for="(m, i) in chatMessages" :key="i" class="chat-msg" :class="m.role">
              <div class="chat-role">{{ m.role === 'user' ? '我' : '豆包' }}</div>
              <div class="chat-text" v-html="formatText(m.text)"></div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="3"
            placeholder="输入一句话，发送测试（建议先发：你好）"
            :disabled="chatLoading"
          />
          <div class="chat-actions">
            <el-button @click="chatOpen=false" :disabled="chatLoading">关闭</el-button>
            <el-button type="primary" :loading="chatLoading" @click="sendChat()">发送</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { Refresh, Document, Right, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const rows = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const loadingAnswers = ref(false)
const studentId = Number(localStorage.getItem('userId') || 0)

// 详情相关状态
const open = ref(false)
const current = ref(null)
const summary = ref(null)
const activeTab = ref('summary')
const wrongAnswers = ref([])
const questionMeta = ref({}) // 存储题目元数据

// 聊天测试（移动到首页后，这里保留接口以兼容旧调用，可考虑后续删除）
const chatOpen = ref(false)
const chatInput = ref('')
const chatLoading = ref(false)
const chatMessages = ref([]) // { role: 'user'|'assistant', text: string }[]

// 进度条颜色策略
const customColors = [
  { color: '#f56c6c', percentage: 60 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#5cb87a', percentage: 100 },
]

// --- 辅助函数 ---
function getStatusType(status) {
  const map = {
    'FINISHED': 'success',
    'GRADED': 'success',
    'SUBMIT': 'success',
    'STARTED': 'primary',
    'START': 'primary',
    'CREATED': 'info'
  }
  return map[status] || 'info'
}

function formatStatus(status) {
  const map = {
    'FINISHED': '已交卷',
    'GRADED': '已阅卷',
    'SUBMIT': '已提交待阅卷',
    'STARTED': '进行中',
    'START': '进行中',
    'CREATED': '未开始'
  }
  return map[status] || status
}

// --- API 操作 ---
async function loadData() {
  loading.value = true
  try {
    const { data } = await http.get(`/exams/students/${studentId}/records`)
    rows.value = data?.data || []
  } catch (e) {
    // 错误静默处理或提示
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  current.value = row
  open.value = true
  activeTab.value = 'summary'
  summary.value = null
  wrongAnswers.value = []
  questionMeta.value = {}
  
  detailLoading.value = true
  try {
    const { data } = await http.get(`/exams/sessions/${row.sessionId}/summary`)
    summary.value = data?.data || { total: 0, objective: 0, subjective: 0 }
    
    // 加载试卷题目信息
    try {
      const { data: paperData } = await http.get(`/exams/papers/${row.paperId}/preview`)
      const questions = paperData?.data || []
      const meta = {}
      questions.forEach(q => {
        meta[q.id] = {
          stem: q.stem,
          type: q.type,
          score: q.score,
          optionsJson: q.optionsJson,
          answerJson: q.answerJson
        }
      })
      questionMeta.value = meta
    } catch (e) {
      console.error('加载试卷信息失败', e)
    }
    
    // 加载错题（如果已提交/已阅卷/已结束）
    if (['GRADED', 'FINISHED', 'SUBMIT', 'SUBMITTED'].includes(row.status)) {
      await loadWrongAnswers(row.sessionId)
    }
  } catch (e) {
    ElMessage.error('获取成绩详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function loadWrongAnswers(sessionId) {
  loadingAnswers.value = true
  try {
    // 使用学生专用接口获取答案记录
    const { data } = await http.get(`/exams/sessions/${sessionId}/my-answers`, {
      params: { studentId }
    })
    const answers = data?.data || []
    
    // 筛选错题：如果满分未知，默认认为满分 100，用于保证展示
    wrongAnswers.value = answers
      .filter(a => {
        const meta = questionMeta.value[a.questionId] || {}
        const fullScore = (meta.score ?? a.fullScore ?? 100)
        const studentScore = a.score_final ?? a.scoreFinal ?? a.score_auto ?? a.scoreAuto ?? 0
        return studentScore < fullScore
      })
      .map(a => {
        const meta = questionMeta.value[a.questionId] || {}
        const fullScore = meta.score ?? a.fullScore ?? 100
        return {
          questionId: a.questionId,
          questionStem: meta.stem || '',
          questionType: meta.type || 'UNKNOWN',
          studentAnswer: a.answerJson || a.answer_json || '',
          correctAnswer: meta.answerJson || '',
          score: a.score_final ?? a.scoreFinal ?? a.score_auto ?? a.scoreAuto ?? 0,
          fullScore,
          aiExplanation: '',
          aiLoading: false
        }
      })
  } catch (e) {
    // 如果接口需要权限，静默失败
    console.warn('无法加载答案记录，可能需要教师权限', e)
    wrongAnswers.value = []
  } finally {
    loadingAnswers.value = false
  }
}

async function getAIExplanation(item) {
  if (item.aiLoading) return
  item.aiLoading = true
  try {
    const { data } = await http.post(
      '/ai/explain-wrong-answer',
      {
        questionStem: item.questionStem,
        studentAnswer: item.studentAnswer,
        correctAnswer: item.correctAnswer,
        questionType: item.questionType
      },
      { timeout: 60000 } // AI 可能较慢，单独放宽超时
    )
    item.aiExplanation = data?.data || 'AI解答获取失败'
  } catch (e) {
    ElMessage.error('获取AI解答失败：' + (e.response?.data?.message || e.message))
    item.aiExplanation = '获取AI解答时发生错误'
  } finally {
    item.aiLoading = false
  }
}

function openChat() {
  // 兼容旧按钮：现在推荐在“控制台”中使用全局聊天框
  chatOpen.value = true
}

async function sendChat() {
  if (chatLoading.value) return
  const q = String(chatInput.value || '').trim()
  if (!q) {
    ElMessage.warning('请输入内容')
    return
  }
  chatMessages.value.push({ role: 'user', text: q })
  chatInput.value = ''
  chatLoading.value = true
  try {
    const { data } = await http.post('/ai/chat', { question: q }, { timeout: 60000 })
    chatMessages.value.push({ role: 'assistant', text: data?.data || '（无返回）' })
  } catch (e) {
    ElMessage.error('聊天失败：' + (e.response?.data?.message || e.message))
    chatMessages.value.push({ role: 'assistant', text: '（请求失败，请稍后重试）' })
  } finally {
    chatLoading.value = false
  }
}

function formatAnswer(answerJson, questionType) {
  if (!answerJson) return ''
  try {
    const parsed = JSON.parse(answerJson)
    if (questionType === 'SINGLE' || questionType === 'JUDGE') {
      return parsed || ''
    } else if (questionType === 'MULTI') {
      return Array.isArray(parsed) ? parsed.join(', ') : String(parsed)
    } else if (questionType === 'FILL') {
      return Array.isArray(parsed) ? parsed.join(' | ') : String(parsed)
    } else {
      return String(parsed)
    }
  } catch {
    return answerJson
  }
}

function formatText(text) {
  if (!text) return ''
  return String(text).replace(/\n/g, '<br>')
}

function getTypeTag(type) {
  const map = {
    'SINGLE': 'primary',
    'MULTI': 'success',
    'JUDGE': 'warning',
    'FILL': 'info',
    'SHORT': 'danger',
    'CODE': 'danger'
  }
  return map[type] || 'info'
}

function retake(row) {
  // 如果考试已经结束，拦截
  if (row.status === 'FINISHED' || row.status === 'GRADED') {
    ElMessage.warning('该场考试已结束，无法重新进入')
    return
  }

  // 使用 Router 跳转，体验更好
  router.push({
    path: '/exam/take',
    query: {
      sessionId: row.sessionId,
      paperId: row.paperId,
      duration: 60 // 注意：这里的 duration 最好从后端 row 数据中获取，如果 row 里没有，暂时保持 60
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.record-container {
  padding: 24px;
  background-color: #f6f8f9;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

.main-card {
  border-radius: 8px;
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

/* 成绩文字样式 */
.total-score {
  font-weight: bold;
  color: #409eff;
  font-size: 15px;
}
.placeholder {
  color: #c0c4cc;
}

/* --- 聊天测试 --- */
.chat-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chat-history {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  background: #fafafa;
}
.chat-msg {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.chat-msg:last-child {
  border-bottom: none;
}
.chat-role {
  width: 46px;
  flex: 0 0 46px;
  font-weight: 600;
  color: #606266;
}
.chat-text {
  flex: 1;
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-msg.user .chat-text {
  color: #1f2d3d;
}
.chat-msg.assistant .chat-text {
  color: #303133;
}
.chat-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 详情弹窗样式 */
.score-report {
  text-align: center;
  padding: 10px;
}

.score-banner {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-banner .label {
  font-size: 14px;
  color: #67c23a;
  margin-bottom: 8px;
}

.score-banner .number {
  font-size: 48px;
  font-weight: bold;
  color: #67c23a;
  line-height: 1;
}

.score-part {
  font-weight: 600;
  color: #606266;
}

.mt-4 {
  margin-top: 16px;
}

/* 错题列表样式 */
.wrong-answers-list {
  max-height: 600px;
  overflow-y: auto;
}

.wrong-answer-item {
  margin-bottom: 24px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.q-index {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.question-content {
  margin-top: 12px;
}

.q-stem {
  margin-bottom: 16px;
  padding: 12px;
  background: white;
  border-radius: 4px;
  line-height: 1.6;
  color: #606266;
}

.answer-section {
  margin-bottom: 16px;
}

.answer-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
}

.answer-row .label {
  font-weight: 600;
  color: #909399;
  min-width: 80px;
  margin-right: 12px;
}

.answer-row .value {
  flex: 1;
  word-break: break-word;
}

.answer-row .value.wrong {
  color: #f56c6c;
  font-weight: 500;
}

.answer-row .value.correct {
  color: #67c23a;
  font-weight: 500;
}

.ai-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
}

.ai-explanation {
  margin-top: 16px;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.explanation-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

.no-wrong-answers {
  padding: 40px;
  text-align: center;
}
</style>